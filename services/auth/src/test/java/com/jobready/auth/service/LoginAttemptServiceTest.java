package com.jobready.auth.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobready.auth.exception.TooManyAttemptsException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    private static final String EMAIL = "jane@example.com";
    private static final String IP = "203.0.113.7";
    private static final String LOGIN_KEY = "login:fail:" + EMAIL + ":" + IP;
    private static final String REGISTER_KEY = "register:attempt:" + IP;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private SecurityAuditLog auditLog;

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        service = new LoginAttemptService(redisTemplate, auditLog, 5, 10, 900);
    }

    @Test
    void underThresholdIsAllowed() {
        when(valueOps.get(LOGIN_KEY)).thenReturn("4");
        assertDoesNotThrow(() -> service.checkLoginAllowed(EMAIL, IP));
    }

    @Test
    void atThresholdIsLockedWithRemainingTtlAsRetryAfter() {
        when(valueOps.get(LOGIN_KEY)).thenReturn("5");
        when(redisTemplate.getExpire(LOGIN_KEY)).thenReturn(321L);

        TooManyAttemptsException ex =
                assertThrows(TooManyAttemptsException.class, () -> service.checkLoginAllowed(EMAIL, IP));
        assertEquals(321, ex.getRetryAfterSeconds());
    }

    @Test
    void firstFailureStartsTheWindow() {
        when(valueOps.increment(LOGIN_KEY)).thenReturn(1L);

        service.recordLoginFailure(EMAIL, IP);

        verify(redisTemplate).expire(LOGIN_KEY, Duration.ofSeconds(900));
        verify(auditLog, never()).lockoutTriggered(EMAIL, IP, 1);
    }

    @Test
    void thresholdCrossingFailureAuditsLockout() {
        when(valueOps.increment(LOGIN_KEY)).thenReturn(5L);

        service.recordLoginFailure(EMAIL, IP);

        verify(auditLog).lockoutTriggered(EMAIL, IP, 5);
    }

    @Test
    void emailKeyIsCaseInsensitive() {
        when(valueOps.get(LOGIN_KEY)).thenReturn(null);
        assertDoesNotThrow(() -> service.checkLoginAllowed("Jane@Example.COM", IP));
        verify(valueOps).get(LOGIN_KEY);
    }

    @Test
    void successResetDeletesTheCounter() {
        service.resetLoginFailures(EMAIL, IP);
        verify(redisTemplate).delete(LOGIN_KEY);
    }

    @Test
    void registerWithinLimitIsAllowed() {
        when(valueOps.increment(REGISTER_KEY)).thenReturn(10L);
        assertDoesNotThrow(() -> service.recordRegisterAttempt(IP));
    }

    @Test
    void registerOverLimitThrows() {
        when(valueOps.increment(REGISTER_KEY)).thenReturn(11L);
        when(redisTemplate.getExpire(REGISTER_KEY)).thenReturn(600L);

        TooManyAttemptsException ex =
                assertThrows(TooManyAttemptsException.class, () -> service.recordRegisterAttempt(IP));
        assertEquals(600, ex.getRetryAfterSeconds());
    }
}
