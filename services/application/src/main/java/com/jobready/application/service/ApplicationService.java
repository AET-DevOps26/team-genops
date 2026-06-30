package com.jobready.application.service;

import com.jobready.application.generated.modelDto.CreateApplicationRequest;
import com.jobready.application.generated.modelDto.JobApplication;
import com.jobready.application.generated.modelDto.UpdateApplicationRequest;

import java.util.List;
import java.util.UUID;

public interface ApplicationService {

    JobApplication create(UUID userId, CreateApplicationRequest request);

    List<JobApplication> list(UUID userId);

    JobApplication get(UUID userId, UUID id);

    JobApplication update(UUID userId, UUID id, UpdateApplicationRequest request);

    void delete(UUID userId, UUID id);
}
