package com.jobready.application.internal;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        when(eventRepository.existsByApplicationIdAndSourceMessageId(applicationId, "gmail-msg-1")).thenReturn(false);

        boolean applied = service.apply(applicationId, request("interview",
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
        when(eventRepository.existsByApplicationIdAndSourceMessageId(applicationId, "gmail-msg-1")).thenReturn(true);

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
        when(eventRepository.existsByApplicationIdAndSourceMessageId(applicationId, "gmail-msg-1")).thenReturn(false);

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
    void apply_whenApplicationNotOwnedByUser_throwsNotFound() {
        when(applicationRepository.findByIdAndUserId(applicationId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.apply(applicationId, request("interview", List.of())))
            .isInstanceOf(ApplicationNotFoundException.class);
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
