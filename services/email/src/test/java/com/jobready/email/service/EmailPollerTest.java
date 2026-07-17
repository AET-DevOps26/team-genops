package com.jobready.email.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobready.email.modelEntity.EmailConnectionEntity;
import com.jobready.email.repository.EmailConnectionRepository;
import com.jobready.email.repository.ProcessedEmailRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailPollerTest {

    @Mock
    private EmailConnectionRepository connectionRepository;

    @Mock
    private ProcessedEmailRepository processedEmailRepository;

    @Mock
    private GoogleOAuthClient googleOAuthClient;

    @Mock
    private GmailClient gmailClient;

    @InjectMocks
    private EmailPoller poller;

    private EmailConnectionEntity connection(UUID userId, Instant tokenExpiry) {
        EmailConnectionEntity connection = new EmailConnectionEntity();
        connection.setUserId(userId);
        connection.setAccessToken("access-token");
        connection.setRefreshToken("refresh-token");
        connection.setTokenExpiry(tokenExpiry);
        return connection;
    }

    private GmailClient.MessageMetadata message(String id) {
        return new GmailClient.MessageMetadata(id, "Subject " + id, "sender@x.com", "snippet", Instant.now());
    }

    @Test
    void storesNewMessagesAndCountsDuplicatesAsZero() {
        UUID userId = UUID.randomUUID();
        when(connectionRepository.findAll())
                .thenReturn(List.of(connection(userId, Instant.now().plusSeconds(600))));
        when(gmailClient.listRecentMessageIds("access-token")).thenReturn(List.of("m1", "m2"));
        when(gmailClient.fetchMessage(eq("access-token"), anyString())).thenAnswer(inv -> message(inv.getArgument(1)));
        // m1 is new, m2 already stored — ON CONFLICT DO NOTHING reports 0 rows.
        when(processedEmailRepository.insertIgnoringDuplicates(
                        eq(userId), eq("m1"), anyString(), anyString(), anyString(), any()))
                .thenReturn(1);
        when(processedEmailRepository.insertIgnoringDuplicates(
                        eq(userId), eq("m2"), anyString(), anyString(), anyString(), any()))
                .thenReturn(0);

        assertThat(poller.pollOnce()).isEqualTo(1);
        verify(googleOAuthClient, never()).refreshAccessToken(anyString());
    }

    @Test
    void refreshesAndPersistsExpiredToken() {
        UUID userId = UUID.randomUUID();
        EmailConnectionEntity expired = connection(userId, Instant.now().minusSeconds(60));
        Instant newExpiry = Instant.now().plusSeconds(3600);
        when(connectionRepository.findAll()).thenReturn(List.of(expired));
        when(googleOAuthClient.refreshAccessToken("refresh-token"))
                .thenReturn(new GoogleOAuthClient.RefreshedToken("new-access", newExpiry));
        when(gmailClient.listRecentMessageIds("new-access")).thenReturn(List.of());

        poller.pollOnce();

        verify(connectionRepository).save(expired);
        assertThat(expired.getAccessToken()).isEqualTo("new-access");
        assertThat(expired.getTokenExpiry()).isEqualTo(newExpiry);
    }

    @Test
    void oneFailingConnectionDoesNotStopTheOthers() {
        EmailConnectionEntity failing =
                connection(UUID.randomUUID(), Instant.now().plusSeconds(600));
        UUID healthyUser = UUID.randomUUID();
        EmailConnectionEntity healthy = connection(healthyUser, Instant.now().plusSeconds(600));
        healthy.setAccessToken("healthy-token");
        when(connectionRepository.findAll()).thenReturn(List.of(failing, healthy));
        when(gmailClient.listRecentMessageIds("access-token")).thenThrow(new IllegalStateException("gmail 500"));
        when(gmailClient.listRecentMessageIds("healthy-token")).thenReturn(List.of("m1"));
        when(gmailClient.fetchMessage("healthy-token", "m1")).thenReturn(message("m1"));
        when(processedEmailRepository.insertIgnoringDuplicates(
                        eq(healthyUser), eq("m1"), anyString(), anyString(), anyString(), any()))
                .thenReturn(1);

        assertThat(poller.pollOnce()).isEqualTo(1);
    }
}
