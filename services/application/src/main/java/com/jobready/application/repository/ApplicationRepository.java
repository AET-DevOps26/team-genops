package com.jobready.application.repository;

import com.jobready.application.modelEntity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    /** All of a user's applications, newest first. */
    List<Application> findByUserIdOrderByAppliedAtDesc(UUID userId);

    /** A single application scoped to its owner — used so users can never read another user's rows. */
    Optional<Application> findByIdAndUserId(UUID id, UUID userId);
}
