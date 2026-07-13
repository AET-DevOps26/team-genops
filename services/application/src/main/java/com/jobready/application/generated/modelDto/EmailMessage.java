package com.jobready.application.generated.modelDto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * EmailMessage
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class EmailMessage {

  private String messageId;

  private @Nullable String subject = null;

  private @Nullable String sender = null;

  private @Nullable String snippet = null;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime receivedAt = null;

  public EmailMessage() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public EmailMessage(String messageId) {
    this.messageId = messageId;
  }

  public EmailMessage messageId(String messageId) {
    this.messageId = messageId;
    return this;
  }

  /**
   * Provider-side unique message identifier
   * @return messageId
   */
  @NotNull 
  @Schema(name = "message_id", description = "Provider-side unique message identifier", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("message_id")
  public String getMessageId() {
    return messageId;
  }

  @JsonProperty("message_id")
  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  public EmailMessage subject(@Nullable String subject) {
    this.subject = subject;
    return this;
  }

  /**
   * Get subject
   * @return subject
   */
  
  @Schema(name = "subject", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("subject")
  public @Nullable String getSubject() {
    return subject;
  }

  @JsonProperty("subject")
  public void setSubject(@Nullable String subject) {
    this.subject = subject;
  }

  public EmailMessage sender(@Nullable String sender) {
    this.sender = sender;
    return this;
  }

  /**
   * Get sender
   * @return sender
   */
  
  @Schema(name = "sender", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("sender")
  public @Nullable String getSender() {
    return sender;
  }

  @JsonProperty("sender")
  public void setSender(@Nullable String sender) {
    this.sender = sender;
  }

  public EmailMessage snippet(@Nullable String snippet) {
    this.snippet = snippet;
    return this;
  }

  /**
   * Get snippet
   * @return snippet
   */
  
  @Schema(name = "snippet", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("snippet")
  public @Nullable String getSnippet() {
    return snippet;
  }

  @JsonProperty("snippet")
  public void setSnippet(@Nullable String snippet) {
    this.snippet = snippet;
  }

  public EmailMessage receivedAt(@Nullable OffsetDateTime receivedAt) {
    this.receivedAt = receivedAt;
    return this;
  }

  /**
   * Get receivedAt
   * @return receivedAt
   */
  @Valid 
  @Schema(name = "received_at", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("received_at")
  public @Nullable OffsetDateTime getReceivedAt() {
    return receivedAt;
  }

  @JsonProperty("received_at")
  public void setReceivedAt(@Nullable OffsetDateTime receivedAt) {
    this.receivedAt = receivedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EmailMessage emailMessage = (EmailMessage) o;
    return Objects.equals(this.messageId, emailMessage.messageId) &&
        Objects.equals(this.subject, emailMessage.subject) &&
        Objects.equals(this.sender, emailMessage.sender) &&
        Objects.equals(this.snippet, emailMessage.snippet) &&
        Objects.equals(this.receivedAt, emailMessage.receivedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(messageId, subject, sender, snippet, receivedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EmailMessage {\n");
    sb.append("    messageId: ").append(toIndentedString(messageId)).append("\n");
    sb.append("    subject: ").append(toIndentedString(subject)).append("\n");
    sb.append("    sender: ").append(toIndentedString(sender)).append("\n");
    sb.append("    snippet: ").append(toIndentedString(snippet)).append("\n");
    sb.append("    receivedAt: ").append(toIndentedString(receivedAt)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    return o == null ? "null" : o.toString().replace("\n", "\n    ");
  }
}

