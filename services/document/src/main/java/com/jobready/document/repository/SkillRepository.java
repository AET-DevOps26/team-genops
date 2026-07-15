package com.jobready.document.repository;

import com.jobready.document.modelEntity.SkillEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<SkillEntity, UUID> {

    /** A user's skills in insertion order (stable for the UI). */
    List<SkillEntity> findByUserIdOrderByCreatedAtAsc(UUID userId);

    /** Scoped to the owner — users can never touch another user's rows. */
    Optional<SkillEntity> findByIdAndUserId(UUID id, UUID userId);
}
