package com.jobready.application.repository;

import com.jobready.application.modelEntity.ApplicationEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationEventRepository extends JpaRepository<ApplicationEvent, UUID> {

    List<ApplicationEvent> findByApplicationIdAndUserIdOrderByOccurredAtDesc(UUID applicationId, UUID userId);

    /** Idempotency check for email-derived updates — one event per (application, Gmail message). */
    boolean existsByApplicationIdAndSourceMessageId(UUID applicationId, String sourceMessageId);

    /**
     * Idempotency check for email-derived auto-creates — before the application exists there is
     * no application id to key on, so one email creates at most one application per user.
     */
    boolean existsByUserIdAndSourceMessageId(UUID userId, String sourceMessageId);

    void deleteByApplicationIdAndUserId(UUID applicationId, UUID userId);
}
