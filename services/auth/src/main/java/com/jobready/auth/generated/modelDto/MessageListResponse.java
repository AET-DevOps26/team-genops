package com.jobready.auth.generated.modelDto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.jobready.auth.generated.modelDto.MessageItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * MessageListResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class MessageListResponse {

  private List<@Valid MessageItem> messages = new ArrayList<>();

  public MessageListResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public MessageListResponse(List<@Valid MessageItem> messages) {
    this.messages = messages;
  }

  public MessageListResponse messages(List<@Valid MessageItem> messages) {
    this.messages = messages;
    return this;
  }

  public MessageListResponse addMessagesItem(MessageItem messagesItem) {
    if (this.messages == null) {
      this.messages = new ArrayList<>();
    }
    this.messages.add(messagesItem);
    return this;
  }

  /**
   * Get messages
   * @return messages
   */
  @NotNull @Valid 
  @Schema(name = "messages", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("messages")
  public List<@Valid MessageItem> getMessages() {
    return messages;
  }

  @JsonProperty("messages")
  public void setMessages(List<@Valid MessageItem> messages) {
    this.messages = messages;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MessageListResponse messageListResponse = (MessageListResponse) o;
    return Objects.equals(this.messages, messageListResponse.messages);
  }

  @Override
  public int hashCode() {
    return Objects.hash(messages);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MessageListResponse {\n");
    sb.append("    messages: ").append(toIndentedString(messages)).append("\n");
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

