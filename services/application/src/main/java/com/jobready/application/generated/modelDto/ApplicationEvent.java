package com.jobready.application.generated.modelDto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.jobready.application.generated.modelDto.ApplicationEventType;
import com.jobready.application.generated.modelDto.ApplicationStage;
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
 * ApplicationEvent
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class ApplicationEvent {

  private UUID id;

  private UUID applicationId;

  private ApplicationEventType eventType;

  private String title;

  private @Nullable String description = null;

  private @Nullable ApplicationStage stageFrom = null;

  private @Nullable ApplicationStage stageTo = null;

  /**
   * Whether the event was derived from a detected email or a manual change
   */
  public enum SourceEnum {
    EMAIL("email"),
    
    MANUAL("manual");

    private final String value;

    SourceEnum(String value) {
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
    public static SourceEnum fromValue(String value) {
      for (SourceEnum b : SourceEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private SourceEnum source;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime occurredAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public ApplicationEvent() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ApplicationEvent(UUID id, UUID applicationId, ApplicationEventType eventType, String title, SourceEnum source, OffsetDateTime occurredAt, OffsetDateTime createdAt) {
    this.id = id;
    this.applicationId = applicationId;
    this.eventType = eventType;
    this.title = title;
    this.source = source;
    this.occurredAt = occurredAt;
    this.createdAt = createdAt;
  }

  public ApplicationEvent id(UUID id) {
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

  public ApplicationEvent applicationId(UUID applicationId) {
    this.applicationId = applicationId;
    return this;
  }

  /**
   * Get applicationId
   * @return applicationId
   */
  @NotNull @Valid 
  @Schema(name = "application_id", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("application_id")
  public UUID getApplicationId() {
    return applicationId;
  }

  @JsonProperty("application_id")
  public void setApplicationId(UUID applicationId) {
    this.applicationId = applicationId;
  }

  public ApplicationEvent eventType(ApplicationEventType eventType) {
    this.eventType = eventType;
    return this;
  }

  /**
   * Get eventType
   * @return eventType
   */
  @NotNull @Valid 
  @Schema(name = "event_type", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("event_type")
  public ApplicationEventType getEventType() {
    return eventType;
  }

  @JsonProperty("event_type")
  public void setEventType(ApplicationEventType eventType) {
    this.eventType = eventType;
  }

  public ApplicationEvent title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Short human-readable label for the event
   * @return title
   */
  @NotNull 
  @Schema(name = "title", description = "Short human-readable label for the event", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(String title) {
    this.title = title;
  }

  public ApplicationEvent description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Longer summary of what happened (LLM-generated for email events)
   * @return description
   */
  
  @Schema(name = "description", description = "Longer summary of what happened (LLM-generated for email events)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public ApplicationEvent stageFrom(@Nullable ApplicationStage stageFrom) {
    this.stageFrom = stageFrom;
    return this;
  }

  /**
   * Get stageFrom
   * @return stageFrom
   */
  @Valid 
  @Schema(name = "stage_from", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("stage_from")
  public @Nullable ApplicationStage getStageFrom() {
    return stageFrom;
  }

  @JsonProperty("stage_from")
  public void setStageFrom(@Nullable ApplicationStage stageFrom) {
    this.stageFrom = stageFrom;
  }

  public ApplicationEvent stageTo(@Nullable ApplicationStage stageTo) {
    this.stageTo = stageTo;
    return this;
  }

  /**
   * Get stageTo
   * @return stageTo
   */
  @Valid 
  @Schema(name = "stage_to", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("stage_to")
  public @Nullable ApplicationStage getStageTo() {
    return stageTo;
  }

  @JsonProperty("stage_to")
  public void setStageTo(@Nullable ApplicationStage stageTo) {
    this.stageTo = stageTo;
  }

  public ApplicationEvent source(SourceEnum source) {
    this.source = source;
    return this;
  }

  /**
   * Whether the event was derived from a detected email or a manual change
   * @return source
   */
  @NotNull 
  @Schema(name = "source", description = "Whether the event was derived from a detected email or a manual change", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("source")
  public SourceEnum getSource() {
    return source;
  }

  @JsonProperty("source")
  public void setSource(SourceEnum source) {
    this.source = source;
  }

  public ApplicationEvent occurredAt(OffsetDateTime occurredAt) {
    this.occurredAt = occurredAt;
    return this;
  }

  /**
   * When the event actually happened (for email events, the email's received date)
   * @return occurredAt
   */
  @NotNull @Valid 
  @Schema(name = "occurred_at", description = "When the event actually happened (for email events, the email's received date)", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("occurred_at")
  public OffsetDateTime getOccurredAt() {
    return occurredAt;
  }

  @JsonProperty("occurred_at")
  public void setOccurredAt(OffsetDateTime occurredAt) {
    this.occurredAt = occurredAt;
  }

  public ApplicationEvent createdAt(OffsetDateTime createdAt) {
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
    ApplicationEvent applicationEvent = (ApplicationEvent) o;
    return Objects.equals(this.id, applicationEvent.id) &&
        Objects.equals(this.applicationId, applicationEvent.applicationId) &&
        Objects.equals(this.eventType, applicationEvent.eventType) &&
        Objects.equals(this.title, applicationEvent.title) &&
        Objects.equals(this.description, applicationEvent.description) &&
        Objects.equals(this.stageFrom, applicationEvent.stageFrom) &&
        Objects.equals(this.stageTo, applicationEvent.stageTo) &&
        Objects.equals(this.source, applicationEvent.source) &&
        Objects.equals(this.occurredAt, applicationEvent.occurredAt) &&
        Objects.equals(this.createdAt, applicationEvent.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, applicationId, eventType, title, description, stageFrom, stageTo, source, occurredAt, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApplicationEvent {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    applicationId: ").append(toIndentedString(applicationId)).append("\n");
    sb.append("    eventType: ").append(toIndentedString(eventType)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    stageFrom: ").append(toIndentedString(stageFrom)).append("\n");
    sb.append("    stageTo: ").append(toIndentedString(stageTo)).append("\n");
    sb.append("    source: ").append(toIndentedString(source)).append("\n");
    sb.append("    occurredAt: ").append(toIndentedString(occurredAt)).append("\n");
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

