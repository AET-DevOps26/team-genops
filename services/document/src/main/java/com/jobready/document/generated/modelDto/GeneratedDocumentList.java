package com.jobready.document.generated.modelDto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.jobready.document.generated.modelDto.GeneratedDocument;
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
 * GeneratedDocumentList
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class GeneratedDocumentList {

  private List<@Valid GeneratedDocument> items = new ArrayList<>();

  public GeneratedDocumentList() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GeneratedDocumentList(List<@Valid GeneratedDocument> items) {
    this.items = items;
  }

  public GeneratedDocumentList items(List<@Valid GeneratedDocument> items) {
    this.items = items;
    return this;
  }

  public GeneratedDocumentList addItemsItem(GeneratedDocument itemsItem) {
    if (this.items == null) {
      this.items = new ArrayList<>();
    }
    this.items.add(itemsItem);
    return this;
  }

  /**
   * Get items
   * @return items
   */
  @NotNull @Valid 
  @Schema(name = "items", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("items")
  public List<@Valid GeneratedDocument> getItems() {
    return items;
  }

  @JsonProperty("items")
  public void setItems(List<@Valid GeneratedDocument> items) {
    this.items = items;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GeneratedDocumentList generatedDocumentList = (GeneratedDocumentList) o;
    return Objects.equals(this.items, generatedDocumentList.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GeneratedDocumentList {\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
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

