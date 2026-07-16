package com.jobready.auth.generated.modelDto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * CreateSessionRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class CreateSessionRequest {

  /**
   * Type of chat session
   */
  public enum SessionTypeEnum {
    INSIGHT_CHAT("insight_chat"),
    
    COVER_LETTER_CHAT("cover_letter_chat"),
    
    FIT_ANALYSIS_CHAT("fit_analysis_chat");

    private final String value;

    SessionTypeEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static SessionTypeEnum fromValue(String value) {
      for (SessionTypeEnum b : SessionTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private SessionTypeEnum sessionType = SessionTypeEnum.INSIGHT_CHAT;

  public CreateSessionRequest sessionType(SessionTypeEnum sessionType) {
    this.sessionType = sessionType;
    return this;
  }

  /**
   * Type of chat session
   * @return sessionType
   */
  
  @Schema(name = "session_type", description = "Type of chat session", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("session_type")
  public SessionTypeEnum getSessionType() {
    return sessionType;
  }

  @JsonProperty("session_type")
  public void setSessionType(SessionTypeEnum sessionType) {
    this.sessionType = sessionType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateSessionRequest createSessionRequest = (CreateSessionRequest) o;
    return Objects.equals(this.sessionType, createSessionRequest.sessionType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sessionType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateSessionRequest {\n");
    sb.append("    sessionType: ").append(toIndentedString(sessionType)).append("\n");
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

