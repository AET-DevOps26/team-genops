package com.jobready.email.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

class GmailBodyExtractionTest {

    private static String encode(String text) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private static GmailClient.Payload leaf(String mimeType, String text) {
        return new GmailClient.Payload(mimeType, null, new GmailClient.Body(encode(text)), null);
    }

    @Test
    void simplePlainTextBody() {
        GmailClient.Payload payload = leaf("text/plain", "Hello interview");
        assertThat(GmailClient.extractBody(payload)).isEqualTo("Hello interview");
    }

    @Test
    void multipartPrefersPlainTextOverHtml() {
        GmailClient.Payload payload = new GmailClient.Payload(
                "multipart/alternative",
                null,
                null,
                List.of(leaf("text/html", "<p>html version</p>"), leaf("text/plain", "plain version")));
        assertThat(GmailClient.extractBody(payload)).isEqualTo("plain version");
    }

    @Test
    void fallsBackToStrippedHtml() {
        GmailClient.Payload payload = new GmailClient.Payload(
                "multipart/alternative",
                null,
                null,
                List.of(leaf(
                        "text/html",
                        "<html><style>.x{color:red}</style><body><p>We would like to "
                                + "<b>invite</b> you.</p></body></html>")));
        assertThat(GmailClient.extractBody(payload)).isEqualTo("We would like to invite you.");
    }

    @Test
    void nestedMultipartIsSearchedDepthFirst() {
        GmailClient.Payload inner = new GmailClient.Payload(
                "multipart/alternative", null, null, List.of(leaf("text/plain", "nested text")));
        GmailClient.Payload payload = new GmailClient.Payload("multipart/mixed", null, null, List.of(inner));
        assertThat(GmailClient.extractBody(payload)).isEqualTo("nested text");
    }

    @Test
    void attachmentOnlyMessage_returnsNull() {
        GmailClient.Payload payload = new GmailClient.Payload(
                "multipart/mixed",
                null,
                null,
                List.of(new GmailClient.Payload("application/pdf", null, new GmailClient.Body(null), null)));
        assertThat(GmailClient.extractBody(payload)).isNull();
    }

    @Test
    void longBodyIsTruncated() {
        GmailClient.Payload payload = leaf("text/plain", "x".repeat(GmailClient.MAX_BODY_CHARS + 500));
        assertThat(GmailClient.extractBody(payload)).hasSize(GmailClient.MAX_BODY_CHARS);
    }

    @Test
    void urlSafeBase64WithoutPaddingDecodes() {
        // '~' and '?' force URL-safe alphabet characters in the encoding.
        String text = "subject?~ body with url-safe chars ÿ";
        GmailClient.Payload payload = leaf("text/plain", text);
        assertThat(GmailClient.extractBody(payload)).isEqualTo(text);
    }
}
