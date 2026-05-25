package com.jobready.auth.controller;

import com.jobready.auth.generated.api.AuthApi;
import com.jobready.auth.generated.modelDto.RegisterRequest;
import com.jobready.auth.generated.modelDto.TokenResponse;
import com.jobready.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    public ResponseEntity<TokenResponse> register(RegisterRequest request) {
        return ResponseEntity.status(201).body(authService.register(request));
    }
}
