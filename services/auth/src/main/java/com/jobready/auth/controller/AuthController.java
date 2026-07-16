package com.jobready.auth.controller;

import com.jobready.auth.config.CookieProperties;
import com.jobready.auth.exception.InvalidCredentialsException;
import com.jobready.auth.generated.api.AuthApi;
import com.jobready.auth.generated.modelDto.LoginRequest;
import com.jobready.auth.generated.modelDto.RegisterRequest;
import com.jobready.auth.generated.modelDto.UserResponse;
import com.jobready.auth.model.IssuedSession;
import com.jobready.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {
    private final AuthService authService;
    private final HttpServletRequest request;
    private final CookieProperties cookieProperties;

    @Override
    public ResponseEntity<UserResponse> register(RegisterRequest registerRequest) {
        IssuedSession session = authService.register(registerRequest);
        return ResponseEntity.status(201).headers(sessionCookies(session)).body(session.user());
    }

    @Override
    public ResponseEntity<UserResponse> login(LoginRequest loginRequest) {
        IssuedSession session = authService.login(loginRequest);
        return ResponseEntity.ok().headers(sessionCookies(session)).body(session.user());
    }

    @Override
    public ResponseEntity<Void> refreshToken() {
        IssuedSession session = authService.refresh(requireCookie(cookieProperties.getRefreshName()));
        return ResponseEntity.noContent().headers(sessionCookies(session)).build();
    }

    @Override
    public ResponseEntity<Void> logout() {
        String refreshToken = readCookie(cookieProperties.getRefreshName());
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        return ResponseEntity.noContent().headers(clearedCookies()).build();
    }

    @Override
    public ResponseEntity<UserResponse> getMe() {
        JwtAuthenticationToken auth =
                (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString(auth.getToken().getSubject());
        return ResponseEntity.ok(authService.getMe(userId));
    }

    private HttpHeaders sessionCookies(IssuedSession session) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(
                HttpHeaders.SET_COOKIE,
                cookie(
                                cookieProperties.getAccessName(),
                                session.accessToken(),
                                cookieProperties.getAccessPath(),
                                session.accessMaxAge())
                        .toString());
        headers.add(
                HttpHeaders.SET_COOKIE,
                cookie(
                                cookieProperties.getRefreshName(),
                                session.refreshToken(),
                                cookieProperties.getRefreshPath(),
                                session.refreshMaxAge())
                        .toString());
        return headers;
    }

    private HttpHeaders clearedCookies() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(
                HttpHeaders.SET_COOKIE,
                cookie(cookieProperties.getAccessName(), "", cookieProperties.getAccessPath(), Duration.ZERO)
                        .toString());
        headers.add(
                HttpHeaders.SET_COOKIE,
                cookie(cookieProperties.getRefreshName(), "", cookieProperties.getRefreshPath(), Duration.ZERO)
                        .toString());
        return headers;
    }

    private ResponseCookie cookie(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path(path)
                .maxAge(maxAge)
                .build();
    }

    private String readCookie(String name) {
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (name.equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }

    private String requireCookie(String name) {
        String value = readCookie(name);
        if (value == null) {
            throw new InvalidCredentialsException();
        }
        return value;
    }
}
