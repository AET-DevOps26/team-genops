package com.jobready.email.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jobready.email.config.EmailProperties;
import com.jobready.email.modelEntity.ProcessedEmailEntity;
import com.jobready.email.modelEntity.ProcessedEmailEntity.AnalysisStatus;
import com.jobready.email.repository.ProcessedEmailRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

@ExtendWith(MockitoExtension.class)
class EmailAnalysisServiceTest {

    @Mock
    private ProcessedEmailRepository repository;

    @Mock
    private ApplicationClient applicationClient;

    @Mock
    private GenaiClient genaiClient;

    private final UUID userId = UUID.randomUUID();
    private final UUID applicationId = UUID.randomUUID();

    private EmailAnalysisService service(String token) {
        EmailProperties properties = new EmailProperties(
                new EmailProperties.Google("id", "secret", "http://cb", null, null, null, null),
                new EmailProperties.State("key", 600),
                "http://localhost:5173",
                "enc-key",
                25,
                "http://genai:8000",
                "http://application:8080",
                token,
                new EmailProperties.Analysis(10, 3, 0.6, 0.8));
        return new EmailAnalysisService(repository, applicationClient, genaiClient, properties);
    }

    private ProcessedEmailEntity email(String subject) {
        ProcessedEmailEntity e = new ProcessedEmailEntity();
        e.setUserId(userId);
        e.setMessageId("gmail-msg-1");
        e.setSubject(subject);
        e.setSender("someone@gmail.com");
        e.setBody("We would like to invite you to an interview at Acme on Friday.");
        e.setReceivedAt(Instant.parse("2026-07-14T10:00:00Z"));
        return e;
    }

    private void pendingReturns(ProcessedEmailEntity... emails) {
        when(repository.findByAnalysisStatusOrderByProcessedAtAsc(eq(AnalysisStatus.PENDING), any(Limit.class)))
                .thenReturn(List.of(emails));
    }

    private GenaiClient.EmailAnalysisResult verdict(
            String appId, String company, double confidence, boolean interviewInvite) {
        return new GenaiClient.EmailAnalysisResult(
                true,
                appId,
                company,
                "Engineer",
                interviewInvite,
                confidence,
                interviewInvite ? "interview" : null,
                new GenaiClient.TimelineEvent("interview_scheduled", "Interview invitation", "Invited"),
                List.of(new GenaiClient.ActionItem("Interview on Friday", "Prepare")));
    }

    @Test
    void disabledWithoutToken_neverTouchesAnything() {
        assertThat(service("").analyzePending()).isZero();
        verifyNoInteractions(repository, applicationClient, genaiClient);
    }

    @Test
    void confidentMatch_appliesUpdateAndMarksAnalyzed() {
        ProcessedEmailEntity email = email("Interview invitation - Acme");
        pendingReturns(email);
        when(applicationClient.listApplications(userId)).thenReturn(List.of());
        when(genaiClient.analyzeEmail(eq(userId), any(), anyList()))
                .thenReturn(verdict(applicationId.toString(), "Acme", 0.9, true));

        assertThat(service("token").analyzePending()).isEqualTo(1);

        verify(applicationClient).applyEmailUpdate(eq(applicationId), any());
        assertThat(email.getAnalysisStatus()).isEqualTo(AnalysisStatus.ANALYZED);
        assertThat(email.getMatchedApplicationId()).isEqualTo(applicationId);
        verify(repository).save(email);
    }

    @Test
    void belowUpdateThreshold_marksIrrelevantWithoutApplying() {
        ProcessedEmailEntity email = email("Interview invitation - Acme");
        pendingReturns(email);
        when(applicationClient.listApplications(userId)).thenReturn(List.of());
        when(genaiClient.analyzeEmail(eq(userId), any(), anyList()))
                .thenReturn(verdict(applicationId.toString(), "Acme", 0.5, true));

        service("token").analyzePending();

        verify(applicationClient, never()).applyEmailUpdate(any(), any());
        verify(applicationClient, never()).createFromEmail(any(), any());
        assertThat(email.getAnalysisStatus()).isEqualTo(AnalysisStatus.IRRELEVANT);
    }

    @Test
    void unmatchedHighConfidence_autoCreatesWithInterviewStage() {
        ProcessedEmailEntity email = email("Interview invitation - Globex");
        pendingReturns(email);
        when(applicationClient.listApplications(userId)).thenReturn(List.of());
        when(genaiClient.analyzeEmail(eq(userId), any(), anyList())).thenReturn(verdict(null, "Globex", 0.9, true));
        when(applicationClient.createFromEmail(eq(userId), any()))
                .thenReturn(new ApplicationClient.EmailCreateResponse(true, applicationId));

        service("token").analyzePending();

        var captor = org.mockito.ArgumentCaptor.forClass(ApplicationClient.EmailCreateRequest.class);
        verify(applicationClient).createFromEmail(eq(userId), captor.capture());
        assertThat(captor.getValue().company()).isEqualTo("Globex");
        assertThat(captor.getValue().suggestedStage()).isEqualTo("interview");
        assertThat(email.getAnalysisStatus()).isEqualTo(AnalysisStatus.ANALYZED);
        assertThat(email.getMatchedApplicationId()).isEqualTo(applicationId);
    }

