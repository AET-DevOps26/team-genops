package com.jobready.document.repository;

import com.jobready.document.modelEntity.ResumeEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeRepository extends JpaRepository<ResumeEntity, UUID> {

    List<ResumeEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<ResumeEntity> findByUserIdAndApplicationIdOrderByCreatedAtDesc(UUID userId, UUID applicationId);

    /** Scoped to the owner — users can never touch another user's rows. */
    Optional<ResumeEntity> findByIdAndUserId(UUID id, UUID userId);
}
