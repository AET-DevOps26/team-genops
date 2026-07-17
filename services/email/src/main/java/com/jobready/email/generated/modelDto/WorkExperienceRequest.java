package com.jobready.email.generated.modelDto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * WorkExperienceRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class WorkExperienceRequest {

  private String company;

  private String role;

  private @Nullable String location;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate startDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate endDate;

  private Boolean isCurrent = false;

  private @Nullable String description;

  public WorkExperienceRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public WorkExperienceRequest(String company, String role, LocalDate startDate) {
    this.company = company;
    this.role = role;
    this.startDate = startDate;
  }

  public WorkExperienceRequest company(String company) {
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

  public WorkExperienceRequest role(String role) {
    this.role = role;
    return this;
  }

  /**
   * Get role
   * @return role
   */
  @NotNull @Size(min = 1, max = 255) 
  @Schema(name = "role", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("role")
  public String getRole() {
    return role;
  }

  @JsonProperty("role")
  public void setRole(String role) {
    this.role = role;
  }

  public WorkExperienceRequest location(@Nullable String location) {
    this.location = location;
    return this;
  }

  /**
   * Get location
   * @return location
   */
  @Size(max = 255) 
  @Schema(name = "location", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("location")
  public @Nullable String getLocation() {
    return location;
  }

  @JsonProperty("location")
  public void setLocation(@Nullable String location) {
    this.location = location;
  }

  public WorkExperienceRequest startDate(LocalDate startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * Get startDate
   * @return startDate
   */
  @NotNull @Valid 
  @Schema(name = "start_date", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("start_date")
  public LocalDate getStartDate() {
    return startDate;
  }

  @JsonProperty("start_date")
  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public WorkExperienceRequest endDate(@Nullable LocalDate endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * Get endDate
   * @return endDate
   */
  @Valid 
  @Schema(name = "end_date", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("end_date")
  public @Nullable LocalDate getEndDate() {
    return endDate;
  }

  @JsonProperty("end_date")
  public void setEndDate(@Nullable LocalDate endDate) {
    this.endDate = endDate;
  }

  public WorkExperienceRequest isCurrent(Boolean isCurrent) {
    this.isCurrent = isCurrent;
    return this;
  }

  /**
   * Get isCurrent
   * @return isCurrent
   */
  
  @Schema(name = "is_current", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("is_current")
  public Boolean getIsCurrent() {
    return isCurrent;
  }

  @JsonProperty("is_current")
  public void setIsCurrent(Boolean isCurrent) {
    this.isCurrent = isCurrent;
  }

  public WorkExperienceRequest description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Get description
   * @return description
   */
  
  @Schema(name = "description", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WorkExperienceRequest workExperienceRequest = (WorkExperienceRequest) o;
    return Objects.equals(this.company, workExperienceRequest.company) &&
        Objects.equals(this.role, workExperienceRequest.role) &&
        Objects.equals(this.location, workExperienceRequest.location) &&
        Objects.equals(this.startDate, workExperienceRequest.startDate) &&
        Objects.equals(this.endDate, workExperienceRequest.endDate) &&
        Objects.equals(this.isCurrent, workExperienceRequest.isCurrent) &&
        Objects.equals(this.description, workExperienceRequest.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(company, role, location, startDate, endDate, isCurrent, description);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkExperienceRequest {\n");
    sb.append("    company: ").append(toIndentedString(company)).append("\n");
    sb.append("    role: ").append(toIndentedString(role)).append("\n");
    sb.append("    location: ").append(toIndentedString(location)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
    sb.append("    isCurrent: ").append(toIndentedString(isCurrent)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
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

