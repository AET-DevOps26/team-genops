package com.jobready.auth.service;

import com.jobready.auth.config.JwtProperties;
import com.jobready.auth.exception.EmailAlreadyTakenException;
import com.jobready.auth.exception.InvalidCredentialsException;
import com.jobready.auth.exception.TooManyAttemptsException;
import com.jobready.auth.generated.modelDto.LoginRequest;
import com.jobready.auth.generated.modelDto.RegisterRequest;
import com.jobready.auth.generated.modelDto.UserResponse;
import com.jobready.auth.model.IssuedSession;
import com.jobready.auth.modelEntity.User;
import com.jobready.auth.repository.UserRepository;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /**
     * Any valid BCrypt hash works here — it only equalizes login timing for
     * nonexistent accounts; nothing can log in with it (the null-user check wins).
     */
    private static final String DUMMY_HASH =
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("timing-equalizer");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final LoginAttemptService loginAttemptService;
    private final SecurityAuditLog auditLog;

    @Override
    public IssuedSession register(RegisterRequest request, String clientIp) {
        String email = request.getEmail();
        try {
            loginAttemptService.recordRegisterAttempt(clientIp);
        } catch (TooManyAttemptsException e) {
            auditLog.registerRejected(email, clientIp, "rate_limited");
            throw e;
        }
        if (userRepository.existsByEmail(email)) {
            auditLog.registerRejected(email, clientIp, "email_taken");
            throw new EmailAlreadyTakenException(email);
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        auditLog.registerSucceeded(email, clientIp);
        return issueSession(user);
    }

    @Override
    public IssuedSession login(LoginRequest request, String clientIp) {
        String email = request.getEmail();
        try {
            loginAttemptService.checkLoginAllowed(email, clientIp);
        } catch (TooManyAttemptsException e) {
            auditLog.loginBlocked(email, clientIp);
            throw e;
        }
        User user = userRepository.findByEmail(email).orElse(null);
        // Exactly one BCrypt comparison on every path: without the dummy compare, a
        // missing user returns ~100ms faster and response timing enumerates accounts.
        boolean matches =
                passwordEncoder.matches(request.getPassword(), user != null ? user.getPasswordHash() : DUMMY_HASH);
        if (user == null || !matches) {
            loginAttemptService.recordLoginFailure(email, clientIp);
            auditLog.loginFailed(email, clientIp);
            throw new InvalidCredentialsException();
        }
        loginAttemptService.resetLoginFailures(email, clientIp);
        auditLog.loginSucceeded(email, clientIp);
        return issueSession(user);
    }

    @Override
    public void logout(String refreshToken) {
        jwtService.revokeRefreshToken(refreshToken);
        auditLog.logout();
    }

    @Override
    public IssuedSession refresh(String refreshToken) {
        try {
            UUID userId = jwtService.validateRefreshToken(refreshToken);
            jwtService.revokeRefreshToken(refreshToken);
            User user = userRepository.findById(userId).orElseThrow(InvalidCredentialsException::new);
            return issueSession(user);
        } catch (InvalidCredentialsException e) {
            auditLog.refreshRejected();
            throw e;
        }
    }

    @Override
    public UserResponse getMe(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(InvalidCredentialsException::new);
        return toUserResponse(user);
    }

    private IssuedSession issueSession(User user) {
        return new IssuedSession(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                Duration.ofSeconds(jwtProperties.getAccessTokenExpiry()),
                Duration.ofSeconds(jwtProperties.getRefreshTokenExpiry()),
                toUserResponse(user));
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse().id(user.getId()).email(user.getEmail()).createdAt(user.getCreatedAt());
    }
}
