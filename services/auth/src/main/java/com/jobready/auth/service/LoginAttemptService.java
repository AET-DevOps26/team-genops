package com.jobready.auth.service;

import com.jobready.auth.exception.TooManyAttemptsException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis-backed brute-force throttling.
 *
 * <p>Login failures are counted per email+IP (an attacker spraying one account from
 * one address locks only that pairing; keying per-email alone would let an attacker
 * deliberately lock a victim out). Failures for nonexistent emails are counted too —
 * otherwise the lockout's very presence would leak whether an account exists.
 *
 * <p>Registration is throttled per IP regardless of outcome, which is the mitigation
 * for the (documented) email-enumeration oracle on register.
 */
@Service
public class LoginAttemptService {

    private static final String LOGIN_KEY_PREFIX = "login:fail:";
    private static final String REGISTER_KEY_PREFIX = "register:attempt:";

    private final StringRedisTemplate redisTemplate;
    private final SecurityAuditLog auditLog;
    private final int loginMaxFailures;
    private final int registerMaxAttempts;
    private final Duration window;

    public LoginAttemptService(
            StringRedisTemplate redisTemplate,
            SecurityAuditLog auditLog,
            @Value("${auth.security.login-max-failures}") int loginMaxFailures,
            @Value("${auth.security.register-max-attempts}") int registerMaxAttempts,
            @Value("${auth.security.attempt-window-seconds}") long windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.auditLog = auditLog;
        this.loginMaxFailures = loginMaxFailures;
        this.registerMaxAttempts = registerMaxAttempts;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    /** Throws {@link TooManyAttemptsException} if this email+IP pairing is locked out. */
    public void checkLoginAllowed(String email, String ip) {
        String key = loginKey(email, ip);
        String count = redisTemplate.opsForValue().get(key);
        if (count != null && Integer.parseInt(count) >= loginMaxFailures) {
            throw new TooManyAttemptsException(retryAfterSeconds(key));
        }
    }

    public void recordLoginFailure(String email, String ip) {
        String key = loginKey(email, ip);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, window);
        }
        if (count != null && count == loginMaxFailures) {
            auditLog.lockoutTriggered(email, ip, count);
        }
    }

    public void resetLoginFailures(String email, String ip) {
        redisTemplate.delete(loginKey(email, ip));
    }

    /** Counts every register attempt for this IP and throws once over the limit. */
    public void recordRegisterAttempt(String ip) {
        String key = REGISTER_KEY_PREFIX + ip;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, window);
        }
        if (count != null && count > registerMaxAttempts) {
            throw new TooManyAttemptsException(retryAfterSeconds(key));
        }
    }

    private String loginKey(String email, String ip) {
        return LOGIN_KEY_PREFIX + email.toLowerCase() + ":" + ip;
    }

    private long retryAfterSeconds(String key) {
        Long ttl = redisTemplate.getExpire(key);
        return (ttl != null && ttl > 0) ? ttl : window.toSeconds();
    }
}