    @Test
    void unmatchedNonInviteHighConfidence_autoCreatesAtApplied() {
        ProcessedEmailEntity email = email("Your application at Globex");
        pendingReturns(email);
        when(applicationClient.listApplications(userId)).thenReturn(List.of());
        when(genaiClient.analyzeEmail(eq(userId), any(), anyList())).thenReturn(verdict(null, "Globex", 0.85, false));
        when(applicationClient.createFromEmail(eq(userId), any()))
                .thenReturn(new ApplicationClient.EmailCreateResponse(true, applicationId));

        service("token").analyzePending();

        var captor = org.mockito.ArgumentCaptor.forClass(ApplicationClient.EmailCreateRequest.class);
        verify(applicationClient).createFromEmail(eq(userId), captor.capture());
        assertThat(captor.getValue().suggestedStage()).isEqualTo("applied");
    }

    @Test
    void unmatchedBelowCreateThreshold_doesNotCreate() {
        ProcessedEmailEntity email = email("Your application at Globex");
        pendingReturns(email);
        when(applicationClient.listApplications(userId)).thenReturn(List.of());
        when(genaiClient.analyzeEmail(eq(userId), any(), anyList())).thenReturn(verdict(null, "Globex", 0.7, false));

        service("token").analyzePending();

        verify(applicationClient, never()).createFromEmail(any(), any());
        assertThat(email.getAnalysisStatus()).isEqualTo(AnalysisStatus.IRRELEVANT);
    }

    @Test
    void bulkMailSubject_skippedWithoutLlmCall() {
        ProcessedEmailEntity email = email("Weekly newsletter: 20% off everything");
        pendingReturns(email);

        service("token").analyzePending();

        verifyNoInteractions(genaiClient);
        verifyNoInteractions(applicationClient);
        assertThat(email.getAnalysisStatus()).isEqualTo(AnalysisStatus.IRRELEVANT);
    }

    @Test
    void relevantHintBeatsSkipHint_stillCallsLlm() {
        // "unsubscribe" alone would be skipped, but "interview" marks it clearly worth analyzing.
        ProcessedEmailEntity email = email("Interview invitation (unsubscribe below)");
        pendingReturns(email);
        when(applicationClient.listApplications(userId)).thenReturn(List.of());
        when(genaiClient.analyzeEmail(eq(userId), any(), anyList()))
                .thenReturn(verdict(applicationId.toString(), "Acme", 0.9, true));

        service("token").analyzePending();

        verify(genaiClient).analyzeEmail(eq(userId), any(), anyList());
        assertThat(email.getAnalysisStatus()).isEqualTo(AnalysisStatus.ANALYZED);
    }

    @Test
    void neutralSubject_stillGoesToLlm() {
        ProcessedEmailEntity email = email("Quick question");
        pendingReturns(email);
        when(applicationClient.listApplications(userId)).thenReturn(List.of());
        when(genaiClient.analyzeEmail(eq(userId), any(), anyList()))
                .thenReturn(new GenaiClient.EmailAnalysisResult(
                        false, null, null, null, false, 0.9, null, null, List.of()));

        service("token").analyzePending();

        verify(genaiClient).analyzeEmail(eq(userId), any(), anyList());
        assertThat(email.getAnalysisStatus()).isEqualTo(AnalysisStatus.IRRELEVANT);
    }

    @Test
    void candidatesAreFetchedOncePerUserWithinABatch() {
        ProcessedEmailEntity first = email("Interview invitation - Acme");
        ProcessedEmailEntity second = email("Offer from Acme");
        second.setMessageId("gmail-msg-2");
        pendingReturns(first, second);
        when(applicationClient.listApplications(userId)).thenReturn(List.of());
        when(genaiClient.analyzeEmail(eq(userId), any(), anyList()))
                .thenReturn(verdict(applicationId.toString(), "Acme", 0.9, false));

        assertThat(service("token").analyzePending()).isEqualTo(2);

        org.mockito.Mockito.verify(applicationClient, org.mockito.Mockito.times(1))
                .listApplications(userId);
    }

    @Test
    void transientFailure_countsAttemptAndStaysPending() {
        ProcessedEmailEntity email = email("Interview invitation - Acme");
        pendingReturns(email);
        when(applicationClient.listApplications(userId)).thenThrow(new IllegalStateException("down"));

        assertThat(service("token").analyzePending()).isZero();

        assertThat(email.getAnalysisAttempts()).isEqualTo(1);
        assertThat(email.getAnalysisStatus()).isEqualTo(AnalysisStatus.PENDING);
        verify(repository).save(email);
    }

    @Test
    void maxAttemptsReached_marksFailed() {
        ProcessedEmailEntity email = email("Interview invitation - Acme");
        email.setAnalysisAttempts(2);
        pendingReturns(email);
        when(applicationClient.listApplications(userId)).thenThrow(new IllegalStateException("down"));

        service("token").analyzePending();

        assertThat(email.getAnalysisAttempts()).isEqualTo(3);
        assertThat(email.getAnalysisStatus()).isEqualTo(AnalysisStatus.FAILED);
    }

    @Test
    void oneFailingEmail_doesNotBlockTheRest() {
        ProcessedEmailEntity broken = email("Interview invitation - Acme");
        ProcessedEmailEntity fine = email("Offer from Globex");
        fine.setMessageId("gmail-msg-2");
        pendingReturns(broken, fine);
        when(applicationClient.listApplications(userId))
                .thenThrow(new IllegalStateException("down"))
                .thenReturn(List.of());
        when(genaiClient.analyzeEmail(eq(userId), any(), anyList()))
                .thenReturn(verdict(applicationId.toString(), "Globex", 0.9, false));

        assertThat(service("token").analyzePending()).isEqualTo(1);
        assertThat(fine.getAnalysisStatus()).isEqualTo(AnalysisStatus.ANALYZED);
        assertThat(broken.getAnalysisStatus()).isEqualTo(AnalysisStatus.PENDING);
    }
}
