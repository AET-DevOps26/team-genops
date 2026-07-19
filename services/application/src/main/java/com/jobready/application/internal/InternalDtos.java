package com.jobready.application.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Request/response bodies of the {@code /internal/**} API. Deliberately hand-written and kept
 * out of {@code api/openapi.yaml}: the spec is the public contract consumed by the web client,
 * while these are a trusted machine-to-machine contract with a different auth model (see
 * {@link InternalTokenFilter}). Documented in the service README.
 */
public final class InternalDtos {

    private InternalDtos() {}

    /** Slim application view returned to the email service as matching candidates. */
    public record ApplicationCandidate(
            UUID id, String company, String jobTitle, String stage, OffsetDateTime updatedAt) {}

    /** A timeline event derived from one email. */
    public record EmailEvent(
            @NotBlank String eventType,
            @NotBlank String title,
            String description,
            @NotNull OffsetDateTime occurredAt) {}

    /** A next-best-action item derived from one email. */
    public record EmailRecommendation(@NotBlank String insight, @NotBlank String recommendedAction) {}

    /**
     * Everything the pipeline derived from one email, applied atomically. {@code userId} is
     * trusted here because the caller authenticated with the internal service token.
     */
    public record EmailUpdateRequest(
            @NotNull UUID userId,
            @NotBlank String sourceMessageId,
            String suggestedStage,
            @NotNull @Valid EmailEvent event,
            @Valid List<EmailRecommendation> recommendations) {}

    /** Outcome of an email-update call. {@code applied} is false for an idempotent replay. */
    public record EmailUpdateResponse(boolean applied) {}

    /**
     * Everything needed to auto-create an application the user does not track yet, derived
     * from one email. {@code suggestedStage} is {@code applied} or {@code interview} (invite).
     */
    public record EmailCreateRequest(
            @NotNull UUID userId,
            @NotBlank String sourceMessageId,
            @NotBlank String company,
            String position,
            String suggestedStage,
            @NotNull @Valid EmailEvent event,
            @Valid List<EmailRecommendation> recommendations) {}

    /**
     * Outcome of an email-create call. {@code created} is false for an idempotent replay or
     * when the company already had an application (the update was applied to it instead);
     * {@code applicationId} names the created or matched application, if any.
     */
    public record EmailCreateResponse(boolean created, UUID applicationId) {}
}
