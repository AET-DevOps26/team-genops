package com.jobready.application.generated.modelDto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.jobready.application.generated.modelDto.ApplicationStage;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * CreateApplicationRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class CreateApplicationRequest {

  private String company;

  private String jobTitle;

  private String jobDescription;

  private @Nullable ApplicationStage stage;

  private @Nullable String jobUrl;

  private @Nullable String companyWebsite;

  private @Nullable String linkedinUrl;

  private @Nullable String notes;

  public CreateApplicationRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateApplicationRequest(String company, String jobTitle, String jobDescription) {
    this.company = company;
    this.jobTitle = jobTitle;
    this.jobDescription = jobDescription;
  }

  public CreateApplicationRequest company(String company) {
    this.company = company;
    return this;
  }

  /**
   * Get company
   * @return company
   */
  @NotNull @Size(min = 1, max = 255) 
  @Schema(name = "company", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("company")
  public String getCompany() {
    return company;
  }

  @JsonProperty("company")
  public void setCompany(String company) {
    this.company = company;
  }

  public CreateApplicationRequest jobTitle(String jobTitle) {
    this.jobTitle = jobTitle;
    return this;
  }

  /**
   * Get jobTitle
   * @return jobTitle
   */
  @NotNull @Size(min = 1, max = 255) 
  @Schema(name = "job_title", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("job_title")
  public String getJobTitle() {
    return jobTitle;
  }

  @JsonProperty("job_title")
  public void setJobTitle(String jobTitle) {
    this.jobTitle = jobTitle;
  }

  public CreateApplicationRequest jobDescription(String jobDescription) {
    this.jobDescription = jobDescription;
    return this;
  }

  /**
   * Get jobDescription
   * @return jobDescription
   */
  @NotNull @Size(min = 1) 
  @Schema(name = "job_description", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("job_description")
  public String getJobDescription() {
    return jobDescription;
  }

  @JsonProperty("job_description")
  public void setJobDescription(String jobDescription) {
    this.jobDescription = jobDescription;
  }

  public CreateApplicationRequest stage(@Nullable ApplicationStage stage) {
    this.stage = stage;
    return this;
  }

  /**
   * Stage to create the application in. Defaults to `draft` when omitted.
   * @return stage
   */
  @Valid 
  @Schema(name = "stage", description = "Stage to create the application in. Defaults to `draft` when omitted.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("stage")
  public @Nullable ApplicationStage getStage() {
    return stage;
  }

  @JsonProperty("stage")
  public void setStage(@Nullable ApplicationStage stage) {
    this.stage = stage;
  }

  public CreateApplicationRequest jobUrl(@Nullable String jobUrl) {
    this.jobUrl = jobUrl;
    return this;
  }

  /**
   * Get jobUrl
   * @return jobUrl
   */
  @Size(max = 512) 
  @Schema(name = "job_url", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("job_url")
  public @Nullable String getJobUrl() {
    return jobUrl;
  }

  @JsonProperty("job_url")
  public void setJobUrl(@Nullable String jobUrl) {
    this.jobUrl = jobUrl;
  }

  public CreateApplicationRequest companyWebsite(@Nullable String companyWebsite) {
    this.companyWebsite = companyWebsite;
    return this;
  }

  /**
   * Get companyWebsite
   * @return companyWebsite
   */
  @Size(max = 512) 
  @Schema(name = "company_website", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("company_website")
  public @Nullable String getCompanyWebsite() {
    return companyWebsite;
  }

  @JsonProperty("company_website")
  public void setCompanyWebsite(@Nullable String companyWebsite) {
    this.companyWebsite = companyWebsite;
  }

  public CreateApplicationRequest linkedinUrl(@Nullable String linkedinUrl) {
    this.linkedinUrl = linkedinUrl;
    return this;
  }

  /**
   * Get linkedinUrl
   * @return linkedinUrl
   */
  @Size(max = 512) 
  @Schema(name = "linkedin_url", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("linkedin_url")
  public @Nullable String getLinkedinUrl() {
    return linkedinUrl;
  }

  @JsonProperty("linkedin_url")
  public void setLinkedinUrl(@Nullable String linkedinUrl) {
    this.linkedinUrl = linkedinUrl;
  }

  public CreateApplicationRequest notes(@Nullable String notes) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateApplicationRequest createApplicationRequest = (CreateApplicationRequest) o;
    return Objects.equals(this.company, createApplicationRequest.company) &&
        Objects.equals(this.jobTitle, createApplicationRequest.jobTitle) &&
        Objects.equals(this.jobDescription, createApplicationRequest.jobDescription) &&
        Objects.equals(this.stage, createApplicationRequest.stage) &&
        Objects.equals(this.jobUrl, createApplicationRequest.jobUrl) &&
        Objects.equals(this.companyWebsite, createApplicationRequest.companyWebsite) &&
        Objects.equals(this.linkedinUrl, createApplicationRequest.linkedinUrl) &&
        Objects.equals(this.notes, createApplicationRequest.notes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(company, jobTitle, jobDescription, stage, jobUrl, companyWebsite, linkedinUrl, notes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateApplicationRequest {\n");
    sb.append("    company: ").append(toIndentedString(company)).append("\n");
    sb.append("    jobTitle: ").append(toIndentedString(jobTitle)).append("\n");
    sb.append("    jobDescription: ").append(toIndentedString(jobDescription)).append("\n");
    sb.append("    stage: ").append(toIndentedString(stage)).append("\n");
    sb.append("    jobUrl: ").append(toIndentedString(jobUrl)).append("\n");
    sb.append("    companyWebsite: ").append(toIndentedString(companyWebsite)).append("\n");
    sb.append("    linkedinUrl: ").append(toIndentedString(linkedinUrl)).append("\n");
    sb.append("    notes: ").append(toIndentedString(notes)).append("\n");
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

