package com.jobready.email.modelEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One connected mailbox per user; OAuth tokens are encrypted at rest via the converter. */
@Entity
@Table(name = "email_connections")
@Getter
@Setter
@NoArgsConstructor
public class EmailConnectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false)
    private String provider;

    @Column(name = "email_address", nullable = false)
    private String emailAddress;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "access_token", nullable = false)
    private String accessToken;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "refresh_token", nullable = false)
    private String refreshToken;

    @Column(name = "token_expiry", nullable = false)
    private Instant tokenExpiry;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
