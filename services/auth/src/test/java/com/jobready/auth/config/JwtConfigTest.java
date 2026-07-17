package com.jobready.auth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobready.auth.TestJwtKeys;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;

class JwtConfigTest {

    private JwtProperties propsWith(String privateKey, String publicKey) {
        JwtProperties props = new JwtProperties();
        props.setPrivateKey(privateKey);
        props.setPublicKey(publicKey);
        props.setKeyId("test-key");
        return props;
    }

    @Test
    void blankKeysFailStartupWithActionableMessage() {
        JwtConfig config = new JwtConfig(propsWith("", ""));
        IllegalStateException ex = assertThrows(IllegalStateException.class, config::rsaKey);
        assertTrue(ex.getMessage().contains("JWT_PRIVATE_KEY"));
        assertTrue(ex.getMessage().contains("JWT_PUBLIC_KEY"));
        assertTrue(ex.getMessage().contains("gen-jwt-keys.sh"));
    }

    @Test
    void missingPrivateKeyAloneFailsStartup() {
        JwtConfig config = new JwtConfig(propsWith(null, TestJwtKeys.publicKeyPem()));
        assertThrows(IllegalStateException.class, config::rsaKey);
    }

    @Test
    void missingPublicKeyAloneFailsStartup() {
        JwtConfig config = new JwtConfig(propsWith(TestJwtKeys.privateKeyPem(), null));
        assertThrows(IllegalStateException.class, config::rsaKey);
    }

    @Test
    void explicitPemKeysLoad() throws Exception {
        JwtConfig config = new JwtConfig(propsWith(TestJwtKeys.privateKeyPem(), TestJwtKeys.publicKeyPem()));
        RSAKey key = config.rsaKey();
        assertNotNull(key.toRSAPublicKey());
        assertEquals("test-key", key.getKeyID());
    }

    @Test
    void envStyleEscapedNewlinesLoad() throws Exception {
        // .env files carry the PEM as one line with literal \n — the loader unescapes.
        String privateEscaped = TestJwtKeys.privateKeyPem().replace("\n", "\\n");
        String publicEscaped = TestJwtKeys.publicKeyPem().replace("\n", "\\n");
        RSAKey key = new JwtConfig(propsWith(privateEscaped, publicEscaped)).rsaKey();
        assertNotNull(key.toRSAPublicKey());
    }
}
