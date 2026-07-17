package com.jobready.document.generated.modelDto;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Application lifecycle stage: `applied` → `follow_up` → `interview` → `offer` → `closed`. 
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.23.0")
public enum ApplicationStage {
  
  APPLIED("applied"),
  
  FOLLOW_UP("follow_up"),
  
  INTERVIEW("interview"),
  
  OFFER("offer"),
  
  CLOSED("closed");

  private final String value;

  ApplicationStage(String value) {
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
  public static ApplicationStage fromValue(String value) {
    for (ApplicationStage b : ApplicationStage.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

