package com.jobready.auth;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Generates an ephemeral RSA keypair for tests, PEM-encoded to match the
 * jwt.private-key / jwt.public-key properties. Production code has no key
 * fallback (JwtConfig fails fast on blank keys); ephemeral keys are only
 * valid in test scope, which is why this generator lives here.
 */
public final class TestJwtKeys {

    private static final KeyPair KEY_PAIR = generate();

    private TestJwtKeys() {}

    public static String privateKeyPem() {
        return pem("PRIVATE KEY", KEY_PAIR.getPrivate().getEncoded());
    }

    public static String publicKeyPem() {
        return pem("PUBLIC KEY", KEY_PAIR.getPublic().getEncoded());
    }

    private static KeyPair generate() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return gen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM without RSA support", e);
        }
    }

    private static String pem(String label, byte[] der) {
        return "-----BEGIN " + label + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der)
                + "\n-----END " + label + "-----\n";
    }
}
