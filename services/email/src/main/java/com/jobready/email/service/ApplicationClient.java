package com.jobready.email.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jobready.email.config.EmailProperties;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client for the application service's internal API: candidate listing, idempotent email-update,
 * and email-create (auto-create an application for an untracked company). Authenticates with the
 * shared {@code INTERNAL_SERVICE_TOKEN}; the contract is documented in the application service's
 * README.
 */
@Component
public class ApplicationClient {

    private final EmailProperties properties;
    private final RestClient restClient;

    public ApplicationClient(EmailProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = builder.baseUrl(properties.applicationServiceUrl())
                .requestFactory(requestFactory)
                .build();
    }

    /** The user's applications, slimmed to what email matching needs. */
    public List<ApplicationCandidate> listApplications(UUID userId) {
        List<ApplicationCandidate> candidates = restClient
                .get()
                .uri("/internal/v1/users/{userId}/applications", userId)
                .headers(h -> h.setBearerAuth(properties.internalServiceToken()))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return candidates == null ? List.of() : candidates;
    }

    /** Applies one email's derived update atomically. Returns false on an idempotent replay. */
    public boolean applyEmailUpdate(UUID applicationId, EmailUpdateRequest request) {
        EmailUpdateResponse response = restClient
                .post()
                .uri("/internal/v1/applications/{id}/email-update", applicationId)
                .headers(h -> h.setBearerAuth(properties.internalServiceToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(EmailUpdateResponse.class);
        return response != null && response.applied();
    }

    /** Auto-creates an application for an untracked company. */
    public EmailCreateResponse createFromEmail(UUID userId, EmailCreateRequest request) {
        EmailCreateResponse response = restClient
                .post()
                .uri("/internal/v1/users/{userId}/applications/email-create", userId)
                .headers(h -> h.setBearerAuth(properties.internalServiceToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(EmailCreateResponse.class);
        if (response == null) {
            throw new IllegalStateException("application service returned no email-create response");
        }
        return response;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApplicationCandidate(
            @JsonProperty("id") UUID id,
            @JsonProperty("company") String company,
            @JsonProperty("jobTitle") String jobTitle,
            @JsonProperty("stage") String stage) {}

    public record EmailEvent(
            @JsonProperty("eventType") String eventType,
            @JsonProperty("title") String title,
            @JsonProperty("description") String description,
            @JsonProperty("occurredAt") String occurredAt) {}

    public record EmailRecommendation(
            @JsonProperty("insight") String insight, @JsonProperty("recommendedAction") String recommendedAction) {}

    public record EmailUpdateRequest(
            @JsonProperty("userId") UUID userId,
            @JsonProperty("sourceMessageId") String sourceMessageId,
            @JsonProperty("suggestedStage") String suggestedStage,
            @JsonProperty("event") EmailEvent event,
            @JsonProperty("recommendations") List<EmailRecommendation> recommendations) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EmailUpdateResponse(@JsonProperty("applied") boolean applied) {}

    public record EmailCreateRequest(
            @JsonProperty("userId") UUID userId,
            @JsonProperty("sourceMessageId") String sourceMessageId,
            @JsonProperty("company") String company,
            @JsonProperty("position") String position,
            @JsonProperty("suggestedStage") String suggestedStage,
            @JsonProperty("event") EmailEvent event,
            @JsonProperty("recommendations") List<EmailRecommendation> recommendations) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmailCreateResponse(
            @JsonProperty("created") boolean created, @JsonProperty("applicationId") UUID applicationId) {}
}
