package com.jobready.auth.service;

import com.jobready.auth.generated.modelDto.LoginRequest;
import com.jobready.auth.generated.modelDto.RegisterRequest;
import com.jobready.auth.generated.modelDto.UserResponse;
import com.jobready.auth.model.IssuedSession;

import java.util.UUID;

public interface AuthService {
    IssuedSession register(RegisterRequest request);
    IssuedSession login(LoginRequest request);
    void logout(String refreshToken);
    IssuedSession refresh(String refreshToken);
    UserResponse getMe(UUID userId);
}
