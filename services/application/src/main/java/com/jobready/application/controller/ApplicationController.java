package com.jobready.application.controller;

import com.jobready.application.generated.api.ApplicationsApi;
import com.jobready.application.generated.modelDto.ApplicationList;
import com.jobready.application.generated.modelDto.CreateApplicationRequest;
import com.jobready.application.generated.modelDto.JobApplication;
import com.jobready.application.generated.modelDto.UpdateApplicationRequest;
import com.jobready.application.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ApplicationController implements ApplicationsApi {

    private final ApplicationService applicationService;

    @Override
    public ResponseEntity<JobApplication> createApplication(CreateApplicationRequest createApplicationRequest) {
        return ResponseEntity.status(201).body(applicationService.create(currentUserId(), createApplicationRequest));
    }

    @Override
    public ResponseEntity<ApplicationList> listApplications() {
        return ResponseEntity.ok(new ApplicationList().items(applicationService.list(currentUserId())));
    }

    @Override
    public ResponseEntity<JobApplication> getApplication(UUID id) {
        return ResponseEntity.ok(applicationService.get(currentUserId(), id));
    }

    @Override
    public ResponseEntity<JobApplication> updateApplication(UUID id, UpdateApplicationRequest updateApplicationRequest) {
        return ResponseEntity.ok(applicationService.update(currentUserId(), id, updateApplicationRequest));
    }

    @Override
    public ResponseEntity<Void> deleteApplication(UUID id) {
        applicationService.delete(currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    /** The owner is always the JWT {@code sub} claim — never read from the request. */
    private UUID currentUserId() {
        JwtAuthenticationToken auth = (JwtAuthenticationToken)
            SecurityContextHolder.getContext().getAuthentication();
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
