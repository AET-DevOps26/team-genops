package com.jobready.email.service;

import com.jobready.email.config.EmailProperties;
import com.jobready.email.exception.InvalidStateException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Signed, single-use OAuth {@code state} tokens.
 *
 * <p>The Gmail callback is unauthenticated (the browser, not a service, calls it), so the state
 * is the only thing binding the flow to a user. It is a short-lived HS256 JWT over the user id
 * plus a random nonce, signed with {@code STATE_SIGNING_KEY}. The callback verifies signature +
 * expiry; the nonce is consumed exactly once (via {@link NonceStore}) only after the flow has
 * actually succeeded, so a transient failure leaves the link reusable.
 */
@Service
public class StateTokenService {

    /** Validated state claims; {@code exp} is needed to expire the consumed-nonce record. */
    public record StateClaims(UUID userId, String nonce, Instant expiresAt) {}

    private final byte[] signingKey;
    private final long ttlSeconds;
    private final NonceStore nonceStore;

    public StateTokenService(EmailProperties properties, NonceStore nonceStore) {
        String key = properties.state().signingKey();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("email.state.signing-key (STATE_SIGNING_KEY) must be set");
        }
        // HS256 requires >= 256-bit keys; hash the configured secret so any length works.
        this.signingKey = sha256(key);
        this.ttlSeconds = properties.state().ttlSeconds();
        this.nonceStore = nonceStore;
    }

    public String issue(UUID userId) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(userId.toString())
                    .claim("nonce", UUID.randomUUID().toString().replace("-", ""))
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(ttlSeconds)))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(signingKey));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Unable to sign OAuth state token", e);
        }
    }

    /**
     * Validate a state token <strong>without consuming it</strong>. Throws
     * {@link InvalidStateException} on a forged, expired, malformed, or already-replayed token.
     * The caller consumes the nonce (via {@link #consume}) once the flow has succeeded.
     */
    public StateClaims validate(String state) {
        JWTClaimsSet claims;
        try {
            SignedJWT jwt = SignedJWT.parse(state);
            if (!jwt.verify(new MACVerifier(signingKey))) {
                throw new InvalidStateException("Invalid or expired OAuth state");
            }
            claims = jwt.getJWTClaimsSet();
        } catch (InvalidStateException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidStateException("Invalid or expired OAuth state", e);
        }

        Date exp = claims.getExpirationTime();
        if (exp == null || exp.toInstant().isBefore(Instant.now())) {
            throw new InvalidStateException("Invalid or expired OAuth state");
        }
        String sub = claims.getSubject();
        String nonce = getNonce(claims);
        if (sub == null || sub.isBlank() || nonce == null || nonce.isBlank()) {
            throw new InvalidStateException("Malformed OAuth state");
        }
        if (nonceStore.isConsumed(nonce)) {
            throw new InvalidStateException("OAuth state has already been used");
        }
        UUID userId;
        try {
            userId = UUID.fromString(sub);
        } catch (IllegalArgumentException e) {
            throw new InvalidStateException("Malformed OAuth state", e);
        }
        return new StateClaims(userId, nonce, exp.toInstant());
    }

    /** Mark a validated nonce as used so it can never be replayed. */
    public void consume(StateClaims claims) {
        nonceStore.consume(claims.nonce(), claims.expiresAt());
    }

    private static String getNonce(JWTClaimsSet claims) {
        Object nonce = claims.getClaim("nonce");
        return nonce == null ? null : nonce.toString();
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
