package com.jobready.auth.service;

import com.jobready.auth.generated.modelDto.LoginRequest;
import com.jobready.auth.generated.modelDto.RegisterRequest;
import com.jobready.auth.generated.modelDto.TokenResponse;
import com.jobready.auth.generated.modelDto.UserResponse;

import java.util.UUID;

public interface AuthService {
    TokenResponse register(RegisterRequest request);
    TokenResponse login(LoginRequest request);
    void logout(String refreshToken);
    TokenResponse refresh(String refreshToken);
    UserResponse getMe(UUID userId);
}
