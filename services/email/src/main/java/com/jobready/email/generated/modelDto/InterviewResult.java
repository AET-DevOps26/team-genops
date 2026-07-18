package com.jobready.email.generated.modelDto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.jobready.email.generated.modelDto.InterviewCompetency;
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
 * Structured outcome of a finished mock interview — the score card.
 */

@Schema(name = "InterviewResult", description = "Structured outcome of a finished mock interview — the score card.")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public class InterviewResult {

  private Integer score;

  private @Nullable String verdict = null;

  private @Nullable String summary = null;

  private List<@Valid InterviewCompetency> competencies = new ArrayList<>();

  private List<String> strengths = new ArrayList<>();

  private List<String> improvements = new ArrayList<>();

  private @Nullable Boolean endedEarly;

  private @Nullable Integer questionsAnswered;

  private @Nullable Integer questionsTotal;

  public InterviewResult() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public InterviewResult(Integer score) {
    this.score = score;
  }

  public InterviewResult score(Integer score) {
    this.score = score;
    return this;
  }

  /**
   * Final score 0-100, after any early-exit penalty
   * @return score
   */
  @NotNull 
  @Schema(name = "score", description = "Final score 0-100, after any early-exit penalty", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("score")
  public Integer getScore() {
    return score;
  }

  @JsonProperty("score")
  public void setScore(Integer score) {
    this.score = score;
  }

  public InterviewResult verdict(@Nullable String verdict) {
    this.verdict = verdict;
    return this;
  }

  /**
   * One short phrase, e.g. \"Strong hire signal\"
   * @return verdict
   */
  
  @Schema(name = "verdict", description = "One short phrase, e.g. \"Strong hire signal\"", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("verdict")
  public @Nullable String getVerdict() {
    return verdict;
  }

  @JsonProperty("verdict")
  public void setVerdict(@Nullable String verdict) {
    this.verdict = verdict;
  }

  public InterviewResult summary(@Nullable String summary) {
    this.summary = summary;
    return this;
  }

  /**
   * Get summary
   * @return summary
   */
  
  @Schema(name = "summary", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("summary")
  public @Nullable String getSummary() {
    return summary;
  }

  @JsonProperty("summary")
  public void setSummary(@Nullable String summary) {
    this.summary = summary;
  }

  public InterviewResult competencies(List<@Valid InterviewCompetency> competencies) {
    this.competencies = competencies;
    return this;
  }

  public InterviewResult addCompetenciesItem(InterviewCompetency competenciesItem) {
    if (this.competencies == null) {
      this.competencies = new ArrayList<>();
    }
    this.competencies.add(competenciesItem);
    return this;
  }

  /**
   * Get competencies
   * @return competencies
   */
  @Valid 
  @Schema(name = "competencies", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("competencies")
  public List<@Valid InterviewCompetency> getCompetencies() {
    return competencies;
  }

  @JsonProperty("competencies")
  public void setCompetencies(List<@Valid InterviewCompetency> competencies) {
    this.competencies = competencies;
  }

  public InterviewResult strengths(List<String> strengths) {
    this.strengths = strengths;
    return this;
  }

  public InterviewResult addStrengthsItem(String strengthsItem) {
    if (this.strengths == null) {
      this.strengths = new ArrayList<>();
    }
    this.strengths.add(strengthsItem);
    return this;
  }

  /**
   * Get strengths
   * @return strengths
   */
  
  @Schema(name = "strengths", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("strengths")
  public List<String> getStrengths() {
    return strengths;
  }

  @JsonProperty("strengths")
  public void setStrengths(List<String> strengths) {
    this.strengths = strengths;
  }

  public InterviewResult improvements(List<String> improvements) {
    this.improvements = improvements;
    return this;
  }

  public InterviewResult addImprovementsItem(String improvementsItem) {
    if (this.improvements == null) {
      this.improvements = new ArrayList<>();
    }
    this.improvements.add(improvementsItem);
    return this;
  }

  /**
   * Get improvements
   * @return improvements
   */
  
  @Schema(name = "improvements", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("improvements")
  public List<String> getImprovements() {
    return improvements;
  }

  @JsonProperty("improvements")
  public void setImprovements(List<String> improvements) {
    this.improvements = improvements;
  }

  public InterviewResult endedEarly(@Nullable Boolean endedEarly) {
    this.endedEarly = endedEarly;
    return this;
  }

  /**
   * True if the candidate ended before the final question (which lowers the score)
   * @return endedEarly
   */
  
  @Schema(name = "ended_early", description = "True if the candidate ended before the final question (which lowers the score)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("ended_early")
  public @Nullable Boolean getEndedEarly() {
    return endedEarly;
  }

  @JsonProperty("ended_early")
  public void setEndedEarly(@Nullable Boolean endedEarly) {
    this.endedEarly = endedEarly;
  }

  public InterviewResult questionsAnswered(@Nullable Integer questionsAnswered) {
    this.questionsAnswered = questionsAnswered;
    return this;
  }

  /**
   * Get questionsAnswered
   * @return questionsAnswered
   */
  
  @Schema(name = "questions_answered", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("questions_answered")
  public @Nullable Integer getQuestionsAnswered() {
    return questionsAnswered;
  }

  @JsonProperty("questions_answered")
  public void setQuestionsAnswered(@Nullable Integer questionsAnswered) {
    this.questionsAnswered = questionsAnswered;
  }

  public InterviewResult questionsTotal(@Nullable Integer questionsTotal) {
    this.questionsTotal = questionsTotal;
    return this;
  }

  /**
   * Get questionsTotal
   * @return questionsTotal
   */
  
  @Schema(name = "questions_total", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("questions_total")
  public @Nullable Integer getQuestionsTotal() {
    return questionsTotal;
  }

  @JsonProperty("questions_total")
  public void setQuestionsTotal(@Nullable Integer questionsTotal) {
    this.questionsTotal = questionsTotal;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InterviewResult interviewResult = (InterviewResult) o;
    return Objects.equals(this.score, interviewResult.score) &&
        Objects.equals(this.verdict, interviewResult.verdict) &&
        Objects.equals(this.summary, interviewResult.summary) &&
        Objects.equals(this.competencies, interviewResult.competencies) &&
        Objects.equals(this.strengths, interviewResult.strengths) &&
        Objects.equals(this.improvements, interviewResult.improvements) &&
        Objects.equals(this.endedEarly, interviewResult.endedEarly) &&
        Objects.equals(this.questionsAnswered, interviewResult.questionsAnswered) &&
        Objects.equals(this.questionsTotal, interviewResult.questionsTotal);
  }

  @Override
  public int hashCode() {
    return Objects.hash(score, verdict, summary, competencies, strengths, improvements, endedEarly, questionsAnswered, questionsTotal);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InterviewResult {\n");
    sb.append("    score: ").append(toIndentedString(score)).append("\n");
    sb.append("    verdict: ").append(toIndentedString(verdict)).append("\n");
    sb.append("    summary: ").append(toIndentedString(summary)).append("\n");
    sb.append("    competencies: ").append(toIndentedString(competencies)).append("\n");
    sb.append("    strengths: ").append(toIndentedString(strengths)).append("\n");
    sb.append("    improvements: ").append(toIndentedString(improvements)).append("\n");
    sb.append("    endedEarly: ").append(toIndentedString(endedEarly)).append("\n");
    sb.append("    questionsAnswered: ").append(toIndentedString(questionsAnswered)).append("\n");
    sb.append("    questionsTotal: ").append(toIndentedString(questionsTotal)).append("\n");
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

