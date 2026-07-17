package com.jobready.email.modelEntity;

import com.jobready.email.crypto.TokenEncryptor;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * Transparently encrypts/decrypts token columns via {@link TokenEncryptor}. Spring Boot wires
 * Hibernate to its bean container, so this converter is a Spring bean and can inject the
 * encryptor (which carries the key from configuration).
 */
@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final TokenEncryptor encryptor;

    public EncryptedStringConverter(TokenEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute == null ? null : encryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData == null ? null : encryptor.decrypt(dbData);
    }
}
