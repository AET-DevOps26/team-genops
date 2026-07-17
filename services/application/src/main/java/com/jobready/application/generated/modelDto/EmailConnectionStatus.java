package com.jobready.application.generated.modelDto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * EmailConnectionStatus
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class EmailConnectionStatus {

  private Boolean connected;

  /**
   * Present only when connected
   */
  public enum ProviderEnum {
    GMAIL("gmail");

    private final String value;

    ProviderEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static ProviderEnum fromValue(String value) {
      for (ProviderEnum b : ProviderEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private @Nullable ProviderEnum provider;

  private @Nullable String emailAddress;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime connectedAt;

  public EmailConnectionStatus() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public EmailConnectionStatus(Boolean connected) {
    this.connected = connected;
  }

  public EmailConnectionStatus connected(Boolean connected) {
    this.connected = connected;
    return this;
  }

  /**
   * Whether the user has a stored email connection
   * @return connected
   */
  @NotNull 
  @Schema(name = "connected", description = "Whether the user has a stored email connection", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("connected")
  public Boolean getConnected() {
    return connected;
  }

  @JsonProperty("connected")
  public void setConnected(Boolean connected) {
    this.connected = connected;
  }

  public EmailConnectionStatus provider(@Nullable ProviderEnum provider) {
    this.provider = provider;
    return this;
  }

  /**
   * Present only when connected
   * @return provider
   */
  
  @Schema(name = "provider", description = "Present only when connected", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("provider")
  public @Nullable ProviderEnum getProvider() {
    return provider;
  }

  @JsonProperty("provider")
  public void setProvider(@Nullable ProviderEnum provider) {
    this.provider = provider;
  }

  public EmailConnectionStatus emailAddress(@Nullable String emailAddress) {
    this.emailAddress = emailAddress;
    return this;
  }

  /**
   * The connected mailbox address — present only when connected
   * @return emailAddress
   */
  @jakarta.validation.constraints.Email 
  @Schema(name = "email_address", description = "The connected mailbox address — present only when connected", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("email_address")
  public @Nullable String getEmailAddress() {
    return emailAddress;
  }

  @JsonProperty("email_address")
  public void setEmailAddress(@Nullable String emailAddress) {
    this.emailAddress = emailAddress;
  }

  public EmailConnectionStatus connectedAt(@Nullable OffsetDateTime connectedAt) {
    this.connectedAt = connectedAt;
    return this;
  }

  /**
   * When the connection was first established — present only when connected
   * @return connectedAt
   */
  @Valid 
  @Schema(name = "connected_at", description = "When the connection was first established — present only when connected", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connected_at")
  public @Nullable OffsetDateTime getConnectedAt() {
    return connectedAt;
  }

  @JsonProperty("connected_at")
  public void setConnectedAt(@Nullable OffsetDateTime connectedAt) {
    this.connectedAt = connectedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EmailConnectionStatus emailConnectionStatus = (EmailConnectionStatus) o;
    return Objects.equals(this.connected, emailConnectionStatus.connected) &&
        Objects.equals(this.provider, emailConnectionStatus.provider) &&
        Objects.equals(this.emailAddress, emailConnectionStatus.emailAddress) &&
        Objects.equals(this.connectedAt, emailConnectionStatus.connectedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connected, provider, emailAddress, connectedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EmailConnectionStatus {\n");
    sb.append("    connected: ").append(toIndentedString(connected)).append("\n");
    sb.append("    provider: ").append(toIndentedString(provider)).append("\n");
    sb.append("    emailAddress: ").append(toIndentedString(emailAddress)).append("\n");
    sb.append("    connectedAt: ").append(toIndentedString(connectedAt)).append("\n");
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

