package com.jobready.email.service;

import com.jobready.email.modelEntity.EmailConnectionEntity;
import com.jobready.email.repository.EmailConnectionRepository;
import com.jobready.email.repository.ProcessedEmailRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background poller: periodically fetch and store new emails for every connection.
 *
 * <p>For each connection it refreshes the access token if expired, lists recent Gmail messages,
 * and inserts any not already stored (deduped on the {@code (user_id, message_id)} unique
 * constraint). Each connection is processed in its own transaction with its own try/catch so one
 * failing mailbox can never block the others.
 *
 * <p>NOTE: {@code @Scheduled} runs per-process, so this is single-instance only — with more than
 * one replica each would poll independently (correctness is preserved by the dedupe, but Gmail
 * quota is wasted). A multi-replica fix is a Redis leader lock — out of scope here.
 */
@Component
public class EmailPoller {

    private static final Logger log = LoggerFactory.getLogger(EmailPoller.class);

    private final EmailConnectionRepository connectionRepository;
    private final ProcessedEmailRepository processedEmailRepository;
    private final GoogleOAuthClient googleOAuthClient;
    private final GmailClient gmailClient;

    public EmailPoller(
            EmailConnectionRepository connectionRepository,
            ProcessedEmailRepository processedEmailRepository,
            GoogleOAuthClient googleOAuthClient,
            GmailClient gmailClient) {
        this.connectionRepository = connectionRepository;
        this.processedEmailRepository = processedEmailRepository;
        this.googleOAuthClient = googleOAuthClient;
        this.gmailClient = gmailClient;
    }

    @Scheduled(
            initialDelayString = "${email.poll-interval-seconds}",
            fixedDelayString = "${email.poll-interval-seconds}",
            timeUnit = java.util.concurrent.TimeUnit.SECONDS)
    public void scheduledPoll() {
        int stored = pollOnce();
        if (stored > 0) {
            log.info("Poller stored {} new email(s)", stored);
        }
    }

    /** Run one poll pass over all connections. Returns total messages stored. */
    public int pollOnce() {
        int total = 0;
        for (EmailConnectionEntity connection : connectionRepository.findAll()) {
            try {
                total += pollConnection(connection);
            } catch (Exception e) { // one bad connection must not stop the rest
                log.error("Polling failed for user {}", connection.getUserId(), e);
            }
        }
        return total;
    }

    /**
     * Fetch and store new messages for one connection. Returns count stored. Each insert (and
     * any token-refresh save) runs in its own short transaction, so a failure mid-mailbox never
     * poisons work already stored — parity with the Python per-connection session isolation.
     */
    public int pollConnection(EmailConnectionEntity connection) {
        String accessToken = freshAccessToken(connection);
        int stored = 0;
        for (String messageId : gmailClient.listRecentMessageIds(accessToken)) {
            GmailClient.MessageMetadata msg = gmailClient.fetchMessage(accessToken, messageId);
            stored += processedEmailRepository.insertIgnoringDuplicates(
                    connection.getUserId(),
                    msg.messageId(),
                    msg.subject(),
                    msg.sender(),
                    msg.snippet(),
                    msg.receivedAt());
        }
        return stored;
    }

    /** Return a valid access token, refreshing and persisting it if expired. */
    private String freshAccessToken(EmailConnectionEntity connection) {
        if (connection.getTokenExpiry().isAfter(Instant.now())) {
            return connection.getAccessToken();
        }
        GoogleOAuthClient.RefreshedToken refreshed = googleOAuthClient.refreshAccessToken(connection.getRefreshToken());
        connection.setAccessToken(refreshed.accessToken());
        connection.setTokenExpiry(refreshed.tokenExpiry());
        connectionRepository.save(connection);
        return refreshed.accessToken();
    }
}
