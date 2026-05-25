package com.jobready.auth.service;

import com.jobready.auth.generated.modelDto.RegisterRequest;
import com.jobready.auth.generated.modelDto.TokenResponse;

public interface AuthService {
    TokenResponse register(RegisterRequest request);
}
