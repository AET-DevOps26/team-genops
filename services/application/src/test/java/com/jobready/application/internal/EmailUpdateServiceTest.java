package com.jobready.application.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobready.application.exception.ApplicationNotFoundException;
import com.jobready.application.generated.modelDto.ApplicationEventType;
import com.jobready.application.generated.modelDto.ApplicationStage;
import com.jobready.application.internal.InternalDtos.EmailEvent;
import com.jobready.application.internal.InternalDtos.EmailRecommendation;
import com.jobready.application.internal.InternalDtos.EmailUpdateRequest;
import com.jobready.application.modelEntity.Application;
import com.jobready.application.modelEntity.ApplicationEvent;
import com.jobready.application.modelEntity.Recommendation;
import com.jobready.application.repository.ApplicationEventRepository;
import com.jobready.application.repository.ApplicationRepository;
import com.jobready.application.repository.RecommendationRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailUpdateServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationEventRepository eventRepository;

    @Mock
    private RecommendationRepository recommendationRepository;

    @InjectMocks
    private EmailUpdateService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID applicationId = UUID.randomUUID();

    private Application application(ApplicationStage stage) {
        Application a = new Application();
        a.setUserId(userId);
        a.setCompany("Acme");
        a.setJobTitle("Engineer");
        a.setStage(stage);
        return a;
    }

    private EmailUpdateRequest request(String suggestedStage, List<EmailRecommendation> recs) {
        return new EmailUpdateRequest(
                userId,
                "gmail-msg-1",
                suggestedStage,
                new EmailEvent("interview_scheduled", "Interview invitation", "Acme invited you", OffsetDateTime.now()),
                recs);
    }

    @Test
    void apply_changesStage_savesEventAndRecommendations() {
        Application existing = application(ApplicationStage.APPLIED);
        when(applicationRepository.findByIdAndUserId(applicationId, userId)).thenReturn(Optional.of(existing));
        when(eventRepository.existsByApplicationIdAndSourceMessageId(applicationId, "gmail-msg-1"))
                .thenReturn(false);

        boolean applied = service.apply(
                applicationId,
                request(
                        "interview",
                        List.of(new EmailRecommendation("Interview on Friday", "Prepare for the interview"))));

        assertThat(applied).isTrue();
        assertThat(existing.getStage()).isEqualTo(ApplicationStage.INTERVIEW);

        ArgumentCaptor<ApplicationEvent> event = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(eventRepository).save(event.capture());
        assertThat(event.getValue().getEventType()).isEqualTo(ApplicationEventType.INTERVIEW_SCHEDULED);
        assertThat(event.getValue().getSource()).isEqualTo(ApplicationEvent.Source.EMAIL);
        assertThat(event.getValue().getSourceMessageId()).isEqualTo("gmail-msg-1");
        assertThat(event.getValue().getStageFrom()).isEqualTo(ApplicationStage.APPLIED);
        assertThat(event.getValue().getStageTo()).isEqualTo(ApplicationStage.INTERVIEW);

        verify(recommendationRepository).save(any(Recommendation.class));
    }

    @Test
    void apply_duplicateMessageId_isIdempotentNoOp() {
        when(applicationRepository.findByIdAndUserId(applicationId, userId))
                .thenReturn(Optional.of(application(ApplicationStage.APPLIED)));
        when(eventRepository.existsByApplicationIdAndSourceMessageId(applicationId, "gmail-msg-1"))
                .thenReturn(true);

        boolean applied = service.apply(applicationId, request("interview", List.of()));

        assertThat(applied).isFalse();
        verify(eventRepository, never()).save(any());
        verify(recommendationRepository, never()).save(any());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void apply_withoutStageSuggestion_keepsStage_andEventHasNoTransition() {
        Application existing = application(ApplicationStage.APPLIED);
        when(applicationRepository.findByIdAndUserId(applicationId, userId)).thenReturn(Optional.of(existing));
        when(eventRepository.existsByApplicationIdAndSourceMessageId(applicationId, "gmail-msg-1"))
                .thenReturn(false);

        boolean applied = service.apply(applicationId, request(null, null));

        assertThat(applied).isTrue();
        assertThat(existing.getStage()).isEqualTo(ApplicationStage.APPLIED);
        ArgumentCaptor<ApplicationEvent> event = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(eventRepository).save(event.capture());
        assertThat(event.getValue().getStageFrom()).isNull();
        assertThat(event.getValue().getStageTo()).isNull();
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void apply_backwardStageSuggestion_recordsEventWithoutTransition() {
        Application existing = application(ApplicationStage.OFFER);
        when(applicationRepository.findByIdAndUserId(applicationId, userId)).thenReturn(Optional.of(existing));

        boolean applied = service.apply(applicationId, request("interview", List.of()));

        assertThat(applied).isTrue();
        assertThat(existing.getStage()).isEqualTo(ApplicationStage.OFFER); // never moves backwards
        verify(applicationRepository, never()).save(any());
        ArgumentCaptor<ApplicationEvent> event = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(eventRepository).save(event.capture());
        assertThat(event.getValue().getStageFrom()).isNull();
        assertThat(event.getValue().getStageTo()).isNull();
    }

    @Test
    void apply_sameEmailPreviouslyAppliedToAnotherApplication_isIdempotentNoOp() {
        when(applicationRepository.findByIdAndUserId(applicationId, userId))
                .thenReturn(Optional.of(application(ApplicationStage.APPLIED)));
        when(eventRepository.existsByApplicationIdAndSourceMessageId(applicationId, "gmail-msg-1"))
                .thenReturn(false);
        when(eventRepository.existsByUserIdAndSourceMessageId(userId, "gmail-msg-1"))
                .thenReturn(true);

        boolean applied = service.apply(applicationId, request("interview", List.of()));

        assertThat(applied).isFalse();
        verify(eventRepository, never()).save(any());
    }

    @Test
    void apply_unknownStageWireValue_throwsInvalidWireValue() {
        when(applicationRepository.findByIdAndUserId(applicationId, userId))
                .thenReturn(Optional.of(application(ApplicationStage.APPLIED)));

        assertThatThrownBy(() -> service.apply(applicationId, request("hired", List.of())))
                .isInstanceOf(com.jobready.application.exception.InvalidWireValueException.class);
    }

    @Test
    void apply_whenApplicationNotOwnedByUser_throwsNotFound() {
        when(applicationRepository.findByIdAndUserId(applicationId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.apply(applicationId, request("interview", List.of())))
                .isInstanceOf(ApplicationNotFoundException.class);
    }

    private InternalDtos.EmailCreateRequest createRequest(String company, String position, String suggestedStage) {
        return new InternalDtos.EmailCreateRequest(
                userId,
                "gmail-msg-1",
                company,
                position,
                suggestedStage,
                new EmailEvent("interview_scheduled", "Interview invitation", "Invited you", OffsetDateTime.now()),
                List.of(new EmailRecommendation("Interview on Friday", "Prepare for the interview")));
    }

    @Test
    void createFromEmail_createsApplicationEventAndRecommendations() {
        when(eventRepository.existsByUserIdAndSourceMessageId(userId, "gmail-msg-1"))
                .thenReturn(false);
        when(applicationRepository.findByUserIdOrderByAppliedAtDesc(userId)).thenReturn(List.of());
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> {
            Application a = inv.getArgument(0);
            a.setId(applicationId);
            return a;
        });

        var response = service.createFromEmail(createRequest("Globex", "Engineer", "interview"));

        assertThat(response.created()).isTrue();
        assertThat(response.applicationId()).isEqualTo(applicationId);

        ArgumentCaptor<Application> saved = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(saved.capture());
        assertThat(saved.getValue().getCompany()).isEqualTo("Globex");
        assertThat(saved.getValue().getJobTitle()).isEqualTo("Engineer");
        assertThat(saved.getValue().getStage()).isEqualTo(ApplicationStage.INTERVIEW);

        ArgumentCaptor<ApplicationEvent> event = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(eventRepository).save(event.capture());
        assertThat(event.getValue().getStageFrom()).isNull();
        assertThat(event.getValue().getStageTo()).isEqualTo(ApplicationStage.INTERVIEW);
        assertThat(event.getValue().getSource()).isEqualTo(ApplicationEvent.Source.EMAIL);
        assertThat(event.getValue().getSourceMessageId()).isEqualTo("gmail-msg-1");

        verify(recommendationRepository).save(any(Recommendation.class));
    }

    @Test
    void createFromEmail_defaultsToAppliedStageAndUnknownPosition() {
        when(eventRepository.existsByUserIdAndSourceMessageId(userId, "gmail-msg-1"))
                .thenReturn(false);
        when(applicationRepository.findByUserIdOrderByAppliedAtDesc(userId)).thenReturn(List.of());
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createFromEmail(createRequest("Globex", null, null));

        ArgumentCaptor<Application> saved = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(saved.capture());
        assertThat(saved.getValue().getStage()).isEqualTo(ApplicationStage.APPLIED);
        assertThat(saved.getValue().getJobTitle()).isEqualTo("Unknown position");
    }

    @Test
    void createFromEmail_duplicateMessageId_isIdempotentNoOp() {
        when(eventRepository.existsByUserIdAndSourceMessageId(userId, "gmail-msg-1"))
                .thenReturn(true);

        var response = service.createFromEmail(createRequest("Globex", "Engineer", "applied"));

        assertThat(response.created()).isFalse();
        verify(applicationRepository, never()).save(any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createFromEmail_existingCompany_appliesUpdateInstead() {
        Application existing = application(ApplicationStage.APPLIED);
        existing.setId(applicationId);
        when(eventRepository.existsByUserIdAndSourceMessageId(userId, "gmail-msg-1"))
                .thenReturn(false);
        when(applicationRepository.findByUserIdOrderByAppliedAtDesc(userId)).thenReturn(List.of(existing));
        when(applicationRepository.findByIdAndUserId(applicationId, userId)).thenReturn(Optional.of(existing));
        when(eventRepository.existsByApplicationIdAndSourceMessageId(applicationId, "gmail-msg-1"))
                .thenReturn(false);

        var response = service.createFromEmail(createRequest("acme", "Engineer", "interview"));

        assertThat(response.created()).isFalse();
        assertThat(response.applicationId()).isEqualTo(applicationId);
        assertThat(existing.getStage()).isEqualTo(ApplicationStage.INTERVIEW);
        verify(eventRepository).save(any(ApplicationEvent.class));
    }

    @Test
    void listCandidates_mapsSlimView() {
        Application existing = application(ApplicationStage.APPLIED);
        when(applicationRepository.findByUserIdOrderByAppliedAtDesc(userId)).thenReturn(List.of(existing));

        var candidates = service.listCandidates(userId);

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).company()).isEqualTo("Acme");
        assertThat(candidates.get(0).stage()).isEqualTo("applied");
    }
}
