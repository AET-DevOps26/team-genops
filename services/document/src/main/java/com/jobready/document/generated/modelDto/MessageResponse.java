package com.jobready.document.generated.modelDto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.jobready.document.generated.modelDto.InterviewResult;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * MessageResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class MessageResponse {

  private String response;

  private @Nullable InterviewResult interview = null;

  public MessageResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public MessageResponse(String response) {
    this.response = response;
  }

  public MessageResponse response(String response) {
    this.response = response;
    return this;
  }

  /**
   * Get response
   * @return response
   */
  @NotNull 
  @Schema(name = "response", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("response")
  public String getResponse() {
    return response;
  }

  @JsonProperty("response")
  public void setResponse(String response) {
    this.response = response;
  }

  public MessageResponse interview(@Nullable InterviewResult interview) {
    this.interview = interview;
    return this;
  }

  /**
   * Present only on the turn that ends a mock interview (the arc's last answer) — the final score card. Null on every other turn.
   * @return interview
   */
  @Valid 
  @Schema(name = "interview", description = "Present only on the turn that ends a mock interview (the arc's last answer) — the final score card. Null on every other turn.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("interview")
  public @Nullable InterviewResult getInterview() {
    return interview;
  }

  @JsonProperty("interview")
  public void setInterview(@Nullable InterviewResult interview) {
    this.interview = interview;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MessageResponse messageResponse = (MessageResponse) o;
    return Objects.equals(this.response, messageResponse.response) &&
        Objects.equals(this.interview, messageResponse.interview);
  }

  @Override
  public int hashCode() {
    return Objects.hash(response, interview);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MessageResponse {\n");
    sb.append("    response: ").append(toIndentedString(response)).append("\n");
    sb.append("    interview: ").append(toIndentedString(interview)).append("\n");
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

