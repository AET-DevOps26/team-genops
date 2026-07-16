package com.jobready.document.repository;

import com.jobready.document.modelEntity.LanguageEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LanguageRepository extends JpaRepository<LanguageEntity, UUID> {

    /** A user's languages in insertion order (stable for the UI). */
    List<LanguageEntity> findByUserIdOrderByCreatedAtAsc(UUID userId);

    /** Scoped to the owner — users can never touch another user's rows. */
    Optional<LanguageEntity> findByIdAndUserId(UUID id, UUID userId);
}
