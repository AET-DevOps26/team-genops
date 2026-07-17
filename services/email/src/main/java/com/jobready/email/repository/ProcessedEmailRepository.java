package com.jobready.email.repository;

import com.jobready.email.modelEntity.ProcessedEmailEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ProcessedEmailRepository extends JpaRepository<ProcessedEmailEntity, UUID> {

    /**
     * Newest first; NULLS LAST keeps messages without a provider timestamp at the end, matching
     * the covering index. Native SQL because JPQL cannot express NULLS LAST portably.
     */
    @Query(
            value = "SELECT * FROM email.processed_emails WHERE user_id = :userId "
                    + "ORDER BY received_at DESC NULLS LAST, processed_at DESC "
                    + "LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<ProcessedEmailEntity> findPageForUser(
            @Param("userId") UUID userId, @Param("limit") int limit, @Param("offset") int offset);

    /**
     * Race-free dedupe on the (user_id, message_id) unique constraint — parity with the Python
     * service's ON CONFLICT DO NOTHING insert. Returns 1 if stored, 0 if already present.
     */
    @Transactional
    @Modifying
    @Query(
            value = "INSERT INTO email.processed_emails "
                    + "(id, user_id, message_id, subject, sender, snippet, received_at, processed_at) "
                    + "VALUES (gen_random_uuid(), :userId, :messageId, :subject, :sender, :snippet, "
                    + ":receivedAt, NOW()) "
                    + "ON CONFLICT (user_id, message_id) DO NOTHING",
            nativeQuery = true)
    int insertIgnoringDuplicates(
            @Param("userId") UUID userId,
            @Param("messageId") String messageId,
            @Param("subject") String subject,
            @Param("sender") String sender,
            @Param("snippet") String snippet,
            @Param("receivedAt") Instant receivedAt);
}
