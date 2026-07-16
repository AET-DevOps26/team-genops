package com.jobready.document.repository;

import com.jobready.document.modelEntity.WorkExperienceEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkExperienceRepository extends JpaRepository<WorkExperienceEntity, UUID> {

    /** A user's work experiences, most recent role first. */
    List<WorkExperienceEntity> findByUserIdOrderByStartDateDesc(UUID userId);

    /** Scoped to the owner — users can never touch another user's rows. */
    Optional<WorkExperienceEntity> findByIdAndUserId(UUID id, UUID userId);
}
