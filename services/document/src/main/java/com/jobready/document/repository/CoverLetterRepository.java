package com.jobready.document.repository;

import com.jobready.document.modelEntity.CoverLetterEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoverLetterRepository extends JpaRepository<CoverLetterEntity, UUID> {

    List<CoverLetterEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<CoverLetterEntity> findByUserIdAndApplicationIdOrderByCreatedAtDesc(UUID userId, UUID applicationId);

    /** Scoped to the owner — users can never touch another user's rows. */
    Optional<CoverLetterEntity> findByIdAndUserId(UUID id, UUID userId);
}
