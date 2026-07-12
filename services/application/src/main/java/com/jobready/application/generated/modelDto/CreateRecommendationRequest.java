package com.jobready.application.generated.modelDto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * CreateRecommendationRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.22.0")
public class CreateRecommendationRequest {

  private String insight;

  private String recommendedAction;

  public CreateRecommendationRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateRecommendationRequest(String insight, String recommendedAction) {
    this.insight = insight;
    this.recommendedAction = recommendedAction;
  }

  public CreateRecommendationRequest insight(String insight) {
    this.insight = insight;
    return this;
  }

  /**
   * Get insight
   * @return insight
   */
  @NotNull @Size(min = 1) 
  @Schema(name = "insight", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("insight")
  public String getInsight() {
    return insight;
  }

  @JsonProperty("insight")
  public void setInsight(String insight) {
    this.insight = insight;
  }

  public CreateRecommendationRequest recommendedAction(String recommendedAction) {
    this.recommendedAction = recommendedAction;
    return this;
  }

  /**
   * Get recommendedAction
   * @return recommendedAction
   */
  @NotNull @Size(min = 1) 
  @Schema(name = "recommended_action", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("recommended_action")
  public String getRecommendedAction() {
    return recommendedAction;
  }

  @JsonProperty("recommended_action")
  public void setRecommendedAction(String recommendedAction) {
    this.recommendedAction = recommendedAction;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateRecommendationRequest createRecommendationRequest = (CreateRecommendationRequest) o;
    return Objects.equals(this.insight, createRecommendationRequest.insight) &&
        Objects.equals(this.recommendedAction, createRecommendationRequest.recommendedAction);
  }

  @Override
  public int hashCode() {
    return Objects.hash(insight, recommendedAction);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateRecommendationRequest {\n");
    sb.append("    insight: ").append(toIndentedString(insight)).append("\n");
    sb.append("    recommendedAction: ").append(toIndentedString(recommendedAction)).append("\n");
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

