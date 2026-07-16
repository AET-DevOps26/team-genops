package com.jobready.document.generated.modelDto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.jobready.document.generated.modelDto.GeneratedDocumentType;
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
 * GeneratedDocument
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class GeneratedDocument {

  private UUID id;

  private @Nullable UUID applicationId = null;

  private GeneratedDocumentType type;

  private String content;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  public GeneratedDocument() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GeneratedDocument(UUID id, GeneratedDocumentType type, String content, OffsetDateTime createdAt) {
    this.id = id;
    this.type = type;
    this.content = content;
    this.createdAt = createdAt;
  }

  public GeneratedDocument id(UUID id) {
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

  public GeneratedDocument applicationId(@Nullable UUID applicationId) {
    this.applicationId = applicationId;
    return this;
  }

  /**
   * The application this document was tailored for (no cross-service FK), or null for a standalone document — a user may polish a general resume without a target job. A document is owned by its user; being filed under an application is optional.
   * @return applicationId
   */
  @Valid 
  @Schema(name = "application_id", description = "The application this document was tailored for (no cross-service FK), or null for a standalone document — a user may polish a general resume without a target job. A document is owned by its user; being filed under an application is optional.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("application_id")
  public @Nullable UUID getApplicationId() {
    return applicationId;
  }

  @JsonProperty("application_id")
  public void setApplicationId(@Nullable UUID applicationId) {
    this.applicationId = applicationId;
  }

  public GeneratedDocument type(GeneratedDocumentType type) {
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

  public GeneratedDocument content(String content) {
    this.content = content;
    return this;
  }

  /**
   * Get content
   * @return content
   */
  @NotNull 
  @Schema(name = "content", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("content")
  public String getContent() {
    return content;
  }

  @JsonProperty("content")
  public void setContent(String content) {
    this.content = content;
  }

  public GeneratedDocument createdAt(OffsetDateTime createdAt) {
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
    GeneratedDocument generatedDocument = (GeneratedDocument) o;
    return Objects.equals(this.id, generatedDocument.id) &&
        Objects.equals(this.applicationId, generatedDocument.applicationId) &&
        Objects.equals(this.type, generatedDocument.type) &&
        Objects.equals(this.content, generatedDocument.content) &&
        Objects.equals(this.createdAt, generatedDocument.createdAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, applicationId, type, content, createdAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GeneratedDocument {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    applicationId: ").append(toIndentedString(applicationId)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    content: ").append(toIndentedString(content)).append("\n");
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

