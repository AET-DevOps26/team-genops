package com.jobready.document.generated.modelDto;

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
 * EducationRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class EducationRequest {

  private String institution;

  private String degree;

  private @Nullable String field;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate startDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate endDate;

  private @Nullable String description;

  public EducationRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public EducationRequest(String institution, String degree, LocalDate startDate) {
    this.institution = institution;
    this.degree = degree;
    this.startDate = startDate;
  }

  public EducationRequest institution(String institution) {
    this.institution = institution;
    return this;
  }

  /**
   * Get institution
   * @return institution
   */
  @NotNull @Size(min = 1, max = 255) 
  @Schema(name = "institution", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("institution")
  public String getInstitution() {
    return institution;
  }

  @JsonProperty("institution")
  public void setInstitution(String institution) {
    this.institution = institution;
  }

  public EducationRequest degree(String degree) {
    this.degree = degree;
    return this;
  }

  /**
   * Get degree
   * @return degree
   */
  @NotNull @Size(min = 1, max = 255) 
  @Schema(name = "degree", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("degree")
  public String getDegree() {
    return degree;
  }

  @JsonProperty("degree")
  public void setDegree(String degree) {
    this.degree = degree;
  }

  public EducationRequest field(@Nullable String field) {
    this.field = field;
    return this;
  }

  /**
   * Get field
   * @return field
   */
  @Size(max = 255) 
  @Schema(name = "field", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("field")
  public @Nullable String getField() {
    return field;
  }

  @JsonProperty("field")
  public void setField(@Nullable String field) {
    this.field = field;
  }

  public EducationRequest startDate(LocalDate startDate) {
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

  public EducationRequest endDate(@Nullable LocalDate endDate) {
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

  public EducationRequest description(@Nullable String description) {
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
    EducationRequest educationRequest = (EducationRequest) o;
    return Objects.equals(this.institution, educationRequest.institution) &&
        Objects.equals(this.degree, educationRequest.degree) &&
        Objects.equals(this.field, educationRequest.field) &&
        Objects.equals(this.startDate, educationRequest.startDate) &&
        Objects.equals(this.endDate, educationRequest.endDate) &&
        Objects.equals(this.description, educationRequest.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institution, degree, field, startDate, endDate, description);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EducationRequest {\n");
    sb.append("    institution: ").append(toIndentedString(institution)).append("\n");
    sb.append("    degree: ").append(toIndentedString(degree)).append("\n");
    sb.append("    field: ").append(toIndentedString(field)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
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

