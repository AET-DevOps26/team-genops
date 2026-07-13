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
 * Education
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class Education {

  private UUID id;

  private String institution;

  private String degree;

  private @Nullable String field = null;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate startDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate endDate = null;

  private @Nullable String description = null;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public Education() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Education(UUID id, String institution, String degree, LocalDate startDate, OffsetDateTime createdAt) {
    this.id = id;
    this.institution = institution;
    this.degree = degree;
    this.startDate = startDate;
    this.createdAt = createdAt;
  }

  public Education id(UUID id) {
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

  public Education institution(String institution) {
    this.institution = institution;
    return this;
  }

  /**
   * Get institution
   * @return institution
   */
  @NotNull 
  @Schema(name = "institution", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("institution")
  public String getInstitution() {
    return institution;
  }

  @JsonProperty("institution")
  public void setInstitution(String institution) {
    this.institution = institution;
  }

  public Education degree(String degree) {
    this.degree = degree;
    return this;
  }

  /**
   * Get degree
   * @return degree
   */
  @NotNull 
  @Schema(name = "degree", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("degree")
  public String getDegree() {
    return degree;
  }

  @JsonProperty("degree")
  public void setDegree(String degree) {
    this.degree = degree;
  }

  public Education field(@Nullable String field) {
    this.field = field;
    return this;
  }

  /**
   * Get field
   * @return field
   */
  
  @Schema(name = "field", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("field")
  public @Nullable String getField() {
    return field;
  }

  @JsonProperty("field")
  public void setField(@Nullable String field) {
    this.field = field;
  }

  public Education startDate(LocalDate startDate) {
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

  public Education endDate(@Nullable LocalDate endDate) {
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

  public Education description(@Nullable String description) {
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

  public Education createdAt(OffsetDateTime createdAt) {
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
    Education education = (Education) o;
    return Objects.equals(this.id, education.id) &&
        Objects.equals(this.institution, education.institution) &&
        Objects.equals(this.degree, education.degree) &&
        Objects.equals(this.field, education.field) &&
        Objects.equals(this.startDate, education.startDate) &&
        Objects.equals(this.endDate, education.endDate) &&
        Objects.equals(this.description, education.description) &&
        Objects.equals(this.createdAt, education.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, institution, degree, field, startDate, endDate, description, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Education {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    institution: ").append(toIndentedString(institution)).append("\n");
    sb.append("    degree: ").append(toIndentedString(degree)).append("\n");
    sb.append("    field: ").append(toIndentedString(field)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
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

