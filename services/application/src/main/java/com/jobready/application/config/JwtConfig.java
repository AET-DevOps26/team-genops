package com.jobready.application.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * This service only <strong>verifies</strong> access tokens — it never signs them. It fetches the
 * auth service's public key from its JWK Set endpoint and validates RS256 signatures locally
 * (defense in depth: every service re-verifies the JWT). There is deliberately no encoder or
 * private key here.
 *
 * <p>Beyond signature + expiry, tokens must carry the agreed {@code iss}/{@code aud} claims —
 * values must match what the auth service stamps (see auth's application.properties).
 */
@Configuration
public class JwtConfig {

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${auth.jwks-url}") String jwksUrl,
            @Value("${auth.jwt.issuer:https://jobready-auth}") String issuer,
            @Value("${auth.jwt.audience:jobready}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUrl).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD, aud -> aud != null && aud.contains(audience))));
        return decoder;
    }
}
