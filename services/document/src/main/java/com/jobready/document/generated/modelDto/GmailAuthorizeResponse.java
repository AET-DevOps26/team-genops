package com.jobready.document.generated.modelDto;

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
 * GmailAuthorizeResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class GmailAuthorizeResponse {

  private URI authorizationUrl;

  public GmailAuthorizeResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GmailAuthorizeResponse(URI authorizationUrl) {
    this.authorizationUrl = authorizationUrl;
  }

  public GmailAuthorizeResponse authorizationUrl(URI authorizationUrl) {
    this.authorizationUrl = authorizationUrl;
    return this;
  }

  /**
   * Google consent URL the client should redirect the user to
   * @return authorizationUrl
   */
  @NotNull @Valid 
  @Schema(name = "authorization_url", example = "https://accounts.google.com/o/oauth2/v2/auth?client_id=...&state=...", description = "Google consent URL the client should redirect the user to", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("authorization_url")
  public URI getAuthorizationUrl() {
    return authorizationUrl;
  }

  @JsonProperty("authorization_url")
  public void setAuthorizationUrl(URI authorizationUrl) {
    this.authorizationUrl = authorizationUrl;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GmailAuthorizeResponse gmailAuthorizeResponse = (GmailAuthorizeResponse) o;
    return Objects.equals(this.authorizationUrl, gmailAuthorizeResponse.authorizationUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authorizationUrl);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GmailAuthorizeResponse {\n");
    sb.append("    authorizationUrl: ").append(toIndentedString(authorizationUrl)).append("\n");
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

