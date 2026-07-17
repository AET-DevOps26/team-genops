package com.jobready.email.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jobready.email.config.EmailProperties;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Read-only Gmail REST API access: list recent message ids and fetch message metadata. */
@Component
public class GmailClient {

    /** Normalised message metadata as stored by the poller. */
    public record MessageMetadata(
            String messageId, String subject, String sender, String snippet, Instant receivedAt) {}

    private final EmailProperties properties;
    private final RestClient restClient;

    public GmailClient(EmailProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.baseUrl(properties.google().gmailBaseUrl()).build();
    }

    public List<String> listRecentMessageIds(String accessToken) {
        MessageListResponse response = restClient
                .get()
                .uri(uri -> uri.path("/gmail/v1/users/me/messages")
                        .queryParam("maxResults", properties.gmailMaxResults())
                        .build())
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .body(MessageListResponse.class);
        if (response == null || response.messages() == null) {
            return List.of();
        }
        return response.messages().stream().map(MessageRef::id).toList();
    }

    /** Fetch a single message's metadata (Subject/From headers, snippet, internal date). */
    public MessageMetadata fetchMessage(String accessToken, String messageId) {
        MessageResponse msg = restClient
                .get()
                .uri(uri -> uri.path("/gmail/v1/users/me/messages/{id}")
                        .queryParam("format", "metadata")
                        .queryParam("metadataHeaders", "Subject", "From")
                        .build(messageId))
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .body(MessageResponse.class);
        if (msg == null) {
            throw new IllegalStateException("Gmail returned no message for id " + messageId);
        }
        String subject = null;
        String sender = null;
        if (msg.payload() != null && msg.payload().headers() != null) {
            for (Header header : msg.payload().headers()) {
                if ("subject".equalsIgnoreCase(header.name())) {
                    subject = header.value();
                } else if ("from".equalsIgnoreCase(header.name())) {
                    sender = header.value();
                }
            }
        }
        Instant receivedAt =
                msg.internalDate() == null ? null : Instant.ofEpochMilli(Long.parseLong(msg.internalDate()));
        return new MessageMetadata(msg.id(), subject, sender, msg.snippet(), receivedAt);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MessageListResponse(@JsonProperty("messages") List<MessageRef> messages) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MessageRef(@JsonProperty("id") String id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MessageResponse(
            @JsonProperty("id") String id,
            @JsonProperty("snippet") String snippet,
            @JsonProperty("internalDate") String internalDate,
            @JsonProperty("payload") Payload payload) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Payload(@JsonProperty("headers") List<Header> headers) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Header(@JsonProperty("name") String name, @JsonProperty("value") String value) {}
}
