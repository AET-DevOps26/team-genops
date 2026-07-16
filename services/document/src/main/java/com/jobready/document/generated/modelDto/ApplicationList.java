package com.jobready.document.generated.modelDto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.jobready.document.generated.modelDto.JobApplication;
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
 * ApplicationList
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class ApplicationList {

  private List<@Valid JobApplication> items = new ArrayList<>();

  private Long total;

  public ApplicationList() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ApplicationList(List<@Valid JobApplication> items, Long total) {
    this.items = items;
    this.total = total;
  }

  public ApplicationList items(List<@Valid JobApplication> items) {
    this.items = items;
    return this;
  }

  public ApplicationList addItemsItem(JobApplication itemsItem) {
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
  public List<@Valid JobApplication> getItems() {
    return items;
  }

  @JsonProperty("items")
  public void setItems(List<@Valid JobApplication> items) {
    this.items = items;
  }

  public ApplicationList total(Long total) {
    this.total = total;
    return this;
  }

  /**
   * Total matching applications (across all pages, after stage filtering)
   * @return total
   */
  @NotNull 
  @Schema(name = "total", description = "Total matching applications (across all pages, after stage filtering)", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("total")
  public Long getTotal() {
    return total;
  }

  @JsonProperty("total")
  public void setTotal(Long total) {
    this.total = total;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApplicationList applicationList = (ApplicationList) o;
    return Objects.equals(this.items, applicationList.items) &&
        Objects.equals(this.total, applicationList.total);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items, total);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApplicationList {\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
    sb.append("    total: ").append(toIndentedString(total)).append("\n");
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

