package com.jobready.document.generated.modelDto;

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
 * ApplicationSummary
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class ApplicationSummary {

  private Long applied;

  private Long followUp;

  private Long interview;

  private Long offer;

  private Long closed;

  private Long total;

  public ApplicationSummary() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ApplicationSummary(Long applied, Long followUp, Long interview, Long offer, Long closed, Long total) {
    this.applied = applied;
    this.followUp = followUp;
    this.interview = interview;
    this.offer = offer;
    this.closed = closed;
    this.total = total;
  }

  public ApplicationSummary applied(Long applied) {
    this.applied = applied;
    return this;
  }

  /**
   * Get applied
   * @return applied
   */
  @NotNull 
  @Schema(name = "applied", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("applied")
  public Long getApplied() {
    return applied;
  }

  @JsonProperty("applied")
  public void setApplied(Long applied) {
    this.applied = applied;
  }

  public ApplicationSummary followUp(Long followUp) {
    this.followUp = followUp;
    return this;
  }

  /**
   * Get followUp
   * @return followUp
   */
  @NotNull 
  @Schema(name = "follow_up", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("follow_up")
  public Long getFollowUp() {
    return followUp;
  }

  @JsonProperty("follow_up")
  public void setFollowUp(Long followUp) {
    this.followUp = followUp;
  }

  public ApplicationSummary interview(Long interview) {
    this.interview = interview;
    return this;
  }

  /**
   * Get interview
   * @return interview
   */
  @NotNull 
  @Schema(name = "interview", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("interview")
  public Long getInterview() {
    return interview;
  }

  @JsonProperty("interview")
  public void setInterview(Long interview) {
    this.interview = interview;
  }

  public ApplicationSummary offer(Long offer) {
    this.offer = offer;
    return this;
  }

  /**
   * Get offer
   * @return offer
   */
  @NotNull 
  @Schema(name = "offer", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("offer")
  public Long getOffer() {
    return offer;
  }

  @JsonProperty("offer")
  public void setOffer(Long offer) {
    this.offer = offer;
  }

  public ApplicationSummary closed(Long closed) {
    this.closed = closed;
    return this;
  }

  /**
   * Get closed
   * @return closed
   */
  @NotNull 
  @Schema(name = "closed", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("closed")
  public Long getClosed() {
    return closed;
  }

  @JsonProperty("closed")
  public void setClosed(Long closed) {
    this.closed = closed;
  }

  public ApplicationSummary total(Long total) {
    this.total = total;
    return this;
  }

  /**
   * Get total
   * @return total
   */
  @NotNull 
  @Schema(name = "total", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("total")
  public Long getTotal() {
    return total;
  }

  @JsonProperty("total")
  public void setTotal(Long total) {
    this.total = total;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApplicationSummary applicationSummary = (ApplicationSummary) o;
    return Objects.equals(this.applied, applicationSummary.applied) &&
        Objects.equals(this.followUp, applicationSummary.followUp) &&
        Objects.equals(this.interview, applicationSummary.interview) &&
        Objects.equals(this.offer, applicationSummary.offer) &&
        Objects.equals(this.closed, applicationSummary.closed) &&
        Objects.equals(this.total, applicationSummary.total);
  }

  @Override
  public int hashCode() {
    return Objects.hash(applied, followUp, interview, offer, closed, total);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApplicationSummary {\n");
    sb.append("    applied: ").append(toIndentedString(applied)).append("\n");
    sb.append("    followUp: ").append(toIndentedString(followUp)).append("\n");
    sb.append("    interview: ").append(toIndentedString(interview)).append("\n");
    sb.append("    offer: ").append(toIndentedString(offer)).append("\n");
    sb.append("    closed: ").append(toIndentedString(closed)).append("\n");
    sb.append("    total: ").append(toIndentedString(total)).append("\n");
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

