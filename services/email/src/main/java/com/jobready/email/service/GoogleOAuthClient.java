package com.jobready.email.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jobready.email.config.EmailProperties;
import java.time.Instant;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Google OAuth2 over plain REST — consent URL, code exchange, token refresh, and mailbox
 * resolution. Deliberately free of DB/web concerns so it can be mocked in service tests.
 */
@Component
public class GoogleOAuthClient {

    public static final String PROVIDER = "gmail";

    private static final List<String> SCOPES = List.of(
            "openid",
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/gmail.readonly");

    /**
     * Tokens returned by a code exchange plus the resolved mailbox address. Google omits the
     * refresh token on re-consent when one was already granted; the callback handles null by
     * asking the user to re-connect.
     */
    public record ExchangedCredentials(
            String emailAddress, String accessToken, String refreshToken, Instant tokenExpiry) {}

    /** A freshly refreshed access token and its expiry. */
    public record RefreshedToken(String accessToken, Instant tokenExpiry) {}

    private final EmailProperties.Google google;
    private final RestClient restClient;

    public GoogleOAuthClient(EmailProperties properties, RestClient.Builder builder) {
        this.google = properties.google();
        this.restClient = builder.build();
    }

    /** Return the Google consent URL, requesting offline access for a refresh token. */
    public String buildAuthorizationUrl(String state) {
        return UriComponentsBuilder.fromUriString(google.authUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", google.clientId())
                .queryParam("redirect_uri", google.redirectUri())
                .queryParam("scope", String.join(" ", SCOPES))
                .queryParam("state", state)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .encode()
                .build()
                .toUriString();
    }

    /** Exchange an authorization code for tokens and resolve the mailbox address. */
    public ExchangedCredentials exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("client_id", google.clientId());
        form.add("client_secret", google.clientSecret());
        form.add("redirect_uri", google.redirectUri());
        TokenResponse tokens = postTokenRequest(form);
        String email = fetchUserEmail(tokens.accessToken());
        return new ExchangedCredentials(email, tokens.accessToken(), tokens.refreshToken(), tokens.expiry());
    }

    /** Use a refresh token to obtain a fresh access token + expiry. */
    public RefreshedToken refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);
        form.add("client_id", google.clientId());
        form.add("client_secret", google.clientSecret());
        TokenResponse tokens = postTokenRequest(form);
        return new RefreshedToken(tokens.accessToken(), tokens.expiry());
    }

    private TokenResponse postTokenRequest(MultiValueMap<String, String> form) {
        TokenResponse tokens = restClient
                .post()
                .uri(google.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (tokens == null || tokens.accessToken() == null) {
            throw new IllegalStateException("Google token endpoint returned no access token");
        }
        return tokens;
    }

    private String fetchUserEmail(String accessToken) {
        UserInfo info = restClient
                .get()
                .uri(google.userinfoUri())
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .body(UserInfo.class);
        if (info == null || info.email() == null) {
            throw new IllegalStateException("Google userinfo returned no email address");
        }
        return info.email();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("expires_in") Long expiresIn) {

        Instant expiry() {
            return Instant.now().plusSeconds(expiresIn == null ? 0 : expiresIn);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UserInfo(@JsonProperty("email") String email) {}
}
