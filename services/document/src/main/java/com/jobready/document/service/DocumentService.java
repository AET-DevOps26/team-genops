package com.jobready.document.service;

import com.jobready.document.generated.modelDto.CreateGeneratedDocumentRequest;
import com.jobready.document.generated.modelDto.Education;
import com.jobready.document.generated.modelDto.EducationRequest;
import com.jobready.document.generated.modelDto.GeneratedDocument;
import com.jobready.document.generated.modelDto.Language;
import com.jobready.document.generated.modelDto.LanguageRequest;
import com.jobready.document.generated.modelDto.Profile;
import com.jobready.document.generated.modelDto.ProfileAggregateResponse;
import com.jobready.document.generated.modelDto.ProfileRequest;
import com.jobready.document.generated.modelDto.Skill;
import com.jobready.document.generated.modelDto.SkillRequest;
import com.jobready.document.generated.modelDto.WorkExperience;
import com.jobready.document.generated.modelDto.WorkExperienceRequest;
import java.util.List;
import java.util.UUID;

public interface DocumentService {

    /** The full profile aggregate; throws {@code ProfileNotFoundException} when none exists. */
    ProfileAggregateResponse getProfile(UUID userId);

    /** Creates the profile on first call, updates the basics afterwards. */
    Profile upsertProfile(UUID userId, ProfileRequest request);

    WorkExperience createWorkExperience(UUID userId, WorkExperienceRequest request);

    WorkExperience updateWorkExperience(UUID userId, UUID id, WorkExperienceRequest request);

    void deleteWorkExperience(UUID userId, UUID id);

    Education createEducation(UUID userId, EducationRequest request);

    Education updateEducation(UUID userId, UUID id, EducationRequest request);

    void deleteEducation(UUID userId, UUID id);

    Skill createSkill(UUID userId, SkillRequest request);

    Skill updateSkill(UUID userId, UUID id, SkillRequest request);

    void deleteSkill(UUID userId, UUID id);

    Language createLanguage(UUID userId, LanguageRequest request);

    Language updateLanguage(UUID userId, UUID id, LanguageRequest request);

    void deleteLanguage(UUID userId, UUID id);

    /** Generated documents (cover letters + resumes), newest first, optionally per application. */
    List<GeneratedDocument> listDocuments(UUID userId, UUID applicationId);

    GeneratedDocument createDocument(UUID userId, CreateGeneratedDocumentRequest request);

    void deleteDocument(UUID userId, UUID id);
}
