package com.jobready.document.modelEntity;

import jakarta.persistence.Column;
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

/**
 * An AI-generated resume persisted by the genai service. {@code application_id} points at
 * the application service's aggregate — deliberately no FK (cross-service reference).
 */
@Entity
@Table(schema = "document", name = "resumes")
@Getter
@Setter
@NoArgsConstructor
public class ResumeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Null for a standalone document — one the user polished without a target job. */
    @Column(name = "application_id")
    private UUID applicationId;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
