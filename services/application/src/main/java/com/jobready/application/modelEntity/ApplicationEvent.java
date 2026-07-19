package com.jobready.application.modelEntity;

import com.jobready.application.generated.modelDto.ApplicationEventType;
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

/**
 * One timeline entry of an application — a detected email, a stage change, an interview
 * invitation, etc. Mapped to {@code application.application_events}. Events are append-only;
 * {@code occurredAt} records when the event actually happened (for email-derived events, the
 * email's received date), unlike {@code createdAt} which is when the row was written.
 */
@Entity
@Table(schema = "application", name = "application_events")
@Getter
@Setter
@NoArgsConstructor
public class ApplicationEvent {

    /** Where an event came from — a detected email or a manual user action. */
    public enum Source {
        EMAIL,
        MANUAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Convert(converter = EventTypeConverter.class)
    @Column(name = "event_type", nullable = false, length = 50)
    private ApplicationEventType eventType;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Convert(converter = StageConverter.class)
    @Column(name = "stage_from", length = 50)
    private ApplicationStage stageFrom;

    @Convert(converter = StageConverter.class)
    @Column(name = "stage_to", length = 50)
    private ApplicationStage stageTo;

    @Column(nullable = false, length = 20)
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private Source source;

    /** Gmail message id of the email that produced this event — the idempotency key. */
    @Column(name = "source_message_id")
    private String sourceMessageId;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
