package com.jobready.application.internal;

import com.jobready.application.exception.ApplicationNotFoundException;
import com.jobready.application.exception.InvalidWireValueException;
import com.jobready.application.generated.modelDto.ApplicationEventType;
import com.jobready.application.generated.modelDto.ApplicationStage;
import com.jobready.application.internal.InternalDtos.ApplicationCandidate;
import com.jobready.application.internal.InternalDtos.EmailCreateRequest;
import com.jobready.application.internal.InternalDtos.EmailCreateResponse;
import com.jobready.application.internal.InternalDtos.EmailRecommendation;
import com.jobready.application.internal.InternalDtos.EmailUpdateRequest;
import com.jobready.application.modelEntity.Application;
import com.jobready.application.modelEntity.ApplicationEvent;
import com.jobready.application.modelEntity.Recommendation;
import com.jobready.application.repository.ApplicationEventRepository;
import com.jobready.application.repository.ApplicationRepository;
import com.jobready.application.repository.RecommendationRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies email-derived updates coming from the email service's detection pipeline. Everything
 * derived from one email — stage change, timeline event, recommendations — lands in a single
 * transaction, deduplicated on the Gmail message id so retried deliveries are no-ops.
 */
@Service
@RequiredArgsConstructor
public class EmailUpdateService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationEventRepository eventRepository;
    private final RecommendationRepository recommendationRepository;

    @Transactional(readOnly = true)
    public List<ApplicationCandidate> listCandidates(UUID userId) {
        return applicationRepository.findByUserIdOrderByAppliedAtDesc(userId).stream()
                .map(a -> new ApplicationCandidate(
                        a.getId(), a.getCompany(), a.getJobTitle(), a.getStage().getValue(), a.getUpdatedAt()))
                .toList();
    }

    /** Returns false when this email was already applied to this application (idempotent replay). */
    @Transactional
    public boolean apply(UUID applicationId, EmailUpdateRequest request) {
        Application application = applicationRepository
                .findByIdAndUserId(applicationId, request.userId())
                .orElseThrow(ApplicationNotFoundException::new);

        // Per-user check matches the DB unique constraint: if a retried email was previously
        // matched to a DIFFERENT application, this still replays as a clean no-op instead of
        // tripping the constraint.
        if (eventRepository.existsByApplicationIdAndSourceMessageId(applicationId, request.sourceMessageId())
                || eventRepository.existsByUserIdAndSourceMessageId(request.userId(), request.sourceMessageId())) {
            return false;
        }

        ApplicationStage stageFrom = application.getStage();
        ApplicationStage stageTo = parseStage(request.suggestedStage());
        // Forward-only, enforced server-side: the LLM prompt asks for it, but prompts are not
        // guarantees (cf. genai's _sanitize). A stale email must never drag the board backwards;
        // the event is still recorded, just without the transition.
        boolean stageChanged = stageTo != null && rank(stageTo) > rank(stageFrom);
        if (stageChanged) {
            application.setStage(stageTo);
            applicationRepository.save(application);
        }

        ApplicationEvent event = new ApplicationEvent();
        event.setUserId(request.userId());
        event.setApplicationId(applicationId);
        event.setEventType(parseEventType(request.event().eventType()));
        event.setTitle(request.event().title());
        event.setDescription(request.event().description());
        if (stageChanged) {
            event.setStageFrom(stageFrom);
            event.setStageTo(stageTo);
        }
        event.setSource(ApplicationEvent.Source.EMAIL);
        event.setSourceMessageId(request.sourceMessageId());
        event.setOccurredAt(request.event().occurredAt());
        eventRepository.save(event);

        saveRecommendations(applicationId, request.userId(), request.recommendations());
        return true;
    }

    /**
     * Creates an application for a company the user does not track yet, derived from one email.
     * Idempotent on (userId, sourceMessageId); if an application for the company already exists
     * (the LLM missed the match), the update is applied to it instead of creating a duplicate.
     */
    @Transactional
    public EmailCreateResponse createFromEmail(EmailCreateRequest request) {
        if (eventRepository.existsByUserIdAndSourceMessageId(request.userId(), request.sourceMessageId())) {
            return new EmailCreateResponse(false, null);
        }

        Application existing = applicationRepository.findByUserIdOrderByAppliedAtDesc(request.userId()).stream()
                .filter(a -> a.getCompany().equalsIgnoreCase(request.company().trim()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            apply(
                    existing.getId(),
                    new EmailUpdateRequest(
                            request.userId(),
                            request.sourceMessageId(),
                            request.suggestedStage(),
                            request.event(),
                            request.recommendations()));
            return new EmailCreateResponse(false, existing.getId());
        }

        Application application = new Application();
        application.setUserId(request.userId());
        application.setCompany(request.company().trim());
        application.setJobTitle(
                request.position() == null || request.position().isBlank()
                        ? "Unknown position"
                        : request.position().trim());
        application.setJobDescription("");
        application.setNotes("Created automatically from a detected email.");
        ApplicationStage parsed = parseStage(request.suggestedStage());
        ApplicationStage stage = parsed == null ? ApplicationStage.APPLIED : parsed;
        application.setStage(stage);
        application = applicationRepository.save(application);

        ApplicationEvent event = new ApplicationEvent();
        event.setUserId(request.userId());
        event.setApplicationId(application.getId());
        event.setEventType(parseEventType(request.event().eventType()));
        event.setTitle(request.event().title());
        event.setDescription(request.event().description());
        event.setStageTo(stage);
        event.setSource(ApplicationEvent.Source.EMAIL);
        event.setSourceMessageId(request.sourceMessageId());
        event.setOccurredAt(request.event().occurredAt());
        eventRepository.save(event);

        saveRecommendations(application.getId(), request.userId(), request.recommendations());
        return new EmailCreateResponse(true, application.getId());
    }

    /** Lifecycle position of each stage; email updates may only ever move applications forward. */
    private static int rank(ApplicationStage stage) {
        return switch (stage) {
            case DRAFT -> 0;
            case APPLIED -> 1;
            case FOLLOW_UP -> 2;
            case INTERVIEW -> 3;
            case OFFER -> 4;
            case CLOSED -> 5; // terminal — reachable from anywhere
        };
    }

    private static ApplicationStage parseStage(String wireValue) {
        if (wireValue == null) {
            return null;
        }
        try {
            return ApplicationStage.fromValue(wireValue);
        } catch (IllegalArgumentException e) {
            throw new InvalidWireValueException("Unknown application stage: " + wireValue, e);
        }
    }

    private static ApplicationEventType parseEventType(String wireValue) {
        try {
            return ApplicationEventType.fromValue(wireValue);
        } catch (IllegalArgumentException e) {
            throw new InvalidWireValueException("Unknown event type: " + wireValue, e);
        }
    }

    private void saveRecommendations(UUID applicationId, UUID userId, List<EmailRecommendation> items) {
        for (EmailRecommendation item : items == null ? List.<EmailRecommendation>of() : items) {
            Recommendation recommendation = new Recommendation();
            recommendation.setUserId(userId);
            recommendation.setApplicationId(applicationId);
            recommendation.setInsight(item.insight());
            recommendation.setRecommendedAction(item.recommendedAction());
            recommendationRepository.save(recommendation);
        }
    }
}
