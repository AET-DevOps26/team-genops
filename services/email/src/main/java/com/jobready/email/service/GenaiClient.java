package com.jobready.email.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jobready.email.config.EmailProperties;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client for genai's internal email-analysis endpoint (LLM classification of one email against
 * the user's applications). Authenticates with the shared {@code INTERNAL_SERVICE_TOKEN}. The
 * wire format is snake_case (FastAPI/pydantic).
 */
@Component
public class GenaiClient {

    /** LLM calls are slow; well beyond any normal HTTP timeout. */
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

    private final EmailProperties properties;
    private final RestClient restClient;

    public GenaiClient(EmailProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = builder.baseUrl(properties.genaiUrl())
                .requestFactory(requestFactory)
                .build();
    }

    public EmailAnalysisResult analyzeEmail(UUID userId, EmailPayload email, List<ApplicationCandidate> candidates) {
        EmailAnalysisResult result = restClient
                .post()
                .uri("/internal/v1/email-analysis")
                .headers(h -> h.setBearerAuth(properties.internalServiceToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EmailAnalysisRequest(userId.toString(), email, candidates))
                .retrieve()
                .body(EmailAnalysisResult.class);
        if (result == null) {
            throw new IllegalStateException("genai returned no analysis result");
        }
        return result;
    }

    public record EmailPayload(
            @JsonProperty("message_id") String messageId,
            @JsonProperty("subject") String subject,
            @JsonProperty("sender") String sender,
            @JsonProperty("body") String body,
            @JsonProperty("received_at") String receivedAt) {}

    public record ApplicationCandidate(
            @JsonProperty("id") String id,
            @JsonProperty("company") String company,
            @JsonProperty("job_title") String jobTitle,
            @JsonProperty("stage") String stage) {}

    record EmailAnalysisRequest(
            @JsonProperty("user_id") String userId,
            @JsonProperty("email") EmailPayload email,
            @JsonProperty("applications") List<ApplicationCandidate> applications) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TimelineEvent(
            @JsonProperty("event_type") String eventType,
            @JsonProperty("title") String title,
            @JsonProperty("description") String description) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ActionItem(
            @JsonProperty("insight") String insight, @JsonProperty("recommended_action") String recommendedAction) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmailAnalysisResult(
            @JsonProperty("relevant") boolean relevant,
            @JsonProperty("application_id") String applicationId,
            @JsonProperty("company") String company,
            @JsonProperty("position") String position,
            @JsonProperty("is_interview_invite") boolean isInterviewInvite,
            @JsonProperty("confidence") double confidence,
            @JsonProperty("suggested_stage") String suggestedStage,
            @JsonProperty("event") TimelineEvent event,
            @JsonProperty("action_items") List<ActionItem> actionItems) {}
}
