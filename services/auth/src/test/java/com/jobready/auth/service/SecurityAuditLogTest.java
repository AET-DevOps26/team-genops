package com.jobready.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Every audit event must increment exactly the counter+label the dashboards and
 * alert rules key on — log line and metric are emitted by the same method, so
 * this pins the metric half of the contract.
 */
class SecurityAuditLogTest {

    private SimpleMeterRegistry registry;
    private SecurityAuditLog audit;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        audit = new SecurityAuditLog(registry);
    }

    private double counter(String name, String... tags) {
        return registry.counter(name, tags).count();
    }

    @Test
    void loginOutcomesLandOnSeparateLabels() {
        audit.loginSucceeded("jane@example.com", "ip");
        audit.loginFailed("jane@example.com", "ip");
        audit.loginFailed("jane@example.com", "ip");
        audit.loginBlocked("jane@example.com", "ip");

        assertEquals(1, counter("auth.login.attempts", "outcome", "success"));
        assertEquals(2, counter("auth.login.attempts", "outcome", "bad_credentials"));
        assertEquals(1, counter("auth.login.attempts", "outcome", "locked_out"));
    }

    @Test
    void lockoutHasItsOwnCounter() {
        audit.lockoutTriggered("jane@example.com", "ip", 5);
        assertEquals(1, counter("auth.lockouts"));
    }

    @Test
    void registrationOutcomesAreLabelled() {
        audit.registerSucceeded("jane@example.com", "ip");
        audit.registerRejected("jane@example.com", "ip", "email_taken");
        audit.registerRejected("jane@example.com", "ip", "rate_limited");

        assertEquals(1, counter("auth.registrations", "outcome", "success"));
        assertEquals(1, counter("auth.registrations", "outcome", "email_taken"));
        assertEquals(1, counter("auth.registrations", "outcome", "rate_limited"));
    }

    @Test
    void refreshOutcomesAreLabelled() {
        audit.refreshSucceeded();
        audit.refreshRejected();

        assertEquals(1, counter("auth.token.refresh", "outcome", "success"));
        assertEquals(1, counter("auth.token.refresh", "outcome", "rejected"));
    }

    @Test
    void noCounterCarriesIdentityLabels() {
        audit.loginFailed("jane@example.com", "203.0.113.7");
        audit.registerRejected("jane@example.com", "203.0.113.7", "email_taken");
        registry.getMeters().forEach(meter -> meter.getId().getTags().forEach(tag -> {
            assertEquals(false, tag.getValue().contains("@"), "metric label leaks an email address");
            assertEquals(false, tag.getValue().contains("203.0.113.7"), "metric label leaks an IP");
        }));
    }
}
