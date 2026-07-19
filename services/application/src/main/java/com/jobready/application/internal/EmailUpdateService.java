package com.jobready.application.internal;

import com.jobready.application.exception.ApplicationNotFoundException;
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

        if (eventRepository.existsByApplicationIdAndSourceMessageId(applicationId, request.sourceMessageId())) {
            return false;
        }

        ApplicationStage stageFrom = application.getStage();
        ApplicationStage stageTo =
                request.suggestedStage() == null ? null : ApplicationStage.fromValue(request.suggestedStage());
        boolean stageChanged = stageTo != null && stageTo != stageFrom;
        if (stageChanged) {
            application.setStage(stageTo);
            applicationRepository.save(application);
        }

        ApplicationEvent event = new ApplicationEvent();
        event.setUserId(request.userId());
        event.setApplicationId(applicationId);
        event.setEventType(ApplicationEventType.fromValue(request.event().eventType()));
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
        ApplicationStage stage = request.suggestedStage() == null
                ? ApplicationStage.APPLIED
                : ApplicationStage.fromValue(request.suggestedStage());
        application.setStage(stage);
        application = applicationRepository.save(application);

        ApplicationEvent event = new ApplicationEvent();
        event.setUserId(request.userId());
        event.setApplicationId(application.getId());
        event.setEventType(ApplicationEventType.fromValue(request.event().eventType()));
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
