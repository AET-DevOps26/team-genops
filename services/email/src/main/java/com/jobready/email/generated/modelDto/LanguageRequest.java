package com.jobready.email.generated.modelDto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.jobready.email.generated.modelDto.LanguageProficiency;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * LanguageRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class LanguageRequest {

  private String name;

  private LanguageProficiency proficiency;

  public LanguageRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public LanguageRequest(String name, LanguageProficiency proficiency) {
    this.name = name;
    this.proficiency = proficiency;
  }

  public LanguageRequest name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull @Size(min = 1, max = 100) 
  @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public LanguageRequest proficiency(LanguageProficiency proficiency) {
    this.proficiency = proficiency;
    return this;
  }

  /**
   * Get proficiency
   * @return proficiency
   */
  @NotNull @Valid 
  @Schema(name = "proficiency", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("proficiency")
  public LanguageProficiency getProficiency() {
    return proficiency;
  }

  @JsonProperty("proficiency")
  public void setProficiency(LanguageProficiency proficiency) {
    this.proficiency = proficiency;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LanguageRequest languageRequest = (LanguageRequest) o;
    return Objects.equals(this.name, languageRequest.name) &&
        Objects.equals(this.proficiency, languageRequest.proficiency);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, proficiency);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LanguageRequest {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    proficiency: ").append(toIndentedString(proficiency)).append("\n");
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

