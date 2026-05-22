package com.jobready.auth;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        return stubTokenResponse();
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        return stubTokenResponse();
    }

    private Map<String, Object> stubTokenResponse() {
        return Map.of(
            "access_token",  "stub-access-token",
            "refresh_token", "stub-refresh-token",
            "token_type",    "Bearer",
            "expires_in",    900
        );
    }
}
