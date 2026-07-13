package com.jobready.document.repository;

import com.jobready.document.modelEntity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<ProfileEntity, UUID> {

    /** One profile per user — {@code user_id} is unique. */
    Optional<ProfileEntity> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
