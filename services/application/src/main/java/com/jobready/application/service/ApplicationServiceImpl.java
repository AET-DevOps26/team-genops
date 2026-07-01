package com.jobready.application.service;

import com.jobready.application.exception.ApplicationNotFoundException;
import com.jobready.application.generated.modelDto.CreateApplicationRequest;
import com.jobready.application.generated.modelDto.JobApplication;
import com.jobready.application.generated.modelDto.UpdateApplicationRequest;
import com.jobready.application.modelEntity.Application;
import com.jobready.application.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository repository;

    @Override
    @Transactional
    public JobApplication create(UUID userId, CreateApplicationRequest request) {
        Application application = new Application();
        application.setUserId(userId);
        application.setCompany(request.getCompany());
        application.setJobTitle(request.getJobTitle());
        application.setJobDescription(request.getJobDescription());
        application.setJobUrl(request.getJobUrl());
        application.setNotes(request.getNotes());
        // stage defaults to APPLIED on the entity — new applications always start in `applied`.
        return toDto(repository.save(application));
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobApplication> list(UUID userId) {
        return repository.findByUserIdOrderByAppliedAtDesc(userId).stream()
            .map(this::toDto)
            .toList();
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
        application.setCompany(request.getCompany());
        application.setJobTitle(request.getJobTitle());
        application.setJobDescription(request.getJobDescription());
        application.setJobUrl(request.getJobUrl());
        application.setStage(request.getStage());
        application.setNotes(request.getNotes());
        return toDto(repository.save(application));
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID id) {
        repository.delete(require(userId, id));
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
            .stage(a.getStage())
            .notes(a.getNotes())
            .appliedAt(a.getAppliedAt())
            .updatedAt(a.getUpdatedAt());
    }
}
