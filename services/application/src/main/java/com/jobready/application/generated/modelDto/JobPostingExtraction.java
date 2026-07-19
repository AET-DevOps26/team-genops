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
 * JobPostingExtraction
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class JobPostingExtraction {

  private @Nullable String company = null;

  private @Nullable String jobTitle = null;

  private @Nullable String jobDescription = null;

  public JobPostingExtraction company(@Nullable String company) {
    this.company = company;
    return this;
  }

  /**
   * Get company
   * @return company
   */
  
  @Schema(name = "company", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("company")
  public @Nullable String getCompany() {
    return company;
  }

  @JsonProperty("company")
  public void setCompany(@Nullable String company) {
    this.company = company;
  }

  public JobPostingExtraction jobTitle(@Nullable String jobTitle) {
    this.jobTitle = jobTitle;
    return this;
  }

  /**
   * Get jobTitle
   * @return jobTitle
   */
  
  @Schema(name = "job_title", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("job_title")
  public @Nullable String getJobTitle() {
    return jobTitle;
  }

  @JsonProperty("job_title")
  public void setJobTitle(@Nullable String jobTitle) {
    this.jobTitle = jobTitle;
  }

  public JobPostingExtraction jobDescription(@Nullable String jobDescription) {
    this.jobDescription = jobDescription;
    return this;
  }

  /**
   * The posting's full description text, cleaned of page furniture
   * @return jobDescription
   */
  
  @Schema(name = "job_description", description = "The posting's full description text, cleaned of page furniture", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("job_description")
  public @Nullable String getJobDescription() {
    return jobDescription;
  }

  @JsonProperty("job_description")
  public void setJobDescription(@Nullable String jobDescription) {
    this.jobDescription = jobDescription;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JobPostingExtraction jobPostingExtraction = (JobPostingExtraction) o;
    return Objects.equals(this.company, jobPostingExtraction.company) &&
        Objects.equals(this.jobTitle, jobPostingExtraction.jobTitle) &&
        Objects.equals(this.jobDescription, jobPostingExtraction.jobDescription);
  }

  @Override
  public int hashCode() {
    return Objects.hash(company, jobTitle, jobDescription);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class JobPostingExtraction {\n");
    sb.append("    company: ").append(toIndentedString(company)).append("\n");
    sb.append("    jobTitle: ").append(toIndentedString(jobTitle)).append("\n");
    sb.append("    jobDescription: ").append(toIndentedString(jobDescription)).append("\n");
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

