package com.jobready.application.service;

import com.jobready.application.exception.ApplicationNotFoundException;
import com.jobready.application.generated.modelDto.ApplicationEventList;
import com.jobready.application.generated.modelDto.ApplicationEventType;
import com.jobready.application.generated.modelDto.ApplicationList;
import com.jobready.application.generated.modelDto.ApplicationStage;
import com.jobready.application.generated.modelDto.ApplicationSummary;
import com.jobready.application.generated.modelDto.CreateApplicationRequest;
import com.jobready.application.generated.modelDto.CreateRecommendationRequest;
import com.jobready.application.generated.modelDto.JobApplication;
import com.jobready.application.generated.modelDto.RecommendationList;
import com.jobready.application.generated.modelDto.UpdateApplicationRequest;
import com.jobready.application.modelEntity.Application;
import com.jobready.application.modelEntity.ApplicationEvent;
import com.jobready.application.modelEntity.Recommendation;
import com.jobready.application.repository.ApplicationEventRepository;
import com.jobready.application.repository.ApplicationRepository;
import com.jobready.application.repository.OffsetPageRequest;
import com.jobready.application.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository repository;
    private final RecommendationRepository recommendationRepository;
    private final ApplicationEventRepository eventRepository;

    @Override
    @Transactional
    public JobApplication create(UUID userId, CreateApplicationRequest request) {
        Application application = new Application();
        application.setUserId(userId);
        application.setCompany(request.getCompany());
        application.setJobTitle(request.getJobTitle());
        application.setJobDescription(request.getJobDescription());
        application.setJobUrl(request.getJobUrl());
        application.setCompanyWebsite(request.getCompanyWebsite());
        application.setLinkedinUrl(request.getLinkedinUrl());
        application.setNotes(request.getNotes());
        // stage defaults to APPLIED on the entity — new applications always start in `applied`.
        return toDto(repository.save(application));
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationList list(UUID userId, ApplicationStage stage, Integer limit, Integer offset) {
        long from = offset == null ? 0 : offset;
        // No limit means "everything after the offset" — cap by the row count instead of a magic number.
        long total = stage == null
            ? repository.countByUserId(userId)
            : repository.countByUserIdAndStage(userId, stage);
        int pageSize = limit != null ? limit : (int) Math.max(1, total - from);

        List<Application> page;
        if (total == 0 || from >= total) {
            page = List.of();
        } else {
            Pageable pageable = new OffsetPageRequest(from, pageSize);
            page = stage == null
                ? repository.findByUserIdOrderByAppliedAtDesc(userId, pageable)
                : repository.findByUserIdAndStageOrderByAppliedAtDesc(userId, stage, pageable);
        }
        return new ApplicationList()
            .items(page.stream().map(this::toDto).toList())
            .total(total);
    }

    @Override
    @Transactional(readOnly = true)
    public JobApplication get(UUID userId, UUID id) {
        return toDto(require(userId, id));
    }

    @Override
    @Transactional
    public JobApplication update(UUID userId, UUID id, UpdateApplicationRequest request) {
        Application application = require(userId, id);
        ApplicationStage previousStage = application.getStage();
        application.setCompany(request.getCompany());
        application.setJobTitle(request.getJobTitle());
        application.setJobDescription(request.getJobDescription());
        application.setJobUrl(request.getJobUrl());
        application.setCompanyWebsite(request.getCompanyWebsite());
        application.setLinkedinUrl(request.getLinkedinUrl());
        application.setStage(request.getStage());
        application.setNotes(request.getNotes());
        if (previousStage != request.getStage()) {
            recordStageChange(application, previousStage, request.getStage());
        }
        return toDto(repository.save(application));
    }

    /** Manual stage transitions land on the timeline too, so it reflects the full history. */
    private void recordStageChange(Application application, ApplicationStage from, ApplicationStage to) {
        ApplicationEvent event = new ApplicationEvent();
        event.setUserId(application.getUserId());
        event.setApplicationId(application.getId());
        event.setEventType(ApplicationEventType.STAGE_CHANGE);
        event.setTitle("Stage changed to " + to.getValue());
        event.setStageFrom(from);
        event.setStageTo(to);
        event.setSource(ApplicationEvent.Source.MANUAL);
        event.setOccurredAt(OffsetDateTime.now());
        eventRepository.save(event);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID id) {
        Application application = require(userId, id);
        recommendationRepository.deleteByApplicationIdAndUserId(id, userId);
        eventRepository.deleteByApplicationIdAndUserId(id, userId);
        repository.delete(application);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationSummary summary(UUID userId) {
        Map<ApplicationStage, Long> counts = repository.countByUserIdGroupedByStage(userId).stream()
            .collect(Collectors.toMap(ApplicationRepository.StageCount::getStage,
                                      ApplicationRepository.StageCount::getCount));
        long applied = counts.getOrDefault(ApplicationStage.APPLIED, 0L);
        long followUp = counts.getOrDefault(ApplicationStage.FOLLOW_UP, 0L);
        long interview = counts.getOrDefault(ApplicationStage.INTERVIEW, 0L);
        long offer = counts.getOrDefault(ApplicationStage.OFFER, 0L);
        long closed = counts.getOrDefault(ApplicationStage.CLOSED, 0L);
        return new ApplicationSummary()
            .applied(applied)
            .followUp(followUp)
            .interview(interview)
            .offer(offer)
            .closed(closed)
            .total(applied + followUp + interview + offer + closed);
    }

    @Override
    @Transactional
    public com.jobready.application.generated.modelDto.Recommendation addRecommendation(
            UUID userId, UUID applicationId, CreateRecommendationRequest request) {
        require(userId, applicationId); // parent must belong to the caller — 404 otherwise
        Recommendation recommendation = new Recommendation();
        recommendation.setUserId(userId);
        recommendation.setApplicationId(applicationId);
        recommendation.setInsight(request.getInsight());
        recommendation.setRecommendedAction(request.getRecommendedAction());
        return toDto(recommendationRepository.save(recommendation));
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationList listRecommendations(UUID userId, UUID applicationId) {
        require(userId, applicationId);
        return new RecommendationList().items(
            recommendationRepository.findByApplicationIdAndUserIdOrderByCreatedAtDesc(applicationId, userId)
                .stream().map(this::toDto).toList());
    }

    @Override
    @Transactional
    public void deleteRecommendation(UUID userId, UUID applicationId, UUID recommendationId) {
        require(userId, applicationId);
        Recommendation recommendation = recommendationRepository.findByIdAndUserId(recommendationId, userId)
            .filter(r -> r.getApplicationId().equals(applicationId))
            .orElseThrow(ApplicationNotFoundException::new);
        recommendationRepository.delete(recommendation);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationEventList listEvents(UUID userId, UUID applicationId) {
        require(userId, applicationId);
        return new ApplicationEventList().items(
            eventRepository.findByApplicationIdAndUserIdOrderByOccurredAtDesc(applicationId, userId)
                .stream().map(this::toDto).toList());
    }

    private com.jobready.application.generated.modelDto.ApplicationEvent toDto(ApplicationEvent e) {
        return new com.jobready.application.generated.modelDto.ApplicationEvent()
            .id(e.getId())
            .applicationId(e.getApplicationId())
            .eventType(e.getEventType())
            .title(e.getTitle())
            .description(e.getDescription())
            .stageFrom(e.getStageFrom())
            .stageTo(e.getStageTo())
            .source(com.jobready.application.generated.modelDto.ApplicationEvent.SourceEnum
                .fromValue(e.getSource().name().toLowerCase()))
            .occurredAt(e.getOccurredAt())
            .createdAt(e.getCreatedAt());
    }

    private Application require(UUID userId, UUID id) {
        return repository.findByIdAndUserId(id, userId)
            .orElseThrow(ApplicationNotFoundException::new);
    }

    private JobApplication toDto(Application a) {
        return new JobApplication()
            .id(a.getId())
            .company(a.getCompany())
            .jobTitle(a.getJobTitle())
            .jobDescription(a.getJobDescription())
            .jobUrl(a.getJobUrl())
            .companyWebsite(a.getCompanyWebsite())
            .linkedinUrl(a.getLinkedinUrl())
            .stage(a.getStage())
            .notes(a.getNotes())
            .appliedAt(a.getAppliedAt())
            .updatedAt(a.getUpdatedAt());
    }

    private com.jobready.application.generated.modelDto.Recommendation toDto(Recommendation r) {
        return new com.jobready.application.generated.modelDto.Recommendation()
            .id(r.getId())
            .applicationId(r.getApplicationId())
            .insight(r.getInsight())
            .recommendedAction(r.getRecommendedAction())
            .createdAt(r.getCreatedAt());
    }
}
