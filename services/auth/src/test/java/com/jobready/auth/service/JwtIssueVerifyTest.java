package com.jobready.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jobready.auth.TestJwtKeys;
import com.jobready.auth.config.JwtConfig;
import com.jobready.auth.config.JwtProperties;
import com.jobready.auth.modelEntity.User;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;

/**
 * Round-trip proof that access tokens carry iss/aud and that the decoder
 * enforces them — a token missing the claims (e.g. minted by an older auth
 * build or a foreign issuer) must be rejected, not just ignored.
 */
class JwtIssueVerifyTest {

    private JwtProperties props;
    private JwtEncoder encoder;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() throws Exception {
        props = new JwtProperties();
        props.setPrivateKey(TestJwtKeys.privateKeyPem());
        props.setPublicKey(TestJwtKeys.publicKeyPem());
        props.setKeyId("test-key");
        props.setAccessTokenExpiry(900);
        props.setIssuer("https://jobready-auth");
        props.setAudience("jobready");
        JwtConfig config = new JwtConfig(props);
        var rsaKey = config.rsaKey();
        encoder = config.jwtEncoder(config.jwkSource(rsaKey));
        decoder = config.jwtDecoder(rsaKey);
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("jane@example.com");
        return user;
    }

    @Test
    void issuedAccessTokenCarriesIssAndAudAndVerifies() {
        // Redis is only used for refresh tokens; access-token issuance never touches it.
        JwtService jwtService = new JwtServiceImpl(encoder, null, props);
        User user = user();

        Jwt jwt = decoder.decode(jwtService.generateAccessToken(user));

        assertEquals("https://jobready-auth", jwt.getIssuer().toString());
        assertEquals("jobready", jwt.getAudience().getFirst());
        assertEquals(user.getId().toString(), jwt.getSubject());
    }

    @Test
    void tokenWithoutIssAndAudIsRejected() {
        Instant now = Instant.now();
        JwtClaimsSet bare = JwtClaimsSet.builder()
                .subject(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(bare)).getTokenValue();

        assertThrows(JwtValidationException.class, () -> decoder.decode(token));
    }

    @Test
    void tokenWithWrongIssuerOrAudienceIsRejected() {
        Instant now = Instant.now();
        JwtClaimsSet foreign = JwtClaimsSet.builder()
                .subject(UUID.randomUUID().toString())
                .issuer("some-other-system")
                .audience(java.util.List.of("their-api"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(foreign)).getTokenValue();

        assertThrows(JwtValidationException.class, () -> decoder.decode(token));
    }
}
