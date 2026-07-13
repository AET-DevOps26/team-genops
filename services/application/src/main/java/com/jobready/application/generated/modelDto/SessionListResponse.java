package com.jobready.application.generated.modelDto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.jobready.application.generated.modelDto.SessionResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * SessionListResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class SessionListResponse {

  private List<@Valid SessionResponse> sessions = new ArrayList<>();

  public SessionListResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SessionListResponse(List<@Valid SessionResponse> sessions) {
    this.sessions = sessions;
  }

  public SessionListResponse sessions(List<@Valid SessionResponse> sessions) {
    this.sessions = sessions;
    return this;
  }

  public SessionListResponse addSessionsItem(SessionResponse sessionsItem) {
    if (this.sessions == null) {
      this.sessions = new ArrayList<>();
    }
    this.sessions.add(sessionsItem);
    return this;
  }

  /**
   * Get sessions
   * @return sessions
   */
  @NotNull @Valid 
  @Schema(name = "sessions", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("sessions")
  public List<@Valid SessionResponse> getSessions() {
    return sessions;
  }

  @JsonProperty("sessions")
  public void setSessions(List<@Valid SessionResponse> sessions) {
    this.sessions = sessions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SessionListResponse sessionListResponse = (SessionListResponse) o;
    return Objects.equals(this.sessions, sessionListResponse.sessions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sessions);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SessionListResponse {\n");
    sb.append("    sessions: ").append(toIndentedString(sessions)).append("\n");
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

