package com.jobready.email.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * This service only <strong>verifies</strong> access tokens — it never signs them. It fetches the
 * auth service's public key from its JWK Set endpoint and validates RS256 signatures locally
 * (defense in depth: every service re-verifies the JWT). There is deliberately no encoder or
 * private key here.
 */
@Configuration
public class JwtConfig {

    @Bean
    public JwtDecoder jwtDecoder(@Value("${auth.jwks-url}") String jwksUrl) {
        return NimbusJwtDecoder.withJwkSetUri(jwksUrl).build();
    }
}
