package com.jobready.email.service;

import com.jobready.email.config.EmailProperties;
import com.jobready.email.modelEntity.ProcessedEmailEntity;
import com.jobready.email.modelEntity.ProcessedEmailEntity.AnalysisStatus;
import com.jobready.email.repository.ProcessedEmailRepository;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

/**
 * Application-detection pipeline: match stored emails to job applications.
 *
 * <p>For each pending email: run a cheap keyword pre-filter on the subject, fetch the user's
 * applications (candidates), ask genai to classify/match the email, and apply the verdict via
 * the application service's internal API — an update on the matched application, or an
 * auto-create when the email confidently references an untracked company.
 *
 * <p>Failure semantics: transient errors (genai or application service down) leave the email
 * {@code pending} and count an attempt; it is retried on later poll cycles until
 * {@code email.analysis.max-attempts}, then marked {@code failed}. Replays after partial failure
 * are safe — the application service dedupes on the Gmail message id. The whole pipeline is
 * disabled (a no-op) while {@code INTERNAL_SERVICE_TOKEN} is blank.
 */
@Service
public class EmailAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(EmailAnalysisService.class);

    /**
     * Subject keywords that make an email clearly worth classifying — these always go to the
     * LLM, no matter what else the subject contains (demo emails come from personal addresses,
     * so the sender never matters here).
     */
    static final List<String> RELEVANT_HINTS = List.of(
            "interview",
            "offer",
            "rejection",
            "application",
            "applied",
            "position",
            "recruiter",
            "recruiting",
            "hiring",
            "candidate",
            "candidacy",
            "assessment",
            "screening",
            "talent");

    /**
     * Subject keywords of obvious bulk mail, skipped without an LLM call — but only when no
     * relevant hint is present. Everything unmatched by either list still goes to the LLM: the
     * pre-filter is a cost optimization, never the relevance decision.
     */
    static final List<String> SKIP_HINTS = List.of(
            "newsletter",
            "unsubscribe",
            "digest",
            "job alert",
            "jobs for you",
            "% off",
            "sale",
            "receipt",
            "order confirmation",
            "delivery",
            "invoice",
            "webinar");

    private final ProcessedEmailRepository repository;
    private final ApplicationClient applicationClient;
    private final GenaiClient genaiClient;
    private final EmailProperties properties;

    public EmailAnalysisService(
            ProcessedEmailRepository repository,
            ApplicationClient applicationClient,
            GenaiClient genaiClient,
            EmailProperties properties) {
        this.repository = repository;
        this.applicationClient = applicationClient;
        this.genaiClient = genaiClient;
        this.properties = properties;
    }

    /**
     * Analyze up to a batch of pending emails. Returns how many reached a final state. Never
     * throws: per-email failures are recorded and retried on later cycles, so one bad email (or
     * a genai outage) cannot break the polling loop.
     */
    public int analyzePending() {
        if (properties.internalServiceToken().isBlank()) {
            log.debug("Application detection disabled — INTERNAL_SERVICE_TOKEN not set");
            return 0;
        }
        List<ProcessedEmailEntity> pending = repository.findByAnalysisStatusOrderByProcessedAtAsc(
                AnalysisStatus.PENDING, Limit.of(properties.analysis().batchSize()));
        int finalized = 0;
        // Candidates are read-only for the duration of one pass; fetch once per user.
        Map<UUID, List<ApplicationClient.ApplicationCandidate>> candidatesByUser = new HashMap<>();
        for (ProcessedEmailEntity email : pending) {
            try {
                if (analyzeOne(email, candidatesByUser)) {
                    finalized++;
                }
            } catch (Exception e) {
                log.error("Analysis failed for message {} (user {})", email.getMessageId(), email.getUserId(), e);
                recordFailure(email);
            }
        }
        return finalized;
    }

    /** Run one email through the pipeline. Returns true when a final state was reached. */
    private boolean analyzeOne(
            ProcessedEmailEntity email, Map<UUID, List<ApplicationClient.ApplicationCandidate>> candidatesByUser) {
        String subject = email.getSubject() == null ? "" : email.getSubject().toLowerCase(Locale.ROOT);
        boolean hinted = RELEVANT_HINTS.stream().anyMatch(subject::contains);
        if (!hinted && SKIP_HINTS.stream().anyMatch(subject::contains)) {
            markFinal(email, AnalysisStatus.IRRELEVANT, null);
            return true;
        }

        List<ApplicationClient.ApplicationCandidate> candidates =
                candidatesByUser.computeIfAbsent(email.getUserId(), applicationClient::listApplications);
        GenaiClient.EmailAnalysisResult verdict = genaiClient.analyzeEmail(
                email.getUserId(),
                new GenaiClient.EmailPayload(
                        email.getMessageId(),
                        email.getSubject(),
                        email.getSender(),
                        email.getBody() != null ? email.getBody() : email.getSnippet(),
                        email.getReceivedAt() == null
                                ? null
                                : email.getReceivedAt().atOffset(ZoneOffset.UTC).toString()),
                candidates.stream()
                        .map(c -> new GenaiClient.ApplicationCandidate(
                                c.id().toString(), c.company(), c.jobTitle(), c.stage()))
                        .toList());

        UUID matched = applyVerdict(email, verdict);
        markFinal(email, matched == null ? AnalysisStatus.IRRELEVANT : AnalysisStatus.ANALYZED, matched);
        return true;
    }

    /** Applies a confident verdict; returns the affected application id, or null for a no-op. */
    private UUID applyVerdict(ProcessedEmailEntity email, GenaiClient.EmailAnalysisResult verdict) {
        if (!verdict.relevant() || verdict.event() == null) {
            return null;
        }
        String occurredAt = email.getReceivedAt() == null
                ? null
                : email.getReceivedAt().atOffset(ZoneOffset.UTC).toString();
        ApplicationClient.EmailEvent event = new ApplicationClient.EmailEvent(
                verdict.event().eventType(),
                verdict.event().title(),
                verdict.event().description(),
                occurredAt);
        List<ApplicationClient.EmailRecommendation> recommendations = verdict.actionItems() == null
                ? List.of()
                : verdict.actionItems().stream()
                        .map(a -> new ApplicationClient.EmailRecommendation(a.insight(), a.recommendedAction()))
                        .toList();

        if (verdict.applicationId() != null
                && verdict.confidence() >= properties.analysis().confidenceThreshold()) {
            UUID applicationId = UUID.fromString(verdict.applicationId());
            applicationClient.applyEmailUpdate(
                    applicationId,
                    new ApplicationClient.EmailUpdateRequest(
                            email.getUserId(), email.getMessageId(), verdict.suggestedStage(), event, recommendations));
            log.info(
                    "Email {} matched application {} (user {}, stage suggestion: {})",
                    email.getMessageId(),
                    applicationId,
                    email.getUserId(),
                    verdict.suggestedStage());
            return applicationId;
        }

        if (verdict.applicationId() == null
                && verdict.company() != null
                && !verdict.company().isBlank()
                && verdict.confidence() >= properties.analysis().createConfidenceThreshold()) {
            ApplicationClient.EmailCreateResponse response = applicationClient.createFromEmail(
                    email.getUserId(),
                    new ApplicationClient.EmailCreateRequest(
                            email.getUserId(),
                            email.getMessageId(),
                            verdict.company(),
                            verdict.position(),
                            verdict.isInterviewInvite() ? "interview" : "applied",
                            event,
                            recommendations));
            log.info(
                    "Email {} auto-created application {} for company '{}' (user {}, created={})",
                    email.getMessageId(),
                    response.applicationId(),
                    verdict.company(),
                    email.getUserId(),
                    response.created());
            return response.applicationId();
        }
        return null;
    }

    private void markFinal(ProcessedEmailEntity email, String status, UUID matchedApplicationId) {
        email.setAnalysisStatus(status);
        email.setMatchedApplicationId(matchedApplicationId);
        repository.save(email);
    }

    private void recordFailure(ProcessedEmailEntity email) {
        try {
            email.setAnalysisAttempts(email.getAnalysisAttempts() + 1);
            if (email.getAnalysisAttempts() >= properties.analysis().maxAttempts()) {
                email.setAnalysisStatus(AnalysisStatus.FAILED);
            }
            repository.save(email);
        } catch (Exception e) { // even bookkeeping must never break the polling loop
            log.error("Could not record analysis failure for message {}", email.getMessageId(), e);
        }
    }
}
