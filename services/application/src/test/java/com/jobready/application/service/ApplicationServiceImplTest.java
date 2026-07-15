package com.jobready.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobready.application.exception.ApplicationNotFoundException;
import com.jobready.application.generated.modelDto.ApplicationList;
import com.jobready.application.generated.modelDto.ApplicationStage;
import com.jobready.application.generated.modelDto.ApplicationSummary;
import com.jobready.application.generated.modelDto.CreateApplicationRequest;
import com.jobready.application.generated.modelDto.CreateRecommendationRequest;
import com.jobready.application.generated.modelDto.JobApplication;
import com.jobready.application.generated.modelDto.RecommendationList;
import com.jobready.application.generated.modelDto.UpdateApplicationRequest;
import com.jobready.application.modelEntity.Application;
import com.jobready.application.modelEntity.Recommendation;
import com.jobready.application.repository.ApplicationRepository;
import com.jobready.application.repository.RecommendationRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock
    private ApplicationRepository repository;

    @Mock
    private RecommendationRepository recommendationRepository;

    @InjectMocks
    private ApplicationServiceImpl service;

    private final UUID userId = UUID.randomUUID();

    @Test
    void create_setsOwnerFromArgument_andDefaultsToApplied() {
        when(repository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateApplicationRequest request =
                new CreateApplicationRequest("Acme", "Engineer").jobDescription("Build things");

        JobApplication result = service.create(userId, request);

        ArgumentCaptor<Application> saved = ArgumentCaptor.forClass(Application.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(userId);
        assertThat(saved.getValue().getStage()).isEqualTo(ApplicationStage.APPLIED);
        assertThat(result.getCompany()).isEqualTo("Acme");
    }

    @Test
    void create_roundTripsCompanyWebsiteAndLinkedinUrl() {
        when(repository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateApplicationRequest request = new CreateApplicationRequest("Acme", "Engineer")
                .companyWebsite("https://acme.example")
                .linkedinUrl("https://linkedin.com/company/acme");

        JobApplication result = service.create(userId, request);

        assertThat(result.getCompanyWebsite()).isEqualTo("https://acme.example");
        assertThat(result.getLinkedinUrl()).isEqualTo("https://linkedin.com/company/acme");
    }

    @Test
    void list_isScopedToTheUser_andReportsTotal() {
        Application a = entity(userId, ApplicationStage.APPLIED);
        when(repository.countByUserId(userId)).thenReturn(1L);
        when(repository.findByUserIdOrderByAppliedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(List.of(a));

        ApplicationList result = service.list(userId, null, null, null);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1L);
    }

    @Test
    void list_withStageFilter_usesFilteredQueryAndCount() {
        Application a = entity(userId, ApplicationStage.INTERVIEW);
        when(repository.countByUserIdAndStage(userId, ApplicationStage.INTERVIEW))
                .thenReturn(3L);
        when(repository.findByUserIdAndStageOrderByAppliedAtDesc(
                        eq(userId), eq(ApplicationStage.INTERVIEW), any(Pageable.class)))
                .thenReturn(List.of(a));

        ApplicationList result = service.list(userId, ApplicationStage.INTERVIEW, 1, 0);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(3L);
    }

    @Test
    void list_offsetPastEnd_returnsEmptyPageWithTotal() {
        when(repository.countByUserId(userId)).thenReturn(2L);

        ApplicationList result = service.list(userId, null, 10, 5);

        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotal()).isEqualTo(2L);
        verify(repository, never()).findByUserIdOrderByAppliedAtDesc(eq(userId), any(Pageable.class));
    }

    @Test
    void get_whenNotOwned_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(userId, id)).isInstanceOf(ApplicationNotFoundException.class);
    }

    @Test
    void update_changesStage() {
        UUID id = UUID.randomUUID();
        Application existing = entity(userId, ApplicationStage.APPLIED);
        when(repository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(existing));
        when(repository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateApplicationRequest request = new UpdateApplicationRequest("Acme", "Engineer", ApplicationStage.INTERVIEW);

        JobApplication result = service.update(userId, id, request);

        assertThat(result.getStage()).isEqualTo(ApplicationStage.INTERVIEW);
        assertThat(existing.getStage()).isEqualTo(ApplicationStage.INTERVIEW);
    }

    @Test
    void delete_alsoRemovesTheApplicationsRecommendations() {
        UUID id = UUID.randomUUID();
        Application existing = entity(userId, ApplicationStage.APPLIED);
        when(repository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(existing));

        service.delete(userId, id);

        verify(recommendationRepository).deleteByApplicationIdAndUserId(id, userId);
        verify(repository).delete(existing);
    }

    @Test
    void delete_whenNotOwned_throwsAndDoesNotDelete() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(userId, id)).isInstanceOf(ApplicationNotFoundException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void summary_fillsMissingStagesWithZero_andTotals() {
        when(repository.countByUserIdGroupedByStage(userId))
                .thenReturn(List.of(stageCount(ApplicationStage.APPLIED, 2L), stageCount(ApplicationStage.OFFER, 1L)));

        ApplicationSummary summary = service.summary(userId);

        assertThat(summary.getApplied()).isEqualTo(2L);
        assertThat(summary.getOffer()).isEqualTo(1L);
        assertThat(summary.getFollowUp()).isZero();
        assertThat(summary.getInterview()).isZero();
        assertThat(summary.getClosed()).isZero();
        assertThat(summary.getTotal()).isEqualTo(3L);
    }

    @Test
    void addRecommendation_requiresOwnedParent() {
        UUID applicationId = UUID.randomUUID();
        when(repository.findByIdAndUserId(applicationId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addRecommendation(
                        userId, applicationId, new CreateRecommendationRequest("silent 16d", "send follow-up")))
                .isInstanceOf(ApplicationNotFoundException.class);
        verify(recommendationRepository, never()).save(any());
    }

    @Test
    void addRecommendation_setsOwnerAndParentFromArguments() {
        UUID applicationId = UUID.randomUUID();
        when(repository.findByIdAndUserId(applicationId, userId))
                .thenReturn(Optional.of(entity(userId, ApplicationStage.APPLIED)));
        when(recommendationRepository.save(any(Recommendation.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.addRecommendation(
                userId, applicationId, new CreateRecommendationRequest("silent 16d", "send follow-up"));

        ArgumentCaptor<Recommendation> saved = ArgumentCaptor.forClass(Recommendation.class);
        verify(recommendationRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(userId);
        assertThat(saved.getValue().getApplicationId()).isEqualTo(applicationId);
        assertThat(result.getInsight()).isEqualTo("silent 16d");
        assertThat(result.getRecommendedAction()).isEqualTo("send follow-up");
    }

    @Test
    void listRecommendations_requiresOwnedParent() {
        UUID applicationId = UUID.randomUUID();
        when(repository.findByIdAndUserId(applicationId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listRecommendations(userId, applicationId))
                .isInstanceOf(ApplicationNotFoundException.class);
    }

    @Test
    void listRecommendations_returnsScopedItems() {
        UUID applicationId = UUID.randomUUID();
        when(repository.findByIdAndUserId(applicationId, userId))
                .thenReturn(Optional.of(entity(userId, ApplicationStage.APPLIED)));
        Recommendation r = new Recommendation();
        r.setId(UUID.randomUUID());
        r.setUserId(userId);
        r.setApplicationId(applicationId);
        r.setInsight("silent 16d");
        r.setRecommendedAction("send follow-up");
        when(recommendationRepository.findByApplicationIdAndUserIdOrderByCreatedAtDesc(applicationId, userId))
                .thenReturn(List.of(r));

        RecommendationList result = service.listRecommendations(userId, applicationId);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getApplicationId()).isEqualTo(applicationId);
    }

    @Test
    void deleteRecommendation_whenAttachedToDifferentApplication_throwsAndDoesNotDelete() {
        UUID applicationId = UUID.randomUUID();
        UUID recommendationId = UUID.randomUUID();
        when(repository.findByIdAndUserId(applicationId, userId))
                .thenReturn(Optional.of(entity(userId, ApplicationStage.APPLIED)));
        Recommendation other = new Recommendation();
        other.setId(recommendationId);
        other.setUserId(userId);
        other.setApplicationId(UUID.randomUUID()); // belongs to another application
        when(recommendationRepository.findByIdAndUserId(recommendationId, userId))
                .thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.deleteRecommendation(userId, applicationId, recommendationId))
                .isInstanceOf(ApplicationNotFoundException.class);
        verify(recommendationRepository, never()).delete(any());
    }

    private Application entity(UUID owner, ApplicationStage stage) {
        Application a = new Application();
        a.setId(UUID.randomUUID());
        a.setUserId(owner);
        a.setCompany("Acme");
        a.setJobTitle("Engineer");
        a.setStage(stage);
        return a;
    }

    private ApplicationRepository.StageCount stageCount(ApplicationStage stage, long count) {
        return new ApplicationRepository.StageCount() {
            @Override
            public ApplicationStage getStage() {
                return stage;
            }

            @Override
            public long getCount() {
                return count;
            }
        };
    }
}
