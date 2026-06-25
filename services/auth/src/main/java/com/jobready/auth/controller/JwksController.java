package com.jobready.auth.controller;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class JwksController {

    public static final String PATH = "/api/v1/auth/.well-known/jwks.json";

    private final RSAKey rsaKey;

    @GetMapping(PATH)
    public Map<String, Object> getJwks() {
        return new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
    }
}
