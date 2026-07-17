package com.jobready.email.generated.modelDto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.jobready.email.generated.modelDto.Education;
import com.jobready.email.generated.modelDto.Language;
import com.jobready.email.generated.modelDto.Profile;
import com.jobready.email.generated.modelDto.Skill;
import com.jobready.email.generated.modelDto.WorkExperience;
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
 * ProfileAggregateResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class ProfileAggregateResponse {

  private Profile profile;

  private List<@Valid WorkExperience> workExperiences = new ArrayList<>();

  private List<@Valid Education> educations = new ArrayList<>();

  private List<@Valid Skill> skills = new ArrayList<>();

  private List<@Valid Language> languages = new ArrayList<>();

  public ProfileAggregateResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProfileAggregateResponse(Profile profile, List<@Valid WorkExperience> workExperiences, List<@Valid Education> educations, List<@Valid Skill> skills, List<@Valid Language> languages) {
    this.profile = profile;
    this.workExperiences = workExperiences;
    this.educations = educations;
    this.skills = skills;
    this.languages = languages;
  }

  public ProfileAggregateResponse profile(Profile profile) {
    this.profile = profile;
    return this;
  }

  /**
   * Get profile
   * @return profile
   */
  @NotNull @Valid 
  @Schema(name = "profile", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("profile")
  public Profile getProfile() {
    return profile;
  }

  @JsonProperty("profile")
  public void setProfile(Profile profile) {
    this.profile = profile;
  }

  public ProfileAggregateResponse workExperiences(List<@Valid WorkExperience> workExperiences) {
    this.workExperiences = workExperiences;
    return this;
  }

  public ProfileAggregateResponse addWorkExperiencesItem(WorkExperience workExperiencesItem) {
    if (this.workExperiences == null) {
      this.workExperiences = new ArrayList<>();
    }
    this.workExperiences.add(workExperiencesItem);
    return this;
  }

  /**
   * Get workExperiences
   * @return workExperiences
   */
  @NotNull @Valid 
  @Schema(name = "work_experiences", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("work_experiences")
  public List<@Valid WorkExperience> getWorkExperiences() {
    return workExperiences;
  }

  @JsonProperty("work_experiences")
  public void setWorkExperiences(List<@Valid WorkExperience> workExperiences) {
    this.workExperiences = workExperiences;
  }

  public ProfileAggregateResponse educations(List<@Valid Education> educations) {
    this.educations = educations;
    return this;
  }

  public ProfileAggregateResponse addEducationsItem(Education educationsItem) {
    if (this.educations == null) {
      this.educations = new ArrayList<>();
    }
    this.educations.add(educationsItem);
    return this;
  }

  /**
   * Get educations
   * @return educations
   */
  @NotNull @Valid 
  @Schema(name = "educations", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("educations")
  public List<@Valid Education> getEducations() {
    return educations;
  }

  @JsonProperty("educations")
  public void setEducations(List<@Valid Education> educations) {
    this.educations = educations;
  }

  public ProfileAggregateResponse skills(List<@Valid Skill> skills) {
    this.skills = skills;
    return this;
  }

  public ProfileAggregateResponse addSkillsItem(Skill skillsItem) {
    if (this.skills == null) {
      this.skills = new ArrayList<>();
    }
    this.skills.add(skillsItem);
    return this;
  }

  /**
   * Get skills
   * @return skills
   */
  @NotNull @Valid 
  @Schema(name = "skills", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("skills")
  public List<@Valid Skill> getSkills() {
    return skills;
  }

  @JsonProperty("skills")
  public void setSkills(List<@Valid Skill> skills) {
    this.skills = skills;
  }

  public ProfileAggregateResponse languages(List<@Valid Language> languages) {
    this.languages = languages;
    return this;
  }

  public ProfileAggregateResponse addLanguagesItem(Language languagesItem) {
    if (this.languages == null) {
      this.languages = new ArrayList<>();
    }
    this.languages.add(languagesItem);
    return this;
  }

  /**
   * Get languages
   * @return languages
   */
  @NotNull @Valid 
  @Schema(name = "languages", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("languages")
  public List<@Valid Language> getLanguages() {
    return languages;
  }

  @JsonProperty("languages")
  public void setLanguages(List<@Valid Language> languages) {
    this.languages = languages;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProfileAggregateResponse profileAggregateResponse = (ProfileAggregateResponse) o;
    return Objects.equals(this.profile, profileAggregateResponse.profile) &&
        Objects.equals(this.workExperiences, profileAggregateResponse.workExperiences) &&
        Objects.equals(this.educations, profileAggregateResponse.educations) &&
        Objects.equals(this.skills, profileAggregateResponse.skills) &&
        Objects.equals(this.languages, profileAggregateResponse.languages);
  }

  @Override
  public int hashCode() {
    return Objects.hash(profile, workExperiences, educations, skills, languages);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProfileAggregateResponse {\n");
    sb.append("    profile: ").append(toIndentedString(profile)).append("\n");
    sb.append("    workExperiences: ").append(toIndentedString(workExperiences)).append("\n");
    sb.append("    educations: ").append(toIndentedString(educations)).append("\n");
    sb.append("    skills: ").append(toIndentedString(skills)).append("\n");
    sb.append("    languages: ").append(toIndentedString(languages)).append("\n");
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

