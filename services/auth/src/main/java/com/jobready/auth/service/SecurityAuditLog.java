package com.jobready.auth.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Single seam for security-relevant events: every auth flow reports its outcome
 * here and nowhere else. Each event emits BOTH a structured log line and a
 * Prometheus counter from the same method, so the forensic record and the
 * alerting signal can never disagree.
 *
 * <p>Log lines are single-line key=value on the dedicated {@code SECURITY_AUDIT}
 * logger (greppable, routable via logback config alone). Identity (email/IP)
 * belongs in log lines ONLY — counter labels carry the bounded {@code outcome}
 * values, never email/IP/user (unbounded cardinality + PII in the metrics
 * pipeline).
 *
 * <p>INFO = normal outcome, WARN = suspicious (lockouts, replayed refresh tokens).
 */
@Component
public class SecurityAuditLog {

    private static final Logger log = LoggerFactory.getLogger("SECURITY_AUDIT");

    private static final String LOGIN_ATTEMPTS = "auth.login.attempts";
    private static final String LOCKOUTS = "auth.lockouts";
    private static final String REGISTRATIONS = "auth.registrations";
    private static final String TOKEN_REFRESH = "auth.token.refresh";

    private final MeterRegistry registry;

    public SecurityAuditLog(MeterRegistry registry) {
        this.registry = registry;
    }

    public void loginSucceeded(String email, String ip) {
        log.info("event=LOGIN_SUCCESS email={} ip={}", email, ip);
        registry.counter(LOGIN_ATTEMPTS, "outcome", "success").increment();
    }

    public void loginFailed(String email, String ip) {
        log.info("event=LOGIN_FAILURE email={} ip={} reason=bad_credentials", email, ip);
        registry.counter(LOGIN_ATTEMPTS, "outcome", "bad_credentials").increment();
    }

    /** A login attempt arrived while the email+IP pairing was already locked out. */
    public void loginBlocked(String email, String ip) {
        log.warn("event=LOGIN_BLOCKED email={} ip={} reason=locked_out", email, ip);
        registry.counter(LOGIN_ATTEMPTS, "outcome", "locked_out").increment();
    }

    /** The failure that crossed the threshold and activated the lockout. */
    public void lockoutTriggered(String email, String ip, long failures) {
        log.warn("event=LOCKOUT_TRIGGERED email={} ip={} failures={}", email, ip, failures);
        registry.counter(LOCKOUTS).increment();
    }

    public void registerSucceeded(String email, String ip) {
        log.info("event=REGISTER_SUCCESS email={} ip={}", email, ip);
        registry.counter(REGISTRATIONS, "outcome", "success").increment();
    }

    /** {@code reason} must stay a bounded set — currently email_taken | rate_limited. */
    public void registerRejected(String email, String ip, String reason) {
        log.info("event=REGISTER_REJECTED email={} ip={} reason={}", email, ip, reason);
        registry.counter(REGISTRATIONS, "outcome", reason).increment();
    }

    public void refreshSucceeded() {
        log.info("event=REFRESH_SUCCESS");
        registry.counter(TOKEN_REFRESH, "outcome", "success").increment();
    }

    /**
     * An invalid, expired, or already-rotated refresh token was presented. The
     * rotated case is the most interesting event in this log: it can mean a stolen
     * token was replayed after the legitimate client already rotated it.
     */
    public void refreshRejected() {
        log.warn("event=REFRESH_REJECTED");
        registry.counter(TOKEN_REFRESH, "outcome", "rejected").increment();
    }

    public void logout() {
        log.info("event=LOGOUT");
    }
}
