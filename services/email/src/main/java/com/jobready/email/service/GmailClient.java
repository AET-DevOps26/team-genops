package com.jobready.email.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jobready.email.config.EmailProperties;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Read-only Gmail REST API access: list recent message ids and fetch full messages. */
@Component
public class GmailClient {

    /** Detection only needs so much text; bodies are truncated defensively before storage. */
    static final int MAX_BODY_CHARS = 8000;

    /** Normalised message content as stored by the poller. */
    public record MessageMetadata(
            String messageId, String subject, String sender, String snippet, String body, Instant receivedAt) {}

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

    /** Fetch a single message in full (Subject/From headers, snippet, body, internal date). */
    public MessageMetadata fetchMessage(String accessToken, String messageId) {
        MessageResponse msg = restClient
                .get()
                .uri(uri -> uri.path("/gmail/v1/users/me/messages/{id}")
                        .queryParam("format", "full")
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
        return new MessageMetadata(msg.id(), subject, sender, msg.snippet(), extractBody(msg.payload()), receivedAt);
    }

    /**
     * Best-effort plain-text body: depth-first {@code text/plain} part, else {@code text/html}
     * with tags stripped. Returns null when the message has no decodable text (e.g.
     * attachment-only).
     */
    static String extractBody(Payload payload) {
        String plain = findPart(payload, "text/plain");
        String text = plain != null ? plain : stripHtml(findPart(payload, "text/html"));
        if (text == null || text.isBlank()) {
            return null;
        }
        text = text.strip();
        if (text.length() <= MAX_BODY_CHARS) {
            return text;
        }
        // Back off one char if the cut would split a surrogate pair — an unpaired
        // surrogate is invalid UTF-8 and Postgres would reject the whole insert.
        int end = Character.isHighSurrogate(text.charAt(MAX_BODY_CHARS - 1)) ? MAX_BODY_CHARS - 1 : MAX_BODY_CHARS;
        return text.substring(0, end);
    }

    private static String findPart(Payload part, String mimeType) {
        if (part == null) {
            return null;
        }
        if (part.mimeType() != null && part.mimeType().equalsIgnoreCase(mimeType)) {
            String decoded = decode(part.body());
            if (decoded != null) {
                return decoded;
            }
        }
        if (part.parts() != null) {
            for (Payload child : part.parts()) {
                String found = findPart(child, mimeType);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static String decode(Body body) {
        if (body == null || body.data() == null || body.data().isEmpty()) {
            return null;
        }
        try {
            // Gmail bodies are URL-safe base64; Java's URL decoder tolerates missing padding.
            return new String(Base64.getUrlDecoder().decode(body.data()), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String stripHtml(String html) {
        if (html == null) {
            return null;
        }
        return html.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ")
                .replaceAll("(?i)<br\\s*/?>|</p>|</div>|</tr>", "\n")
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n{3,}", "\n\n");
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
    record Payload(
            @JsonProperty("mimeType") String mimeType,
            @JsonProperty("headers") List<Header> headers,
            @JsonProperty("body") Body body,
            @JsonProperty("parts") List<Payload> parts) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Body(@JsonProperty("data") String data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Header(@JsonProperty("name") String name, @JsonProperty("value") String value) {}
}
