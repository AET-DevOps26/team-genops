package com.jobready.application.generated.modelDto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.jobready.application.generated.modelDto.GeneratedDocumentType;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * CreateGeneratedDocumentRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class CreateGeneratedDocumentRequest {

  private @Nullable UUID applicationId = null;

  private GeneratedDocumentType type;

  private String content;

  public CreateGeneratedDocumentRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateGeneratedDocumentRequest(GeneratedDocumentType type, String content) {
    this.type = type;
    this.content = content;
  }

  public CreateGeneratedDocumentRequest applicationId(@Nullable UUID applicationId) {
    this.applicationId = applicationId;
    return this;
  }

  /**
   * Omit to save a standalone document not tied to any application.
   * @return applicationId
   */
  @Valid 
  @Schema(name = "application_id", description = "Omit to save a standalone document not tied to any application.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("application_id")
  public @Nullable UUID getApplicationId() {
    return applicationId;
  }

  @JsonProperty("application_id")
  public void setApplicationId(@Nullable UUID applicationId) {
    this.applicationId = applicationId;
  }

  public CreateGeneratedDocumentRequest type(GeneratedDocumentType type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
   */
  @NotNull @Valid 
  @Schema(name = "type", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("type")
  public GeneratedDocumentType getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(GeneratedDocumentType type) {
    this.type = type;
  }

  public CreateGeneratedDocumentRequest content(String content) {
    this.content = content;
    return this;
  }

  /**
   * Get content
   * @return content
   */
  @NotNull @Size(min = 1) 
  @Schema(name = "content", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("content")
  public String getContent() {
    return content;
  }

  @JsonProperty("content")
  public void setContent(String content) {
    this.content = content;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateGeneratedDocumentRequest createGeneratedDocumentRequest = (CreateGeneratedDocumentRequest) o;
    return Objects.equals(this.applicationId, createGeneratedDocumentRequest.applicationId) &&
        Objects.equals(this.type, createGeneratedDocumentRequest.type) &&
        Objects.equals(this.content, createGeneratedDocumentRequest.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(applicationId, type, content);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateGeneratedDocumentRequest {\n");
    sb.append("    applicationId: ").append(toIndentedString(applicationId)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    content: ").append(toIndentedString(content)).append("\n");
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

