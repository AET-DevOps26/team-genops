package com.jobready.auth.generated.modelDto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
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
 * Profile
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class Profile {

  private UUID id;

  private String firstName;

  private String lastName;

  private @Nullable String bio = null;

  private @Nullable String location = null;

  private @Nullable String phone = null;

  private @Nullable String website = null;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime updatedAt;

  public Profile() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Profile(UUID id, String firstName, String lastName, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    this.id = id;
    this.firstName = firstName;
    this.lastName = lastName;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Profile id(UUID id) {
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

  public Profile firstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  /**
   * Get firstName
   * @return firstName
   */
  @NotNull 
  @Schema(name = "first_name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("first_name")
  public String getFirstName() {
    return firstName;
  }

  @JsonProperty("first_name")
  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public Profile lastName(String lastName) {
    this.lastName = lastName;
    return this;
  }

  /**
   * Get lastName
   * @return lastName
   */
  @NotNull 
  @Schema(name = "last_name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("last_name")
  public String getLastName() {
    return lastName;
  }

  @JsonProperty("last_name")
  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public Profile bio(@Nullable String bio) {
    this.bio = bio;
    return this;
  }

  /**
   * Free-text professional summary
   * @return bio
   */
  
  @Schema(name = "bio", description = "Free-text professional summary", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("bio")
  public @Nullable String getBio() {
    return bio;
  }

  @JsonProperty("bio")
  public void setBio(@Nullable String bio) {
    this.bio = bio;
  }

  public Profile location(@Nullable String location) {
    this.location = location;
    return this;
  }

  /**
   * Get location
   * @return location
   */
  
  @Schema(name = "location", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("location")
  public @Nullable String getLocation() {
    return location;
  }

  @JsonProperty("location")
  public void setLocation(@Nullable String location) {
    this.location = location;
  }

  public Profile phone(@Nullable String phone) {
    this.phone = phone;
    return this;
  }

  /**
   * Get phone
   * @return phone
   */
  
  @Schema(name = "phone", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("phone")
  public @Nullable String getPhone() {
    return phone;
  }

  @JsonProperty("phone")
  public void setPhone(@Nullable String phone) {
    this.phone = phone;
  }

  public Profile website(@Nullable String website) {
    this.website = website;
    return this;
  }

  /**
   * Get website
   * @return website
   */
  
  @Schema(name = "website", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("website")
  public @Nullable String getWebsite() {
    return website;
  }

  @JsonProperty("website")
  public void setWebsite(@Nullable String website) {
    this.website = website;
  }

  public Profile createdAt(OffsetDateTime createdAt) {
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

  public Profile updatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  /**
   * Get updatedAt
   * @return updatedAt
   */
  @NotNull @Valid 
  @Schema(name = "updated_at", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("updated_at")
  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  @JsonProperty("updated_at")
  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Profile profile = (Profile) o;
    return Objects.equals(this.id, profile.id) &&
        Objects.equals(this.firstName, profile.firstName) &&
        Objects.equals(this.lastName, profile.lastName) &&
        Objects.equals(this.bio, profile.bio) &&
        Objects.equals(this.location, profile.location) &&
        Objects.equals(this.phone, profile.phone) &&
        Objects.equals(this.website, profile.website) &&
        Objects.equals(this.createdAt, profile.createdAt) &&
        Objects.equals(this.updatedAt, profile.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, firstName, lastName, bio, location, phone, website, createdAt, updatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Profile {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    firstName: ").append(toIndentedString(firstName)).append("\n");
    sb.append("    lastName: ").append(toIndentedString(lastName)).append("\n");
    sb.append("    bio: ").append(toIndentedString(bio)).append("\n");
    sb.append("    location: ").append(toIndentedString(location)).append("\n");
    sb.append("    phone: ").append(toIndentedString(phone)).append("\n");
    sb.append("    website: ").append(toIndentedString(website)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
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

