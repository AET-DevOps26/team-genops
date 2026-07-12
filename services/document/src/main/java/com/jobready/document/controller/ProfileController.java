package com.jobready.document.controller;

import com.jobready.document.generated.api.DocumentsApi;
import com.jobready.document.generated.modelDto.CreateGeneratedDocumentRequest;
import com.jobready.document.generated.modelDto.Education;
import com.jobready.document.generated.modelDto.EducationRequest;
import com.jobready.document.generated.modelDto.GeneratedDocument;
import com.jobready.document.generated.modelDto.GeneratedDocumentList;
import com.jobready.document.generated.modelDto.Language;
import com.jobready.document.generated.modelDto.LanguageRequest;
import com.jobready.document.generated.modelDto.Profile;
import com.jobready.document.generated.modelDto.ProfileAggregateResponse;
import com.jobready.document.generated.modelDto.ProfileRequest;
import com.jobready.document.generated.modelDto.Skill;
import com.jobready.document.generated.modelDto.SkillRequest;
import com.jobready.document.generated.modelDto.WorkExperience;
import com.jobready.document.generated.modelDto.WorkExperienceRequest;
import com.jobready.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProfileController implements DocumentsApi {

    private final DocumentService documentService;

    @Override
    public ResponseEntity<ProfileAggregateResponse> getProfile() {
        return ResponseEntity.ok(documentService.getProfile(currentUserId()));
    }

    @Override
    public ResponseEntity<Profile> upsertProfile(ProfileRequest profileRequest) {
        return ResponseEntity.ok(documentService.upsertProfile(currentUserId(), profileRequest));
    }

    @Override
    public ResponseEntity<WorkExperience> createWorkExperience(WorkExperienceRequest workExperienceRequest) {
        return ResponseEntity.status(201)
            .body(documentService.createWorkExperience(currentUserId(), workExperienceRequest));
    }

    @Override
    public ResponseEntity<WorkExperience> updateWorkExperience(UUID id, WorkExperienceRequest workExperienceRequest) {
        return ResponseEntity.ok(documentService.updateWorkExperience(currentUserId(), id, workExperienceRequest));
    }

    @Override
    public ResponseEntity<Void> deleteWorkExperience(UUID id) {
        documentService.deleteWorkExperience(currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Education> createEducation(EducationRequest educationRequest) {
        return ResponseEntity.status(201)
            .body(documentService.createEducation(currentUserId(), educationRequest));
    }

    @Override
    public ResponseEntity<Education> updateEducation(UUID id, EducationRequest educationRequest) {
        return ResponseEntity.ok(documentService.updateEducation(currentUserId(), id, educationRequest));
    }

    @Override
    public ResponseEntity<Void> deleteEducation(UUID id) {
        documentService.deleteEducation(currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Skill> createSkill(SkillRequest skillRequest) {
        return ResponseEntity.status(201)
            .body(documentService.createSkill(currentUserId(), skillRequest));
    }

    @Override
    public ResponseEntity<Skill> updateSkill(UUID id, SkillRequest skillRequest) {
        return ResponseEntity.ok(documentService.updateSkill(currentUserId(), id, skillRequest));
    }

    @Override
    public ResponseEntity<Void> deleteSkill(UUID id) {
        documentService.deleteSkill(currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Language> createLanguage(LanguageRequest languageRequest) {
        return ResponseEntity.status(201)
            .body(documentService.createLanguage(currentUserId(), languageRequest));
    }

    @Override
    public ResponseEntity<Language> updateLanguage(UUID id, LanguageRequest languageRequest) {
        return ResponseEntity.ok(documentService.updateLanguage(currentUserId(), id, languageRequest));
    }

    @Override
    public ResponseEntity<Void> deleteLanguage(UUID id) {
        documentService.deleteLanguage(currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<GeneratedDocumentList> listDocuments(UUID applicationId) {
        return ResponseEntity.ok(new GeneratedDocumentList()
            .items(documentService.listDocuments(currentUserId(), applicationId)));
    }

    @Override
    public ResponseEntity<GeneratedDocument> createDocument(CreateGeneratedDocumentRequest createGeneratedDocumentRequest) {
        return ResponseEntity.status(201)
            .body(documentService.createDocument(currentUserId(), createGeneratedDocumentRequest));
    }

    @Override
    public ResponseEntity<Void> deleteDocument(UUID id) {
        documentService.deleteDocument(currentUserId(), id);
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
