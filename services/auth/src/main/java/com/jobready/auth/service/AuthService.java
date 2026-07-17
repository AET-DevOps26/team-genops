package com.jobready.auth.service;

import com.jobready.auth.generated.modelDto.LoginRequest;
import com.jobready.auth.generated.modelDto.RegisterRequest;
import com.jobready.auth.generated.modelDto.UserResponse;
import com.jobready.auth.model.IssuedSession;
import java.util.UUID;

public interface AuthService {
    /** {@code clientIp} feeds the register throttle; resolved from XFF at the controller. */
    IssuedSession register(RegisterRequest request, String clientIp);

    /** {@code clientIp} feeds the login lockout; resolved from XFF at the controller. */
    IssuedSession login(LoginRequest request, String clientIp);

    void logout(String refreshToken);

    IssuedSession refresh(String refreshToken);

    UserResponse getMe(UUID userId);
}
