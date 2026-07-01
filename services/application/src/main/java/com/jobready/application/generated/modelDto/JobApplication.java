package com.jobready.application.generated.modelDto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
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
 * JobApplication
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class JobApplication {

  private UUID id;

  private String company;

  private String jobTitle;

  private @Nullable String jobDescription = null;

  private @Nullable String jobUrl = null;

  private ApplicationStage stage;

  private @Nullable String notes = null;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime appliedAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime updatedAt;

  public JobApplication() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public JobApplication(UUID id, String company, String jobTitle, ApplicationStage stage, OffsetDateTime appliedAt, OffsetDateTime updatedAt) {
    this.id = id;
    this.company = company;
    this.jobTitle = jobTitle;
    this.stage = stage;
    this.appliedAt = appliedAt;
    this.updatedAt = updatedAt;
  }

  public JobApplication id(UUID id) {
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

  public JobApplication company(String company) {
    this.company = company;
    return this;
  }

  /**
   * Get company
   * @return company
   */
  @NotNull 
  @Schema(name = "company", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("company")
  public String getCompany() {
    return company;
  }

  @JsonProperty("company")
  public void setCompany(String company) {
    this.company = company;
  }

  public JobApplication jobTitle(String jobTitle) {
    this.jobTitle = jobTitle;
    return this;
  }

  /**
   * Get jobTitle
   * @return jobTitle
   */
  @NotNull 
  @Schema(name = "job_title", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("job_title")
  public String getJobTitle() {
    return jobTitle;
  }

  @JsonProperty("job_title")
  public void setJobTitle(String jobTitle) {
    this.jobTitle = jobTitle;
  }

  public JobApplication jobDescription(@Nullable String jobDescription) {
    this.jobDescription = jobDescription;
    return this;
  }

  /**
   * Free-text job description pasted by the user
   * @return jobDescription
   */
  
  @Schema(name = "job_description", description = "Free-text job description pasted by the user", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("job_description")
  public @Nullable String getJobDescription() {
    return jobDescription;
  }

  @JsonProperty("job_description")
  public void setJobDescription(@Nullable String jobDescription) {
    this.jobDescription = jobDescription;
  }

  public JobApplication jobUrl(@Nullable String jobUrl) {
    this.jobUrl = jobUrl;
    return this;
  }

  /**
   * Get jobUrl
   * @return jobUrl
   */
  
  @Schema(name = "job_url", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("job_url")
  public @Nullable String getJobUrl() {
    return jobUrl;
  }

  @JsonProperty("job_url")
  public void setJobUrl(@Nullable String jobUrl) {
    this.jobUrl = jobUrl;
  }

  public JobApplication stage(ApplicationStage stage) {
    this.stage = stage;
    return this;
  }

  /**
   * Get stage
   * @return stage
   */
  @NotNull @Valid 
  @Schema(name = "stage", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("stage")
  public ApplicationStage getStage() {
    return stage;
  }

  @JsonProperty("stage")
  public void setStage(ApplicationStage stage) {
    this.stage = stage;
  }

  public JobApplication notes(@Nullable String notes) {
    this.notes = notes;
    return this;
  }

  /**
   * Get notes
   * @return notes
   */
  
  @Schema(name = "notes", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("notes")
  public @Nullable String getNotes() {
    return notes;
  }

  @JsonProperty("notes")
  public void setNotes(@Nullable String notes) {
    this.notes = notes;
  }

  public JobApplication appliedAt(OffsetDateTime appliedAt) {
    this.appliedAt = appliedAt;
    return this;
  }

  /**
   * Get appliedAt
   * @return appliedAt
   */
  @NotNull @Valid 
  @Schema(name = "applied_at", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("applied_at")
  public OffsetDateTime getAppliedAt() {
    return appliedAt;
  }

  @JsonProperty("applied_at")
  public void setAppliedAt(OffsetDateTime appliedAt) {
    this.appliedAt = appliedAt;
  }

  public JobApplication updatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  /**
   * Get updatedAt
   * @return updatedAt
   */
  @NotNull @Valid 
  @Schema(name = "updated_at", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("updated_at")
  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  @JsonProperty("updated_at")
  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JobApplication jobApplication = (JobApplication) o;
    return Objects.equals(this.id, jobApplication.id) &&
        Objects.equals(this.company, jobApplication.company) &&
        Objects.equals(this.jobTitle, jobApplication.jobTitle) &&
        Objects.equals(this.jobDescription, jobApplication.jobDescription) &&
        Objects.equals(this.jobUrl, jobApplication.jobUrl) &&
        Objects.equals(this.stage, jobApplication.stage) &&
        Objects.equals(this.notes, jobApplication.notes) &&
        Objects.equals(this.appliedAt, jobApplication.appliedAt) &&
        Objects.equals(this.updatedAt, jobApplication.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, company, jobTitle, jobDescription, jobUrl, stage, notes, appliedAt, updatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class JobApplication {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    company: ").append(toIndentedString(company)).append("\n");
    sb.append("    jobTitle: ").append(toIndentedString(jobTitle)).append("\n");
    sb.append("    jobDescription: ").append(toIndentedString(jobDescription)).append("\n");
    sb.append("    jobUrl: ").append(toIndentedString(jobUrl)).append("\n");
    sb.append("    stage: ").append(toIndentedString(stage)).append("\n");
    sb.append("    notes: ").append(toIndentedString(notes)).append("\n");
    sb.append("    appliedAt: ").append(toIndentedString(appliedAt)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
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

