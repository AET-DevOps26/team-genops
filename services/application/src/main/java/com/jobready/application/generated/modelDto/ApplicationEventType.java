package com.jobready.application.generated.modelDto;

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
 * What kind of timeline event this is.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.22.0")
public enum ApplicationEventType {
  
  STAGE_CHANGE("stage_change"),
  
  EMAIL_RECEIVED("email_received"),
  
  INTERVIEW_SCHEDULED("interview_scheduled"),
  
  OFFER_RECEIVED("offer_received"),
  
  REJECTION("rejection"),
  
  INFO_REQUESTED("info_requested"),
  
  NOTE("note");

  private final String value;

  ApplicationEventType(String value) {
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
  public static ApplicationEventType fromValue(String value) {
    for (ApplicationEventType b : ApplicationEventType.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

