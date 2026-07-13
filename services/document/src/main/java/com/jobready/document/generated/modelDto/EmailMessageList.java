package com.jobready.document.generated.modelDto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.jobready.document.generated.modelDto.EmailMessage;
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
 * EmailMessageList
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class EmailMessageList {

  private List<@Valid EmailMessage> items = new ArrayList<>();

  private Integer limit;

  private Integer offset;

  public EmailMessageList() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public EmailMessageList(List<@Valid EmailMessage> items, Integer limit, Integer offset) {
    this.items = items;
    this.limit = limit;
    this.offset = offset;
  }

  public EmailMessageList items(List<@Valid EmailMessage> items) {
    this.items = items;
    return this;
  }

  public EmailMessageList addItemsItem(EmailMessage itemsItem) {
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
  @NotNull @Valid @Size(max = 100) 
  @Schema(name = "items", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("items")
  public List<@Valid EmailMessage> getItems() {
    return items;
  }

  @JsonProperty("items")
  public void setItems(List<@Valid EmailMessage> items) {
    this.items = items;
  }

  public EmailMessageList limit(Integer limit) {
    this.limit = limit;
    return this;
  }

  /**
   * Get limit
   * @return limit
   */
  @NotNull 
  @Schema(name = "limit", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("limit")
  public Integer getLimit() {
    return limit;
  }

  @JsonProperty("limit")
  public void setLimit(Integer limit) {
    this.limit = limit;
  }

  public EmailMessageList offset(Integer offset) {
    this.offset = offset;
    return this;
  }

  /**
   * Get offset
   * @return offset
   */
  @NotNull 
  @Schema(name = "offset", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("offset")
  public Integer getOffset() {
    return offset;
  }

  @JsonProperty("offset")
  public void setOffset(Integer offset) {
    this.offset = offset;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EmailMessageList emailMessageList = (EmailMessageList) o;
    return Objects.equals(this.items, emailMessageList.items) &&
        Objects.equals(this.limit, emailMessageList.limit) &&
        Objects.equals(this.offset, emailMessageList.offset);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items, limit, offset);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EmailMessageList {\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
    sb.append("    limit: ").append(toIndentedString(limit)).append("\n");
    sb.append("    offset: ").append(toIndentedString(offset)).append("\n");
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

