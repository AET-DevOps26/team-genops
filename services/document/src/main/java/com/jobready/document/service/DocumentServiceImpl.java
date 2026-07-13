package com.jobready.document.service;

import com.jobready.document.exception.ProfileNotFoundException;
import com.jobready.document.exception.ResourceNotFoundException;
import com.jobready.document.generated.modelDto.CreateGeneratedDocumentRequest;
import com.jobready.document.generated.modelDto.Education;
import com.jobready.document.generated.modelDto.EducationRequest;
import com.jobready.document.generated.modelDto.GeneratedDocument;
import com.jobready.document.generated.modelDto.GeneratedDocumentType;
import com.jobready.document.generated.modelDto.Language;
import com.jobready.document.generated.modelDto.LanguageRequest;
import com.jobready.document.generated.modelDto.Profile;
import com.jobready.document.generated.modelDto.ProfileAggregateResponse;
import com.jobready.document.generated.modelDto.ProfileRequest;
import com.jobready.document.generated.modelDto.Skill;
import com.jobready.document.generated.modelDto.SkillRequest;
import com.jobready.document.generated.modelDto.WorkExperience;
import com.jobready.document.generated.modelDto.WorkExperienceRequest;
import com.jobready.document.modelEntity.CoverLetterEntity;
import com.jobready.document.modelEntity.EducationEntity;
import com.jobready.document.modelEntity.LanguageEntity;
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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final ProfileRepository profileRepository;
    private final WorkExperienceRepository workExperienceRepository;
    private final EducationRepository educationRepository;
    private final SkillRepository skillRepository;
    private final LanguageRepository languageRepository;
    private final CoverLetterRepository coverLetterRepository;
    private final ResumeRepository resumeRepository;

    // ------------------------------------------------------------------
    // Profile
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ProfileAggregateResponse getProfile(UUID userId) {
        ProfileEntity profile = profileRepository.findByUserId(userId)
            .orElseThrow(ProfileNotFoundException::new);
        return new ProfileAggregateResponse()
            .profile(toDto(profile))
            .workExperiences(workExperienceRepository.findByUserIdOrderByStartDateDesc(userId)
                .stream().map(this::toDto).toList())
            .educations(educationRepository.findByUserIdOrderByStartDateDesc(userId)
                .stream().map(this::toDto).toList())
            .skills(skillRepository.findByUserIdOrderByCreatedAtAsc(userId)
                .stream().map(this::toDto).toList())
            .languages(languageRepository.findByUserIdOrderByCreatedAtAsc(userId)
                .stream().map(this::toDto).toList());
    }

    // Deliberately NOT @Transactional: a unique-constraint violation would mark a wrapping
    // transaction rollback-only and doom the retry. Each repository call transacts on its own.
    @Override
    public Profile upsertProfile(UUID userId, ProfileRequest request) {
        try {
            return applyProfileUpsert(userId, request);
        } catch (DataIntegrityViolationException ex) {
            // Two concurrent first-time upserts raced on the `user_id` unique constraint.
            // The winner's row exists now, so the retry deterministically takes the update path.
            return applyProfileUpsert(userId, request);
        }
    }

    private Profile applyProfileUpsert(UUID userId, ProfileRequest request) {
        ProfileEntity profile = profileRepository.findByUserId(userId)
            .orElseGet(() -> {
                ProfileEntity created = new ProfileEntity();
                created.setUserId(userId);
                return created;
            });
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setBio(request.getBio());
        profile.setLocation(request.getLocation());
        profile.setPhone(request.getPhone());
        profile.setWebsite(request.getWebsite());
        return toDto(profileRepository.saveAndFlush(profile));
    }

    // ------------------------------------------------------------------
    // Work experiences
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public WorkExperience createWorkExperience(UUID userId, WorkExperienceRequest request) {
        requireProfile(userId);
        WorkExperienceEntity entity = new WorkExperienceEntity();
        entity.setUserId(userId);
        apply(entity, request);
        return toDto(workExperienceRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public WorkExperience updateWorkExperience(UUID userId, UUID id, WorkExperienceRequest request) {
        WorkExperienceEntity entity = workExperienceRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Work experience"));
        apply(entity, request);
        return toDto(workExperienceRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public void deleteWorkExperience(UUID userId, UUID id) {
        WorkExperienceEntity entity = workExperienceRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Work experience"));
        workExperienceRepository.delete(entity);
    }

    // ------------------------------------------------------------------
    // Educations
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public Education createEducation(UUID userId, EducationRequest request) {
        requireProfile(userId);
        EducationEntity entity = new EducationEntity();
        entity.setUserId(userId);
        apply(entity, request);
        return toDto(educationRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public Education updateEducation(UUID userId, UUID id, EducationRequest request) {
        EducationEntity entity = educationRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Education"));
        apply(entity, request);
        return toDto(educationRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public void deleteEducation(UUID userId, UUID id) {
        EducationEntity entity = educationRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Education"));
        educationRepository.delete(entity);
    }

    // ------------------------------------------------------------------
    // Skills
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public Skill createSkill(UUID userId, SkillRequest request) {
        requireProfile(userId);
        SkillEntity entity = new SkillEntity();
        entity.setUserId(userId);
        entity.setName(request.getName());
        entity.setLevel(request.getLevel());
        return toDto(skillRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public Skill updateSkill(UUID userId, UUID id, SkillRequest request) {
        SkillEntity entity = skillRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Skill"));
        entity.setName(request.getName());
        entity.setLevel(request.getLevel());
        return toDto(skillRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public void deleteSkill(UUID userId, UUID id) {
        SkillEntity entity = skillRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Skill"));
        skillRepository.delete(entity);
    }

    // ------------------------------------------------------------------
    // Languages
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public Language createLanguage(UUID userId, LanguageRequest request) {
        requireProfile(userId);
        LanguageEntity entity = new LanguageEntity();
        entity.setUserId(userId);
        entity.setName(request.getName());
        entity.setProficiency(request.getProficiency());
        return toDto(languageRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public Language updateLanguage(UUID userId, UUID id, LanguageRequest request) {
        LanguageEntity entity = languageRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Language"));
        entity.setName(request.getName());
        entity.setProficiency(request.getProficiency());
        return toDto(languageRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public void deleteLanguage(UUID userId, UUID id) {
        LanguageEntity entity = languageRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Language"));
        languageRepository.delete(entity);
    }

    // ------------------------------------------------------------------
    // Generated documents (cover letters + resumes, unified endpoint)
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<GeneratedDocument> listDocuments(UUID userId, UUID applicationId) {
        List<CoverLetterEntity> coverLetters = applicationId == null
            ? coverLetterRepository.findByUserIdOrderByCreatedAtDesc(userId)
            : coverLetterRepository.findByUserIdAndApplicationIdOrderByCreatedAtDesc(userId, applicationId);
        List<ResumeEntity> resumes = applicationId == null
            ? resumeRepository.findByUserIdOrderByCreatedAtDesc(userId)
            : resumeRepository.findByUserIdAndApplicationIdOrderByCreatedAtDesc(userId, applicationId);
        return Stream.concat(
                coverLetters.stream().map(this::toDto),
                resumes.stream().map(this::toDto))
            .sorted(Comparator.comparing(GeneratedDocument::getCreatedAt).reversed())
            .toList();
    }

    @Override
    @Transactional
    public GeneratedDocument createDocument(UUID userId, CreateGeneratedDocumentRequest request) {
        if (request.getType() == GeneratedDocumentType.COVER_LETTER) {
            CoverLetterEntity entity = new CoverLetterEntity();
            entity.setUserId(userId);
            entity.setApplicationId(request.getApplicationId());
            entity.setContent(request.getContent());
            return toDto(coverLetterRepository.saveAndFlush(entity));
        }
        ResumeEntity entity = new ResumeEntity();
        entity.setUserId(userId);
        entity.setApplicationId(request.getApplicationId());
        entity.setContent(request.getContent());
        return toDto(resumeRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public void deleteDocument(UUID userId, UUID id) {
        // The id is unique across both tables (UUIDs), so try each in turn.
        coverLetterRepository.findByIdAndUserId(id, userId).ifPresentOrElse(
            coverLetterRepository::delete,
            () -> resumeRepository.findByIdAndUserId(id, userId).ifPresentOrElse(
                resumeRepository::delete,
                () -> { throw new ResourceNotFoundException("Document"); }));
    }

    // ------------------------------------------------------------------
    // Mapping helpers
    // ------------------------------------------------------------------

    private void requireProfile(UUID userId) {
        if (!profileRepository.existsByUserId(userId)) {
            throw new ProfileNotFoundException();
        }
    }

    private void apply(WorkExperienceEntity entity, WorkExperienceRequest request) {
        entity.setCompany(request.getCompany());
        entity.setRole(request.getRole());
        entity.setLocation(request.getLocation());
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setCurrent(Boolean.TRUE.equals(request.getIsCurrent()));
        entity.setDescription(request.getDescription());
    }

    private void apply(EducationEntity entity, EducationRequest request) {
        entity.setInstitution(request.getInstitution());
        entity.setDegree(request.getDegree());
        entity.setField(request.getField());
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setDescription(request.getDescription());
    }

    private Profile toDto(ProfileEntity e) {
        return new Profile()
            .id(e.getId())
            .firstName(e.getFirstName())
            .lastName(e.getLastName())
            .bio(e.getBio())
            .location(e.getLocation())
            .phone(e.getPhone())
            .website(e.getWebsite())
            .createdAt(e.getCreatedAt())
            .updatedAt(e.getUpdatedAt());
    }

    private WorkExperience toDto(WorkExperienceEntity e) {
        return new WorkExperience()
            .id(e.getId())
            .company(e.getCompany())
            .role(e.getRole())
            .location(e.getLocation())
            .startDate(e.getStartDate())
            .endDate(e.getEndDate())
            .isCurrent(e.isCurrent())
            .description(e.getDescription())
            .createdAt(e.getCreatedAt());
    }

    private Education toDto(EducationEntity e) {
        return new Education()
            .id(e.getId())
            .institution(e.getInstitution())
            .degree(e.getDegree())
            .field(e.getField())
            .startDate(e.getStartDate())
            .endDate(e.getEndDate())
            .description(e.getDescription())
            .createdAt(e.getCreatedAt());
    }

    private Skill toDto(SkillEntity e) {
        return new Skill()
            .id(e.getId())
            .name(e.getName())
            .level(e.getLevel())
            .createdAt(e.getCreatedAt());
    }

    private Language toDto(LanguageEntity e) {
        return new Language()
            .id(e.getId())
            .name(e.getName())
            .proficiency(e.getProficiency())
            .createdAt(e.getCreatedAt());
    }

    private GeneratedDocument toDto(CoverLetterEntity e) {
        return new GeneratedDocument()
            .id(e.getId())
            .applicationId(e.getApplicationId())
            .type(GeneratedDocumentType.COVER_LETTER)
            .content(e.getContent())
            .createdAt(e.getCreatedAt());
    }

    private GeneratedDocument toDto(ResumeEntity e) {
        return new GeneratedDocument()
            .id(e.getId())
            .applicationId(e.getApplicationId())
            .type(GeneratedDocumentType.RESUME)
            .content(e.getContent())
            .createdAt(e.getCreatedAt());
    }
}
