package com.jobready.application.repository;

import com.jobready.application.generated.modelDto.ApplicationStage;
import com.jobready.application.modelEntity.Application;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    /** All of a user's applications, newest first. */
    List<Application> findByUserIdOrderByAppliedAtDesc(UUID userId);

    /** Paginated variant, newest first. */
    List<Application> findByUserIdOrderByAppliedAtDesc(UUID userId, Pageable pageable);

    /** Stage-filtered, paginated, newest first. */
    List<Application> findByUserIdAndStageOrderByAppliedAtDesc(UUID userId, ApplicationStage stage, Pageable pageable);

    long countByUserId(UUID userId);

    long countByUserIdAndStage(UUID userId, ApplicationStage stage);

    /** Per-stage counts for the pipeline summary — one row per stage that has applications. */
    @Query("select a.stage as stage, count(a) as count from Application a where a.userId = :userId group by a.stage")
    List<StageCount> countByUserIdGroupedByStage(@Param("userId") UUID userId);

    /** A single application scoped to its owner — used so users can never read another user's rows. */
    Optional<Application> findByIdAndUserId(UUID id, UUID userId);

    interface StageCount {
        ApplicationStage getStage();

        long getCount();
    }
}
