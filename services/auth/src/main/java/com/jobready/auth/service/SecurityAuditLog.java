package com.jobready.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Single seam for security-relevant events: every auth flow reports its outcome
 * here and nowhere else. Log lines are single-line key=value on the dedicated
 * {@code SECURITY_AUDIT} logger (greppable, routable via logback config alone).
 *
 * <p>Identity (email) belongs in these log lines — that's the forensic record.
 * When Prometheus counters are added, they will be incremented inside these same
 * methods with outcome labels only — never email/IP/user labels (unbounded
 * cardinality + PII in the metrics pipeline).
 *
 * <p>INFO = normal outcome, WARN = suspicious (lockouts, replayed refresh tokens).
 */
@Component
public class SecurityAuditLog {

    private static final Logger log = LoggerFactory.getLogger("SECURITY_AUDIT");

    public void loginSucceeded(String email, String ip) {
        log.info("event=LOGIN_SUCCESS email={} ip={}", email, ip);
    }

    public void loginFailed(String email, String ip) {
        log.info("event=LOGIN_FAILURE email={} ip={} reason=bad_credentials", email, ip);
    }

    /** A login attempt arrived while the email+IP pairing was already locked out. */
    public void loginBlocked(String email, String ip) {
        log.warn("event=LOGIN_BLOCKED email={} ip={} reason=locked_out", email, ip);
    }

    /** The failure that crossed the threshold and activated the lockout. */
    public void lockoutTriggered(String email, String ip, long failures) {
        log.warn("event=LOCKOUT_TRIGGERED email={} ip={} failures={}", email, ip, failures);
    }

    public void registerSucceeded(String email, String ip) {
        log.info("event=REGISTER_SUCCESS email={} ip={}", email, ip);
    }

    public void registerRejected(String email, String ip, String reason) {
        log.info("event=REGISTER_REJECTED email={} ip={} reason={}", email, ip, reason);
    }

    /**
     * An invalid, expired, or already-rotated refresh token was presented. The
     * rotated case is the most interesting event in this log: it can mean a stolen
     * token was replayed after the legitimate client already rotated it.
     */
    public void refreshRejected() {
        log.warn("event=REFRESH_REJECTED");
    }

    public void logout() {
        log.info("event=LOGOUT");
    }
}
