package com.jobready.email.repository;

import com.jobready.email.modelEntity.EmailConnectionEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailConnectionRepository extends JpaRepository<EmailConnectionEntity, UUID> {

    Optional<EmailConnectionEntity> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
