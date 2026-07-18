package com.jobready.auth.generated.modelDto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * SessionResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class SessionResponse {

  private UUID id;

  private UUID userId;

  private @Nullable UUID applicationId = null;

  /**
   * Gets or Sets sessionType
   */
  public enum SessionTypeEnum {
    INSIGHT_CHAT("insight_chat"),
    
    COVER_LETTER_CHAT("cover_letter_chat"),
    
    FIT_ANALYSIS_CHAT("fit_analysis_chat"),
    
    MOCK_INTERVIEW("mock_interview");

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

  private SessionTypeEnum sessionType;

  private @Nullable String summary = null;

  private @Nullable String firstMessage = null;

  /**
   * Lifecycle of a mock interview; null for every other session type
   */
  public enum InterviewStatusEnum {
    IN_PROGRESS("in_progress"),
    
    COMPLETED("completed");

    private final String value;

    InterviewStatusEnum(String value) {
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
    public static InterviewStatusEnum fromValue(String value) {
      for (InterviewStatusEnum b : InterviewStatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      return null;
    }
  }

  private @Nullable InterviewStatusEnum interviewStatus = null;

  private @Nullable Integer interviewScore = null;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public SessionResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SessionResponse(UUID id, UUID userId, SessionTypeEnum sessionType, OffsetDateTime createdAt) {
    this.id = id;
    this.userId = userId;
    this.sessionType = sessionType;
    this.createdAt = createdAt;
  }

  public SessionResponse id(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @NotNull @Valid 
  @Schema(name = "id", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("id")
  public UUID getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(UUID id) {
    this.id = id;
  }

  public SessionResponse userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Get userId
   * @return userId
   */
  @NotNull @Valid 
  @Schema(name = "user_id", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("user_id")
  public UUID getUserId() {
    return userId;
  }

  @JsonProperty("user_id")
  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public SessionResponse applicationId(@Nullable UUID applicationId) {
    this.applicationId = applicationId;
    return this;
  }

  /**
   * The job application this conversation is about, bound the first time one is referenced. Present when the chat was started from an application or one was chosen for a document command; null for a general chat. The UI uses it to know where a generated cover letter or resume would be saved.
   * @return applicationId
   */
  @Valid 
  @Schema(name = "application_id", description = "The job application this conversation is about, bound the first time one is referenced. Present when the chat was started from an application or one was chosen for a document command; null for a general chat. The UI uses it to know where a generated cover letter or resume would be saved.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("application_id")
  public @Nullable UUID getApplicationId() {
    return applicationId;
  }

  @JsonProperty("application_id")
  public void setApplicationId(@Nullable UUID applicationId) {
    this.applicationId = applicationId;
  }

  public SessionResponse sessionType(SessionTypeEnum sessionType) {
    this.sessionType = sessionType;
    return this;
  }

  /**
   * Get sessionType
   * @return sessionType
   */
  @NotNull 
  @Schema(name = "session_type", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("session_type")
  public SessionTypeEnum getSessionType() {
    return sessionType;
  }

  @JsonProperty("session_type")
  public void setSessionType(SessionTypeEnum sessionType) {
    this.sessionType = sessionType;
  }

  public SessionResponse summary(@Nullable String summary) {
    this.summary = summary;
    return this;
  }

  /**
   * AI-generated memory summary, set after enough messages accumulate
   * @return summary
   */
  
  @Schema(name = "summary", description = "AI-generated memory summary, set after enough messages accumulate", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("summary")
  public @Nullable String getSummary() {
    return summary;
  }

  @JsonProperty("summary")
  public void setSummary(@Nullable String summary) {
    this.summary = summary;
  }

  public SessionResponse firstMessage(@Nullable String firstMessage) {
    this.firstMessage = firstMessage;
    return this;
  }

  /**
   * First user message in the session — used as the display title in the UI
   * @return firstMessage
   */
  
  @Schema(name = "first_message", description = "First user message in the session — used as the display title in the UI", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("first_message")
  public @Nullable String getFirstMessage() {
    return firstMessage;
  }

  @JsonProperty("first_message")
  public void setFirstMessage(@Nullable String firstMessage) {
    this.firstMessage = firstMessage;
  }

  public SessionResponse interviewStatus(@Nullable InterviewStatusEnum interviewStatus) {
    this.interviewStatus = interviewStatus;
    return this;
  }

  /**
   * Lifecycle of a mock interview; null for every other session type
   * @return interviewStatus
   */
  
  @Schema(name = "interview_status", description = "Lifecycle of a mock interview; null for every other session type", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("interview_status")
  public @Nullable InterviewStatusEnum getInterviewStatus() {
    return interviewStatus;
  }

  @JsonProperty("interview_status")
  public void setInterviewStatus(@Nullable InterviewStatusEnum interviewStatus) {
    this.interviewStatus = interviewStatus;
  }

  public SessionResponse interviewScore(@Nullable Integer interviewScore) {
    this.interviewScore = interviewScore;
    return this;
  }

  /**
   * Final mock-interview score (0-100), set once the interview is completed
   * @return interviewScore
   */
  
  @Schema(name = "interview_score", description = "Final mock-interview score (0-100), set once the interview is completed", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("interview_score")
  public @Nullable Integer getInterviewScore() {
    return interviewScore;
  }

  @JsonProperty("interview_score")
  public void setInterviewScore(@Nullable Integer interviewScore) {
    this.interviewScore = interviewScore;
  }

  public SessionResponse createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Get createdAt
   * @return createdAt
   */
  @NotNull @Valid 
  @Schema(name = "created_at", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("created_at")
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  @JsonProperty("created_at")
  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SessionResponse sessionResponse = (SessionResponse) o;
    return Objects.equals(this.id, sessionResponse.id) &&
        Objects.equals(this.userId, sessionResponse.userId) &&
        Objects.equals(this.applicationId, sessionResponse.applicationId) &&
        Objects.equals(this.sessionType, sessionResponse.sessionType) &&
        Objects.equals(this.summary, sessionResponse.summary) &&
        Objects.equals(this.firstMessage, sessionResponse.firstMessage) &&
        Objects.equals(this.interviewStatus, sessionResponse.interviewStatus) &&
        Objects.equals(this.interviewScore, sessionResponse.interviewScore) &&
        Objects.equals(this.createdAt, sessionResponse.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, userId, applicationId, sessionType, summary, firstMessage, interviewStatus, interviewScore, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SessionResponse {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    applicationId: ").append(toIndentedString(applicationId)).append("\n");
    sb.append("    sessionType: ").append(toIndentedString(sessionType)).append("\n");
    sb.append("    summary: ").append(toIndentedString(summary)).append("\n");
    sb.append("    firstMessage: ").append(toIndentedString(firstMessage)).append("\n");
    sb.append("    interviewStatus: ").append(toIndentedString(interviewStatus)).append("\n");
    sb.append("    interviewScore: ").append(toIndentedString(interviewScore)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
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

