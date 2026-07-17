package com.jobready.email.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobready.email.exception.InvalidStateException;
import com.jobready.email.generated.modelDto.EmailConnectionStatus;
import com.jobready.email.modelEntity.EmailConnectionEntity;
import com.jobready.email.repository.EmailConnectionRepository;
import com.jobready.email.support.TestProperties;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EmailConnectionServiceTest {

    private final UUID userId = UUID.randomUUID();
    private final NonceStore nonceStore = new NonceStore();
    private final StateTokenService stateTokenService =
            new StateTokenService(TestProperties.emailProperties(), nonceStore);
    private final GoogleOAuthClient googleOAuthClient = mock(GoogleOAuthClient.class);
    private final EmailConnectionRepository connectionRepository = mock(EmailConnectionRepository.class);

    private EmailConnectionService service;

    @BeforeEach
    void setUp() {
        service = new EmailConnectionService(
                TestProperties.emailProperties(), stateTokenService, googleOAuthClient, connectionRepository);
        when(connectionRepository.findByUserId(userId)).thenReturn(Optional.empty());
    }

    private GoogleOAuthClient.ExchangedCredentials credentials(String refreshToken) {
        return new GoogleOAuthClient.ExchangedCredentials(
                "user@gmail.com", "access-token", refreshToken, Instant.now().plusSeconds(3600));
    }

    @Test
    void successfulCallbackStoresConnectionAndRedirectsConnected() {
        when(googleOAuthClient.exchangeCode("the-code")).thenReturn(credentials("refresh-token"));
        String state = stateTokenService.issue(userId);

        URI redirect = service.handleCallback("the-code", state);

        assertThat(redirect.toString()).isEqualTo("http://localhost:5173?email_connected=1");
        ArgumentCaptor<EmailConnectionEntity> saved = ArgumentCaptor.forClass(EmailConnectionEntity.class);
        verify(connectionRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(userId);
        assertThat(saved.getValue().getProvider()).isEqualTo("gmail");
        assertThat(saved.getValue().getEmailAddress()).isEqualTo("user@gmail.com");
        assertThat(saved.getValue().getRefreshToken()).isEqualTo("refresh-token");

        // The nonce is burned: replaying the same state must now fail.
        assertThatThrownBy(() -> service.handleCallback("the-code", state)).isInstanceOf(InvalidStateException.class);
    }

    @Test
    void forgedStateFailsWithoutExchanging() {
        assertThatThrownBy(() -> service.handleCallback("code", "forged-state"))
                .isInstanceOf(InvalidStateException.class);
        verify(googleOAuthClient, never()).exchangeCode(anyString());
        verify(connectionRepository, never()).save(any());
    }

    @Test
    void missingRefreshTokenRedirectsWithErrorAndStoresNothing() {
        when(googleOAuthClient.exchangeCode("the-code")).thenReturn(credentials(null));

        URI redirect = service.handleCallback("the-code", stateTokenService.issue(userId));

        assertThat(redirect.toString()).isEqualTo("http://localhost:5173?email_error=missing_refresh_token");
        verify(connectionRepository, never()).save(any());
    }

    @Test
    void failedExchangeRedirectsWithErrorAndLeavesStateReusable() {
        when(googleOAuthClient.exchangeCode("the-code")).thenThrow(new IllegalStateException("google down"));
        String state = stateTokenService.issue(userId);

        URI redirect = service.handleCallback("the-code", state);

        assertThat(redirect.toString()).isEqualTo("http://localhost:5173?email_error=exchange_failed");
        // A transient failure must not burn the single-use nonce.
        assertThat(stateTokenService.validate(state).userId()).isEqualTo(userId);
    }

    @Test
    void statusReflectsStoredConnection() {
        assertThat(service.getStatus(userId).getConnected()).isFalse();

        EmailConnectionEntity connection = new EmailConnectionEntity();
        connection.setUserId(userId);
        connection.setProvider("gmail");
        connection.setEmailAddress("user@gmail.com");
        connection.setCreatedAt(Instant.parse("2026-01-01T12:00:00Z"));
        when(connectionRepository.findByUserId(userId)).thenReturn(Optional.of(connection));

        EmailConnectionStatus status = service.getStatus(userId);
        assertThat(status.getConnected()).isTrue();
        assertThat(status.getProvider()).isEqualTo(EmailConnectionStatus.ProviderEnum.GMAIL);
        assertThat(status.getEmailAddress()).isEqualTo("user@gmail.com");
        assertThat(status.getConnectedAt()).isEqualTo("2026-01-01T12:00:00Z");
    }

    @Test
    void disconnectDeletesByUserId() {
        service.disconnect(userId);
        verify(connectionRepository).deleteByUserId(userId);
    }
}
