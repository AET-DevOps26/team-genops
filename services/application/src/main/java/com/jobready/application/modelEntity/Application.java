package com.jobready.application.modelEntity;

import com.jobready.application.generated.modelDto.ApplicationStage;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A job application owned by a single user. Mapped to {@code application.applications}.
 *
 * <p>Note: the live schema comes from {@code ddl-auto=update}; the SQL under
 * {@code db/migration/} is unexecuted reference documentation kept in sync by hand.</p>
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

    @Column(name = "company_website", length = 512)
    private String companyWebsite;

    @Column(name = "linkedin_url", length = 512)
    private String linkedinUrl;

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
