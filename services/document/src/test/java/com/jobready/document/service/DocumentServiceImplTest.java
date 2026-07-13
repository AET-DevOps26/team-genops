package com.jobready.document.service;

import com.jobready.document.exception.ProfileNotFoundException;
import com.jobready.document.exception.ResourceNotFoundException;
import com.jobready.document.generated.modelDto.CreateGeneratedDocumentRequest;
import com.jobready.document.generated.modelDto.GeneratedDocument;
import com.jobready.document.generated.modelDto.GeneratedDocumentType;
import com.jobready.document.generated.modelDto.Profile;
import com.jobready.document.generated.modelDto.ProfileAggregateResponse;
import com.jobready.document.generated.modelDto.ProfileRequest;
import com.jobready.document.generated.modelDto.SkillLevel;
import com.jobready.document.generated.modelDto.SkillRequest;
import com.jobready.document.generated.modelDto.WorkExperience;
import com.jobready.document.generated.modelDto.WorkExperienceRequest;
import com.jobready.document.modelEntity.CoverLetterEntity;
import com.jobready.document.modelEntity.ProfileEntity;
import com.jobready.document.modelEntity.ResumeEntity;
import com.jobready.document.modelEntity.SkillEntity;
import com.jobready.document.modelEntity.WorkExperienceEntity;
import com.jobready.document.repository.CoverLetterRepository;
import com.jobready.document.repository.EducationRepository;
import com.jobready.document.repository.LanguageRepository;
import com.jobready.document.repository.ProfileRepository;
import com.jobready.document.repository.ResumeRepository;
import com.jobready.document.repository.SkillRepository;
import com.jobready.document.repository.WorkExperienceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private WorkExperienceRepository workExperienceRepository;
    @Mock
    private EducationRepository educationRepository;
    @Mock
    private SkillRepository skillRepository;
    @Mock
    private LanguageRepository languageRepository;
    @Mock
    private CoverLetterRepository coverLetterRepository;
    @Mock
    private ResumeRepository resumeRepository;

    @InjectMocks
    private DocumentServiceImpl service;

    private final UUID userId = UUID.randomUUID();

    // ------------------------------------------------------------------
    // Profile
    // ------------------------------------------------------------------

    @Test
    void getProfile_whenNoProfile_throwsNotFound() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile(userId))
            .isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    void getProfile_returnsAggregateWithAllSubResources() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile(userId)));
        when(workExperienceRepository.findByUserIdOrderByStartDateDesc(userId))
            .thenReturn(List.of(workExperience(userId)));
        when(educationRepository.findByUserIdOrderByStartDateDesc(userId)).thenReturn(List.of());
        when(skillRepository.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());
        when(languageRepository.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());

        ProfileAggregateResponse result = service.getProfile(userId);

        assertThat(result.getProfile().getFirstName()).isEqualTo("Jane");
        assertThat(result.getWorkExperiences()).hasSize(1);
        assertThat(result.getEducations()).isEmpty();
        assertThat(result.getSkills()).isEmpty();
        assertThat(result.getLanguages()).isEmpty();
    }

    @Test
    void upsertProfile_createsWhenMissing_andSetsOwnerFromArgument() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(profileRepository.saveAndFlush(any(ProfileEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        Profile result = service.upsertProfile(userId,
            new ProfileRequest("Jane", "Doe").location("Munich"));

        ArgumentCaptor<ProfileEntity> saved = ArgumentCaptor.forClass(ProfileEntity.class);
        verify(profileRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(userId);
        assertThat(result.getFirstName()).isEqualTo("Jane");
        assertThat(result.getLocation()).isEqualTo("Munich");
    }

    @Test
    void upsertProfile_updatesExistingRowInsteadOfCreating() {
        ProfileEntity existing = profile(userId);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(profileRepository.saveAndFlush(any(ProfileEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        service.upsertProfile(userId, new ProfileRequest("Janet", "Doe"));

        assertThat(existing.getFirstName()).isEqualTo("Janet");
        verify(profileRepository).saveAndFlush(existing);
    }

    @Test
    void upsertProfile_retriesAsUpdateWhenConcurrentCreateWinsTheRace() {
        // First attempt: no row yet, insert loses the race on the user_id unique constraint.
        // Retry: the winner's row is visible now and gets updated instead.
        ProfileEntity winner = profile(userId);
        when(profileRepository.findByUserId(userId))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(winner));
        when(profileRepository.saveAndFlush(any(ProfileEntity.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate key: profiles_user_id_key"))
            .thenAnswer(inv -> inv.getArgument(0));

        Profile result = service.upsertProfile(userId, new ProfileRequest("Janet", "Doe"));

        assertThat(result.getFirstName()).isEqualTo("Janet");
        assertThat(winner.getFirstName()).isEqualTo("Janet");
        verify(profileRepository, times(2)).saveAndFlush(any(ProfileEntity.class));
    }

    // ------------------------------------------------------------------
    // Sub-resources
    // ------------------------------------------------------------------

    @Test
    void createWorkExperience_whenNoProfile_throwsNotFound() {
        when(profileRepository.existsByUserId(userId)).thenReturn(false);

        WorkExperienceRequest request =
            new WorkExperienceRequest("Acme", "Engineer", LocalDate.of(2023, 1, 1));

        assertThatThrownBy(() -> service.createWorkExperience(userId, request))
            .isInstanceOf(ProfileNotFoundException.class);
        verify(workExperienceRepository, never()).save(any());
    }

    @Test
    void createWorkExperience_setsOwnerFromArgument() {
        when(profileRepository.existsByUserId(userId)).thenReturn(true);
        when(workExperienceRepository.saveAndFlush(any(WorkExperienceEntity.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        WorkExperience result = service.createWorkExperience(userId,
            new WorkExperienceRequest("Acme", "Engineer", LocalDate.of(2023, 1, 1)).isCurrent(true));

        ArgumentCaptor<WorkExperienceEntity> saved = ArgumentCaptor.forClass(WorkExperienceEntity.class);
        verify(workExperienceRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(userId);
        assertThat(saved.getValue().isCurrent()).isTrue();
        assertThat(result.getCompany()).isEqualTo("Acme");
    }

    @Test
    void updateWorkExperience_whenNotOwned_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(workExperienceRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());

        WorkExperienceRequest request =
            new WorkExperienceRequest("Acme", "Engineer", LocalDate.of(2023, 1, 1));

        assertThatThrownBy(() -> service.updateWorkExperience(userId, id, request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteSkill_whenNotOwned_throwsAndDoesNotDelete() {
        UUID id = UUID.randomUUID();
        when(skillRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteSkill(userId, id))
            .isInstanceOf(ResourceNotFoundException.class);
        verify(skillRepository, never()).delete(any());
    }

    @Test
    void createSkill_persistsWireLevelEnum() {
        when(profileRepository.existsByUserId(userId)).thenReturn(true);
        when(skillRepository.saveAndFlush(any(SkillEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createSkill(userId, new SkillRequest("Java", SkillLevel.ADVANCED));

        ArgumentCaptor<SkillEntity> saved = ArgumentCaptor.forClass(SkillEntity.class);
        verify(skillRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getLevel()).isEqualTo(SkillLevel.ADVANCED);
        assertThat(saved.getValue().getLevel().getValue()).isEqualTo("advanced");
    }

    // ------------------------------------------------------------------
    // Generated documents
    // ------------------------------------------------------------------

    @Test
    void createDocument_coverLetter_landsInCoverLettersTable() {
        when(coverLetterRepository.saveAndFlush(any(CoverLetterEntity.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        UUID applicationId = UUID.randomUUID();

        GeneratedDocument result = service.createDocument(userId,
            new CreateGeneratedDocumentRequest(applicationId, GeneratedDocumentType.COVER_LETTER, "Dear team"));

        ArgumentCaptor<CoverLetterEntity> saved = ArgumentCaptor.forClass(CoverLetterEntity.class);
        verify(coverLetterRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(userId);
        assertThat(saved.getValue().getApplicationId()).isEqualTo(applicationId);
        assertThat(result.getType()).isEqualTo(GeneratedDocumentType.COVER_LETTER);
        verify(resumeRepository, never()).save(any());
    }

    @Test
    void createDocument_resume_landsInResumesTable() {
        when(resumeRepository.saveAndFlush(any(ResumeEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        GeneratedDocument result = service.createDocument(userId,
            new CreateGeneratedDocumentRequest(UUID.randomUUID(), GeneratedDocumentType.RESUME, "..."));

        assertThat(result.getType()).isEqualTo(GeneratedDocumentType.RESUME);
        verify(coverLetterRepository, never()).save(any());
    }

    @Test
    void listDocuments_mergesBothTypesNewestFirst_scopedToApplication() {
        UUID applicationId = UUID.randomUUID();
        CoverLetterEntity older = coverLetter(userId, applicationId, OffsetDateTime.now().minusHours(2));
        ResumeEntity newer = resume(userId, applicationId, OffsetDateTime.now());
        when(coverLetterRepository.findByUserIdAndApplicationIdOrderByCreatedAtDesc(userId, applicationId))
            .thenReturn(List.of(older));
        when(resumeRepository.findByUserIdAndApplicationIdOrderByCreatedAtDesc(userId, applicationId))
            .thenReturn(List.of(newer));

        List<GeneratedDocument> result = service.listDocuments(userId, applicationId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getType()).isEqualTo(GeneratedDocumentType.RESUME);
        assertThat(result.get(1).getType()).isEqualTo(GeneratedDocumentType.COVER_LETTER);
    }

    @Test
    void deleteDocument_fallsBackToResumes_andThrowsWhenNowhere() {
        UUID id = UUID.randomUUID();
        when(coverLetterRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());
        when(resumeRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteDocument(userId, id))
            .isInstanceOf(ResourceNotFoundException.class);
        verify(coverLetterRepository, never()).delete(any());
        verify(resumeRepository, never()).delete(any());
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private ProfileEntity profile(UUID owner) {
        ProfileEntity p = new ProfileEntity();
        p.setId(UUID.randomUUID());
        p.setUserId(owner);
        p.setFirstName("Jane");
        p.setLastName("Doe");
        return p;
    }

    private WorkExperienceEntity workExperience(UUID owner) {
        WorkExperienceEntity w = new WorkExperienceEntity();
        w.setId(UUID.randomUUID());
        w.setUserId(owner);
        w.setCompany("Acme");
        w.setRole("Engineer");
        w.setStartDate(LocalDate.of(2023, 1, 1));
        return w;
    }

    private CoverLetterEntity coverLetter(UUID owner, UUID applicationId, OffsetDateTime createdAt) {
        CoverLetterEntity c = new CoverLetterEntity();
        c.setId(UUID.randomUUID());
        c.setUserId(owner);
        c.setApplicationId(applicationId);
        c.setContent("Dear team");
        c.setCreatedAt(createdAt);
        return c;
    }

    private ResumeEntity resume(UUID owner, UUID applicationId, OffsetDateTime createdAt) {
        ResumeEntity r = new ResumeEntity();
        r.setId(UUID.randomUUID());
        r.setUserId(owner);
        r.setApplicationId(applicationId);
        r.setContent("...");
        r.setCreatedAt(createdAt);
        return r;
    }
}
