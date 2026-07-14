package com.jobready.application.repository;

import com.jobready.application.modelEntity.ApplicationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplicationEventRepository extends JpaRepository<ApplicationEvent, UUID> {

    List<ApplicationEvent> findByApplicationIdAndUserIdOrderByOccurredAtDesc(UUID applicationId, UUID userId);

    /** Idempotency check for email-derived updates — one event per (application, Gmail message). */
    boolean existsByApplicationIdAndSourceMessageId(UUID applicationId, String sourceMessageId);

    void deleteByApplicationIdAndUserId(UUID applicationId, UUID userId);
}
