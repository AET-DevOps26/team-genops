package com.jobready.auth.generated.modelDto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ProfileRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class ProfileRequest {

  private String firstName;

  private String lastName;

  private @Nullable String bio;

  private @Nullable String location;

  private @Nullable String phone;

  private @Nullable String website;

  public ProfileRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProfileRequest(String firstName, String lastName) {
    this.firstName = firstName;
    this.lastName = lastName;
  }

  public ProfileRequest firstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  /**
   * Get firstName
   * @return firstName
   */
  @NotNull @Size(min = 1, max = 100) 
  @Schema(name = "first_name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("first_name")
  public String getFirstName() {
    return firstName;
  }

  @JsonProperty("first_name")
  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public ProfileRequest lastName(String lastName) {
    this.lastName = lastName;
    return this;
  }

  /**
   * Get lastName
   * @return lastName
   */
  @NotNull @Size(min = 1, max = 100) 
  @Schema(name = "last_name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("last_name")
  public String getLastName() {
    return lastName;
  }

  @JsonProperty("last_name")
  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public ProfileRequest bio(@Nullable String bio) {
    this.bio = bio;
    return this;
  }

  /**
   * Get bio
   * @return bio
   */
  
  @Schema(name = "bio", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("bio")
  public @Nullable String getBio() {
    return bio;
  }

  @JsonProperty("bio")
  public void setBio(@Nullable String bio) {
    this.bio = bio;
  }

  public ProfileRequest location(@Nullable String location) {
    this.location = location;
    return this;
  }

  /**
   * Get location
   * @return location
   */
  @Size(max = 255) 
  @Schema(name = "location", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("location")
  public @Nullable String getLocation() {
    return location;
  }

  @JsonProperty("location")
  public void setLocation(@Nullable String location) {
    this.location = location;
  }

  public ProfileRequest phone(@Nullable String phone) {
    this.phone = phone;
    return this;
  }

  /**
   * Get phone
   * @return phone
   */
  @Size(max = 50) 
  @Schema(name = "phone", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("phone")
  public @Nullable String getPhone() {
    return phone;
  }

  @JsonProperty("phone")
  public void setPhone(@Nullable String phone) {
    this.phone = phone;
  }

  public ProfileRequest website(@Nullable String website) {
    this.website = website;
    return this;
  }

  /**
   * Get website
   * @return website
   */
  @Size(max = 255) 
  @Schema(name = "website", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("website")
  public @Nullable String getWebsite() {
    return website;
  }

  @JsonProperty("website")
  public void setWebsite(@Nullable String website) {
    this.website = website;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProfileRequest profileRequest = (ProfileRequest) o;
    return Objects.equals(this.firstName, profileRequest.firstName) &&
        Objects.equals(this.lastName, profileRequest.lastName) &&
        Objects.equals(this.bio, profileRequest.bio) &&
        Objects.equals(this.location, profileRequest.location) &&
        Objects.equals(this.phone, profileRequest.phone) &&
        Objects.equals(this.website, profileRequest.website);
  }

  @Override
  public int hashCode() {
    return Objects.hash(firstName, lastName, bio, location, phone, website);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProfileRequest {\n");
    sb.append("    firstName: ").append(toIndentedString(firstName)).append("\n");
    sb.append("    lastName: ").append(toIndentedString(lastName)).append("\n");
    sb.append("    bio: ").append(toIndentedString(bio)).append("\n");
    sb.append("    location: ").append(toIndentedString(location)).append("\n");
    sb.append("    phone: ").append(toIndentedString(phone)).append("\n");
    sb.append("    website: ").append(toIndentedString(website)).append("\n");
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

