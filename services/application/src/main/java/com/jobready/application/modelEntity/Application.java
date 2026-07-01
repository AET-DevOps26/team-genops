package com.jobready.application.modelEntity;

import com.jobready.application.generated.modelDto.ApplicationStage;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A job application owned by a single user. Mapped to {@code application.applications}.
 *
 * <p>Note: the {@code application.fit_analyses} table from the Flyway reference migration is
 * intentionally <strong>not</strong> mapped here — fit analysis is out of scope for this service.
 * Because {@code ddl-auto=update} creates tables from entities, only {@code applications} is created
 * at runtime, so the Flyway SQL no longer reflects the live schema 1:1.</p>
 */
@Entity
@Table(schema = "application", name = "applications")
@Getter
@Setter
@NoArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String company;

    @Column(name = "job_title", nullable = false)
    private String jobTitle;

    @Column(name = "job_description", columnDefinition = "text")
    private String jobDescription;

    @Column(name = "job_url", length = 512)
    private String jobUrl;

    @Convert(converter = StageConverter.class)
    @Column(nullable = false, length = 50)
    private ApplicationStage stage = ApplicationStage.APPLIED;

    @Column(columnDefinition = "text")
    private String notes;

    @CreationTimestamp
    @Column(name = "applied_at", nullable = false, updatable = false)
    private OffsetDateTime appliedAt;

    // @UpdateTimestamp sets the value on INSERT and on every UPDATE, so the initial value is
    // populated at creation too — no separate @CreationTimestamp needed.
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
