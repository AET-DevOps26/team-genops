package com.jobready.email.generated.modelDto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
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
 * Recommendation
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class Recommendation {

  private UUID id;

  private UUID applicationId;

  private String insight;

  private String recommendedAction;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public Recommendation() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Recommendation(UUID id, UUID applicationId, String insight, String recommendedAction, OffsetDateTime createdAt) {
    this.id = id;
    this.applicationId = applicationId;
    this.insight = insight;
    this.recommendedAction = recommendedAction;
    this.createdAt = createdAt;
  }

  public Recommendation id(UUID id) {
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

  public Recommendation applicationId(UUID applicationId) {
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

  public Recommendation insight(String insight) {
    this.insight = insight;
    return this;
  }

  /**
   * The observation the recommendation is based on
   * @return insight
   */
  @NotNull 
  @Schema(name = "insight", description = "The observation the recommendation is based on", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("insight")
  public String getInsight() {
    return insight;
  }

  @JsonProperty("insight")
  public void setInsight(String insight) {
    this.insight = insight;
  }

  public Recommendation recommendedAction(String recommendedAction) {
    this.recommendedAction = recommendedAction;
    return this;
  }

  /**
   * The suggested next best action
   * @return recommendedAction
   */
  @NotNull 
  @Schema(name = "recommended_action", description = "The suggested next best action", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("recommended_action")
  public String getRecommendedAction() {
    return recommendedAction;
  }

  @JsonProperty("recommended_action")
  public void setRecommendedAction(String recommendedAction) {
    this.recommendedAction = recommendedAction;
  }

  public Recommendation createdAt(OffsetDateTime createdAt) {
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
    Recommendation recommendation = (Recommendation) o;
    return Objects.equals(this.id, recommendation.id) &&
        Objects.equals(this.applicationId, recommendation.applicationId) &&
        Objects.equals(this.insight, recommendation.insight) &&
        Objects.equals(this.recommendedAction, recommendation.recommendedAction) &&
        Objects.equals(this.createdAt, recommendation.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, applicationId, insight, recommendedAction, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Recommendation {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    applicationId: ").append(toIndentedString(applicationId)).append("\n");
    sb.append("    insight: ").append(toIndentedString(insight)).append("\n");
    sb.append("    recommendedAction: ").append(toIndentedString(recommendedAction)).append("\n");
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

