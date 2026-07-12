package com.jobready.application.service;

import com.jobready.application.generated.modelDto.ApplicationList;
import com.jobready.application.generated.modelDto.ApplicationStage;
import com.jobready.application.generated.modelDto.ApplicationSummary;
import com.jobready.application.generated.modelDto.CreateApplicationRequest;
import com.jobready.application.generated.modelDto.CreateRecommendationRequest;
import com.jobready.application.generated.modelDto.JobApplication;
import com.jobready.application.generated.modelDto.Recommendation;
import com.jobready.application.generated.modelDto.RecommendationList;
import com.jobready.application.generated.modelDto.UpdateApplicationRequest;

import java.util.UUID;

public interface ApplicationService {

    JobApplication create(UUID userId, CreateApplicationRequest request);

    /** Stage-filtered, paginated list; {@code total} reflects the full filtered count. */
    ApplicationList list(UUID userId, ApplicationStage stage, Integer limit, Integer offset);

    JobApplication get(UUID userId, UUID id);

    JobApplication update(UUID userId, UUID id, UpdateApplicationRequest request);

    void delete(UUID userId, UUID id);

    ApplicationSummary summary(UUID userId);

    Recommendation addRecommendation(UUID userId, UUID applicationId, CreateRecommendationRequest request);

    RecommendationList listRecommendations(UUID userId, UUID applicationId);

    void deleteRecommendation(UUID userId, UUID applicationId, UUID recommendationId);
}
