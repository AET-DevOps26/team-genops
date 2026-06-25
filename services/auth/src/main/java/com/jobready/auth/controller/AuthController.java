package com.jobready.auth.controller;

import com.jobready.auth.generated.api.AuthApi;
import com.jobready.auth.generated.modelDto.LoginRequest;
import com.jobready.auth.generated.modelDto.RefreshRequest;
import com.jobready.auth.generated.modelDto.RegisterRequest;
import com.jobready.auth.generated.modelDto.TokenResponse;
import com.jobready.auth.generated.modelDto.UserResponse;
import com.jobready.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    public ResponseEntity<TokenResponse> register(RegisterRequest request) {
        return ResponseEntity.status(201).body(authService.register(request));
    }

    @Override
    public ResponseEntity<TokenResponse> login(LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @Override
    public ResponseEntity<Void> logout(RefreshRequest refreshRequest) {
        authService.logout(refreshRequest.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<TokenResponse> refreshToken(RefreshRequest refreshRequest) {
        return ResponseEntity.ok(authService.refresh(refreshRequest.getRefreshToken()));
    }

    @Override
    public ResponseEntity<UserResponse> getMe() {
        JwtAuthenticationToken auth = (JwtAuthenticationToken)
            SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString(auth.getToken().getSubject());
        return ResponseEntity.ok(authService.getMe(userId));
    }
}
