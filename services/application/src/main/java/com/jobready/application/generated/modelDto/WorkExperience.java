package com.jobready.application.generated.modelDto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
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
 * WorkExperience
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class WorkExperience {

  private UUID id;

  private String company;

  private String role;

  private @Nullable String location = null;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate startDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate endDate = null;

  private Boolean isCurrent;

  private @Nullable String description = null;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public WorkExperience() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public WorkExperience(UUID id, String company, String role, LocalDate startDate, Boolean isCurrent, OffsetDateTime createdAt) {
    this.id = id;
    this.company = company;
    this.role = role;
    this.startDate = startDate;
    this.isCurrent = isCurrent;
    this.createdAt = createdAt;
  }

  public WorkExperience id(UUID id) {
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

  public WorkExperience company(String company) {
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

  public WorkExperience role(String role) {
    this.role = role;
    return this;
  }

  /**
   * Get role
   * @return role
   */
  @NotNull 
  @Schema(name = "role", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("role")
  public String getRole() {
    return role;
  }

  @JsonProperty("role")
  public void setRole(String role) {
    this.role = role;
  }

  public WorkExperience location(@Nullable String location) {
    this.location = location;
    return this;
  }

  /**
   * Get location
   * @return location
   */
  
  @Schema(name = "location", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("location")
  public @Nullable String getLocation() {
    return location;
  }

  @JsonProperty("location")
  public void setLocation(@Nullable String location) {
    this.location = location;
  }

  public WorkExperience startDate(LocalDate startDate) {
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

  public WorkExperience endDate(@Nullable LocalDate endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * Null while `is_current` is true
   * @return endDate
   */
  @Valid 
  @Schema(name = "end_date", description = "Null while `is_current` is true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("end_date")
  public @Nullable LocalDate getEndDate() {
    return endDate;
  }

  @JsonProperty("end_date")
  public void setEndDate(@Nullable LocalDate endDate) {
    this.endDate = endDate;
  }

  public WorkExperience isCurrent(Boolean isCurrent) {
    this.isCurrent = isCurrent;
    return this;
  }

  /**
   * Get isCurrent
   * @return isCurrent
   */
  @NotNull 
  @Schema(name = "is_current", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("is_current")
  public Boolean getIsCurrent() {
    return isCurrent;
  }

  @JsonProperty("is_current")
  public void setIsCurrent(Boolean isCurrent) {
    this.isCurrent = isCurrent;
  }

  public WorkExperience description(@Nullable String description) {
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

  public WorkExperience createdAt(OffsetDateTime createdAt) {
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
    WorkExperience workExperience = (WorkExperience) o;
    return Objects.equals(this.id, workExperience.id) &&
        Objects.equals(this.company, workExperience.company) &&
        Objects.equals(this.role, workExperience.role) &&
        Objects.equals(this.location, workExperience.location) &&
        Objects.equals(this.startDate, workExperience.startDate) &&
        Objects.equals(this.endDate, workExperience.endDate) &&
        Objects.equals(this.isCurrent, workExperience.isCurrent) &&
        Objects.equals(this.description, workExperience.description) &&
        Objects.equals(this.createdAt, workExperience.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, company, role, location, startDate, endDate, isCurrent, description, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkExperience {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    company: ").append(toIndentedString(company)).append("\n");
    sb.append("    role: ").append(toIndentedString(role)).append("\n");
    sb.append("    location: ").append(toIndentedString(location)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
    sb.append("    isCurrent: ").append(toIndentedString(isCurrent)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
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

