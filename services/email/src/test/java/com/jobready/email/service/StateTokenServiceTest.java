package com.jobready.email.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jobready.email.exception.InvalidStateException;
import com.jobready.email.support.TestProperties;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StateTokenServiceTest {

    private final NonceStore nonceStore = new NonceStore();
    private final StateTokenService service = new StateTokenService(TestProperties.emailProperties(), nonceStore);

    @Test
    void roundTripsUserId() {
        UUID userId = UUID.randomUUID();
        StateTokenService.StateClaims claims = service.validate(service.issue(userId));
        assertThat(claims.userId()).isEqualTo(userId);
        assertThat(claims.nonce()).isNotBlank();
        assertThat(claims.expiresAt()).isAfter(Instant.now());
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        StateTokenService other =
                new StateTokenService(TestProperties.emailProperties("attacker-key"), new NonceStore());
        String forged = other.issue(UUID.randomUUID());
        assertThatThrownBy(() -> service.validate(forged)).isInstanceOf(InvalidStateException.class);
    }

    @Test
    void rejectsGarbageToken() {
        assertThatThrownBy(() -> service.validate("not-a-jwt")).isInstanceOf(InvalidStateException.class);
    }

    @Test
    void validateDoesNotConsume_butReservationBlocksReplay() {
        String state = service.issue(UUID.randomUUID());
        StateTokenService.StateClaims claims = service.validate(state);
        // Validation alone must not burn the nonce (a failed exchange leaves it reusable).
        assertThat(service.validate(state).nonce()).isEqualTo(claims.nonce());

        // Reservation is atomic: only one concurrent callback can win it.
        assertThat(service.reserve(claims)).isTrue();
        assertThat(service.reserve(claims)).isFalse();
        assertThatThrownBy(() -> service.validate(state))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("already been used");

        // Releasing after a recoverable failure makes the state usable again.
        service.release(claims);
        assertThat(service.validate(state).nonce()).isEqualTo(claims.nonce());
    }

    @Test
    void nonceStorePrunesExpiredEntries() {
        nonceStore.reserve("stale", Instant.now().minusSeconds(5));
        assertThat(nonceStore.isConsumed("stale")).isFalse();
    }
}
