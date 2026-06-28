package com.jobready.auth.model;

import com.jobready.auth.generated.modelDto.UserResponse;

import java.time.Duration;

/**
 * Internal carrier from the service layer to the controller.
 * Tokens are written into HttpOnly cookies by the controller,
 * and only {@link #user()} is serialized into the response body.
 */
public record IssuedSession(
    String accessToken,
    String refreshToken,
    Duration accessMaxAge,
    Duration refreshMaxAge,
    UserResponse user
) {}
