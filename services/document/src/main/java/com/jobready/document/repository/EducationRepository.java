package com.jobready.document.repository;

import com.jobready.document.modelEntity.EducationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EducationRepository extends JpaRepository<EducationEntity, UUID> {

    /** A user's education entries, most recent first. */
    List<EducationEntity> findByUserIdOrderByStartDateDesc(UUID userId);

    /** Scoped to the owner — users can never touch another user's rows. */
    Optional<EducationEntity> findByIdAndUserId(UUID id, UUID userId);
}
