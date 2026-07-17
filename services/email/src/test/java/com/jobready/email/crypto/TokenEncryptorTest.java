package com.jobready.email.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TokenEncryptorTest {

    private final TokenEncryptor encryptor = new TokenEncryptor("test-key");

    @Test
    void roundTripsPlaintext() {
        String ciphertext = encryptor.encrypt("ya29.secret-token");
        assertThat(ciphertext).isNotEqualTo("ya29.secret-token");
        assertThat(encryptor.decrypt(ciphertext)).isEqualTo("ya29.secret-token");
    }

    @Test
    void usesAFreshIvPerEncryption() {
        assertThat(encryptor.encrypt("same")).isNotEqualTo(encryptor.encrypt("same"));
    }

    @Test
    void rejectsCiphertextFromAnotherKey() {
        String foreign = new TokenEncryptor("other-key").encrypt("secret");
        assertThatThrownBy(() -> encryptor.decrypt(foreign)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsBlankKey() {
        assertThatThrownBy(() -> new TokenEncryptor(" ")).isInstanceOf(IllegalStateException.class);
    }
}
