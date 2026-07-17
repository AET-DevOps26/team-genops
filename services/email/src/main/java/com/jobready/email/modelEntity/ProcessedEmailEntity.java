package com.jobready.email.modelEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A Gmail message fetched by the poller, deduplicated on (user_id, message_id). */
@Entity
@Table(name = "processed_emails", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "message_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ProcessedEmailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "message_id", nullable = false)
    private String messageId;

    @Column
    private String subject;

    @Column
    private String sender;

    @Column
    private String snippet;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt = Instant.now();
}
