package com.jobready.application.repository;

import com.jobready.application.modelEntity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

    /** One application's recommendations, newest first, scoped to the owner. */
    List<Recommendation> findByApplicationIdAndUserIdOrderByCreatedAtDesc(UUID applicationId, UUID userId);

    /** A single recommendation scoped to its owner — users can never touch another user's rows. */
    Optional<Recommendation> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Removes an application's recommendations when the application is deleted. ddl-auto doesn't
     * create the FK cascade from the reference SQL, so the service cascades explicitly.
     */
    void deleteByApplicationIdAndUserId(UUID applicationId, UUID userId);
}
