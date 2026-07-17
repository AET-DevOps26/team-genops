package com.jobready.email.service;

import com.jobready.email.config.EmailProperties;
import com.jobready.email.generated.modelDto.EmailConnectionStatus;
import com.jobready.email.generated.modelDto.GmailAuthorizeResponse;
import com.jobready.email.modelEntity.EmailConnectionEntity;
import com.jobready.email.repository.EmailConnectionRepository;
import java.net.URI;
import java.time.ZoneOffset;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

/** Gmail connection lifecycle: authorize, OAuth callback, status, disconnect. */
@Service
public class EmailConnectionService {

    private static final Logger log = LoggerFactory.getLogger(EmailConnectionService.class);

    private final EmailProperties properties;
    private final StateTokenService stateTokenService;
    private final GoogleOAuthClient googleOAuthClient;
    private final EmailConnectionRepository connectionRepository;

    public EmailConnectionService(
            EmailProperties properties,
            StateTokenService stateTokenService,
            GoogleOAuthClient googleOAuthClient,
            EmailConnectionRepository connectionRepository) {
        this.properties = properties;
        this.stateTokenService = stateTokenService;
        this.googleOAuthClient = googleOAuthClient;
        this.connectionRepository = connectionRepository;
    }

    /** Return a Google consent URL with a signed, single-use state bound to the user. */
    public GmailAuthorizeResponse authorize(UUID userId) {
        String state = stateTokenService.issue(userId);
        return new GmailAuthorizeResponse()
                .authorizationUrl(URI.create(googleOAuthClient.buildAuthorizationUrl(state)));
    }

    /**
     * Handle Google's redirect: verify state, exchange code, store the connection.
     *
     * <p>Unauthenticated — the signed {@code state} is the trust anchor and yields the user id.
     * This is a browser-facing endpoint, so recoverable failures redirect back to the frontend
     * with an {@code email_error} flag; a forged/expired/replayed state still hard-fails (400 via
     * {@code InvalidStateException}): it signals tampering or a stale link, not a user mid-flow.
     *
     * @return the frontend URI the browser should be redirected to
     */
    @Transactional
    public URI handleCallback(String code, String state) {
        StateTokenService.StateClaims claims = stateTokenService.validate(state);
        GoogleOAuthClient.ExchangedCredentials creds;
        try {
            creds = googleOAuthClient.exchangeCode(code);
        } catch (Exception e) {
            log.error("Code exchange failed for user {}", claims.userId(), e);
            return frontendRedirect("email_error", "exchange_failed");
        }

        if (creds.refreshToken() == null || creds.refreshToken().isBlank()) {
            // Without a refresh token the poller cannot keep fetching after the access token
            // expires; send the user back to re-consent.
            return frontendRedirect("email_error", "missing_refresh_token");
        }

        // Only burn the single-use nonce now that the exchange has actually succeeded, so a
        // transient Google failure leaves the link reusable instead of forcing a restart.
        stateTokenService.consume(claims);
        upsertConnection(claims.userId(), creds);
        return frontendRedirect("email_connected", "1");
    }

    public EmailConnectionStatus getStatus(UUID userId) {
        return connectionRepository
                .findByUserId(userId)
                .map(conn -> new EmailConnectionStatus()
                        .connected(true)
                        // Only Gmail is implemented today; the OpenAPI enum is [gmail]. Return
                        // the constant so the response can't drift from the published contract.
                        .provider(EmailConnectionStatus.ProviderEnum.GMAIL)
                        .emailAddress(conn.getEmailAddress())
                        .connectedAt(conn.getCreatedAt().atOffset(ZoneOffset.UTC)))
                .orElseGet(() -> new EmailConnectionStatus().connected(false));
    }

    @Transactional
    public void disconnect(UUID userId) {
        connectionRepository.deleteByUserId(userId);
    }

    private void upsertConnection(UUID userId, GoogleOAuthClient.ExchangedCredentials creds) {
        EmailConnectionEntity connection = connectionRepository
                .findByUserId(userId)
                .orElseGet(() -> {
                    EmailConnectionEntity created = new EmailConnectionEntity();
                    created.setUserId(userId);
                    return created;
                });
        connection.setProvider(GoogleOAuthClient.PROVIDER);
        connection.setEmailAddress(creds.emailAddress());
        connection.setAccessToken(creds.accessToken());
        connection.setRefreshToken(creds.refreshToken());
        connection.setTokenExpiry(creds.tokenExpiry());
        connectionRepository.save(connection);
    }

    /**
     * Redirect the browser back to the frontend, merging the query param safely — parsing the
     * configured URL keeps the redirect valid even if it already carries a query string.
     */
    private URI frontendRedirect(String name, String value) {
        return UriComponentsBuilder.fromUriString(properties.frontendRedirectUrl())
                .queryParam(name, value)
                .build()
                .toUri();
    }
}
