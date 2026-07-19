package com.jobready.auth.generated.modelDto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.net.URI;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * JobPostingExtractRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class JobPostingExtractRequest {

  private URI url;

  public JobPostingExtractRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public JobPostingExtractRequest(URI url) {
    this.url = url;
  }

  public JobPostingExtractRequest url(URI url) {
    this.url = url;
    return this;
  }

  /**
   * Public http(s) URL of the job posting to extract fields from
   * @return url
   */
  @NotNull @Valid @Size(max = 2048) 
  @Schema(name = "url", description = "Public http(s) URL of the job posting to extract fields from", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("url")
  public URI getUrl() {
    return url;
  }

  @JsonProperty("url")
  public void setUrl(URI url) {
    this.url = url;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JobPostingExtractRequest jobPostingExtractRequest = (JobPostingExtractRequest) o;
    return Objects.equals(this.url, jobPostingExtractRequest.url);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class JobPostingExtractRequest {\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
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

