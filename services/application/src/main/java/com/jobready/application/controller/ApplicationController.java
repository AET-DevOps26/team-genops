package com.jobready.application.controller;

import com.jobready.application.generated.api.ApplicationsApi;
import com.jobready.application.generated.modelDto.ApplicationEventList;
import com.jobready.application.generated.modelDto.ApplicationList;
import com.jobready.application.generated.modelDto.ApplicationStage;
import com.jobready.application.generated.modelDto.ApplicationSummary;
import com.jobready.application.generated.modelDto.CreateApplicationRequest;
import com.jobready.application.generated.modelDto.CreateRecommendationRequest;
import com.jobready.application.generated.modelDto.JobApplication;
import com.jobready.application.generated.modelDto.Recommendation;
import com.jobready.application.generated.modelDto.RecommendationList;
import com.jobready.application.generated.modelDto.UpdateApplicationRequest;
import com.jobready.application.service.ApplicationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ApplicationController implements ApplicationsApi {

    private final ApplicationService applicationService;

    @Override
    public ResponseEntity<JobApplication> createApplication(CreateApplicationRequest createApplicationRequest) {
        return ResponseEntity.status(201).body(applicationService.create(currentUserId(), createApplicationRequest));
    }

    @Override
    public ResponseEntity<ApplicationList> listApplications(ApplicationStage stage, Integer limit, Integer offset) {
        return ResponseEntity.ok(applicationService.list(currentUserId(), stage, limit, offset));
    }

    @Override
    public ResponseEntity<JobApplication> getApplication(UUID id) {
        return ResponseEntity.ok(applicationService.get(currentUserId(), id));
    }

    @Override
    public ResponseEntity<JobApplication> updateApplication(
            UUID id, UpdateApplicationRequest updateApplicationRequest) {
        return ResponseEntity.ok(applicationService.update(currentUserId(), id, updateApplicationRequest));
    }

    @Override
    public ResponseEntity<Void> deleteApplication(UUID id) {
        applicationService.delete(currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ApplicationSummary> getApplicationSummary() {
        return ResponseEntity.ok(applicationService.summary(currentUserId()));
    }

    @Override
    public ResponseEntity<Recommendation> createRecommendation(
            UUID id, CreateRecommendationRequest createRecommendationRequest) {
        return ResponseEntity.status(201)
                .body(applicationService.addRecommendation(currentUserId(), id, createRecommendationRequest));
    }

    @Override
    public ResponseEntity<RecommendationList> listRecommendations(UUID id) {
        return ResponseEntity.ok(applicationService.listRecommendations(currentUserId(), id));
    }

    @Override
    public ResponseEntity<Void> deleteRecommendation(UUID id, UUID recommendationId) {
        applicationService.deleteRecommendation(currentUserId(), id, recommendationId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ApplicationEventList> listApplicationEvents(UUID id) {
        return ResponseEntity.ok(applicationService.listEvents(currentUserId(), id));
    }

    /** The owner is always the JWT {@code sub} claim — never read from the request. */
    private UUID currentUserId() {
        JwtAuthenticationToken auth =
                (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        String subject = auth.getToken().getSubject();
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            // A validly-signed token whose `sub` isn't a UUID is an authentication problem,
            // not a server error — surface it as 401 rather than letting it become a 500.
            throw new BadCredentialsException("Token subject is not a valid user id");
        }
    }
}
