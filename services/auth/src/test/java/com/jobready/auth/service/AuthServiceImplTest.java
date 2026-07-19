package com.jobready.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobready.auth.config.JwtProperties;
import com.jobready.auth.exception.EmailAlreadyTakenException;
import com.jobready.auth.exception.InvalidCredentialsException;
import com.jobready.auth.exception.TooManyAttemptsException;
import com.jobready.auth.generated.modelDto.LoginRequest;
import com.jobready.auth.generated.modelDto.RegisterRequest;
import com.jobready.auth.model.IssuedSession;
import com.jobready.auth.modelEntity.User;
import com.jobready.auth.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    private static final String EMAIL = "jane@example.com";
    private static final String IP = "203.0.113.7";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private SecurityAuditLog auditLog;

    @InjectMocks
    private AuthServiceImpl service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void stubTokenIssuance() {
        when(jwtProperties.getAccessTokenExpiry()).thenReturn(900L);
        when(jwtProperties.getRefreshTokenExpiry()).thenReturn(604800L);
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
    }

    private User user(String email) {
        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        user.setPasswordHash("$2a$10$storedhash");
        user.setCreatedAt(OffsetDateTime.now());
        return user;
    }

    // ------------------------------------------------------------------
    // Register
    // ------------------------------------------------------------------

    @Test
    void register_storesOnlyTheHash_neverTheRawPassword() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode("s3cret-plaintext")).thenReturn("$2a$10$hashed");

        service.register(new RegisterRequest(EMAIL, "s3cret-plaintext"), IP);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("$2a$10$hashed");
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("s3cret-plaintext");
    }

    @Test
    void register_rejectsAnEmailThatIsAlreadyTakenAndAudits() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("taken@example.com", "pw"), IP))
                .isInstanceOf(EmailAlreadyTakenException.class);

        verify(userRepository, never()).save(any());
        verify(auditLog).registerRejected("taken@example.com", IP, "email_taken");
    }

    @Test
    void register_issuesASessionForTheNewUserAndAudits() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashed");

        IssuedSession session = service.register(new RegisterRequest(EMAIL, "pw"), IP);

        assertThat(session.accessToken()).isEqualTo("access-token");
        assertThat(session.refreshToken()).isEqualTo("refresh-token");
        assertThat(session.user().getEmail()).isEqualTo(EMAIL);
        verify(auditLog).registerSucceeded(EMAIL, IP);
    }

    @Test
    void register_neverExposesTheHashInTheResponse() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashed");

        IssuedSession session = service.register(new RegisterRequest(EMAIL, "pw"), IP);

        assertThat(session.user().toString()).doesNotContain("$2a$10$hashed");
    }

    @Test
    void register_throttledIsRejectedBeforeTouchingTheDatabase() {
        doThrow(new TooManyAttemptsException(900)).when(loginAttemptService).recordRegisterAttempt(IP);

        assertThatThrownBy(() -> service.register(new RegisterRequest(EMAIL, "x"), IP))
                .isInstanceOf(TooManyAttemptsException.class);

        verify(auditLog).registerRejected(EMAIL, IP, "rate_limited");
        verify(userRepository, never()).existsByEmail(anyString());
    }

    // ------------------------------------------------------------------
    // Login
    // ------------------------------------------------------------------

    @Test
    void login_succeedsWhenThePasswordMatches_resetsCounterAndAudits() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user(EMAIL)));
        when(passwordEncoder.matches("right-password", "$2a$10$storedhash")).thenReturn(true);

        IssuedSession session = service.login(new LoginRequest(EMAIL, "right-password"), IP);

        assertThat(session.accessToken()).isEqualTo("access-token");
        assertThat(session.user().getId()).isEqualTo(userId);
        verify(loginAttemptService).resetLoginFailures(EMAIL, IP);
        verify(auditLog).loginSucceeded(EMAIL, IP);
    }

    @Test
    void login_rejectsAWrongPassword_recordsFailureAndAudits() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user(EMAIL)));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, "wrong"), IP))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateAccessToken(any());
        // The attempt was already reserved up front; failure adds no second count.
        verify(loginAttemptService).reserveLoginAttempt(EMAIL, IP);
        verify(auditLog).loginFailed(EMAIL, IP);
        verify(loginAttemptService, never()).resetLoginFailures(anyString(), anyString());
    }

    @Test
    void login_rejectsAnUnknownEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("nobody@example.com", "pw"), IP))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_lockedOutIsBlockedBeforeAnyCredentialWork() {
        doThrow(new TooManyAttemptsException(900)).when(loginAttemptService).reserveLoginAttempt(EMAIL, IP);

        assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, "secret"), IP))
                .isInstanceOf(TooManyAttemptsException.class);

        verify(auditLog).loginBlocked(EMAIL, IP);
        verify(userRepository, never()).findByEmail(anyString());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    /**
     * An unknown email and a wrong password must fail identically. A distinguishable
     * response would let an attacker enumerate which addresses have accounts.
     */
    @Test
    void login_doesNotRevealWhetherTheAccountExists() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user(EMAIL)));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        Throwable unknownEmail = catchThrowable(() -> service.login(new LoginRequest("nobody@example.com", "pw"), IP));
        Throwable wrongPassword = catchThrowable(() -> service.login(new LoginRequest(EMAIL, "pw"), IP));

        assertThat(unknownEmail).isInstanceOf(InvalidCredentialsException.class);
        assertThat(wrongPassword).isInstanceOf(InvalidCredentialsException.class);
        assertThat(unknownEmail).hasSameClassAs(wrongPassword);
        assertThat(unknownEmail.getMessage()).isEqualTo(wrongPassword.getMessage());
    }

    @Test
    void login_unknownUserStillRunsExactlyOneHashComparison() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, "secret"), IP))
                .isInstanceOf(InvalidCredentialsException.class);

        // The timing-equalizing dummy compare: same BCrypt cost as the found-user path.
        verify(passwordEncoder, times(1)).matches(eq("secret"), anyString());
        verify(loginAttemptService).reserveLoginAttempt(EMAIL, IP);
        verify(auditLog).loginFailed(EMAIL, IP);
    }

    // ------------------------------------------------------------------
    // Refresh — rotation
    // ------------------------------------------------------------------

    /**
     * Refresh tokens are single-use: the presented token is revoked as part of issuing
     * the replacement, so a stolen token cannot be replayed after the legitimate holder
     * has used it.
     */
    @Test
    void refresh_revokesThePresentedTokenAndIssuesANewOne() {
        when(jwtService.validateRefreshToken("old-refresh")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(EMAIL)));

        IssuedSession session = service.refresh("old-refresh");

        verify(jwtService).revokeRefreshToken("old-refresh");
        assertThat(session.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void refresh_revokesTheOldTokenBeforeIssuingTheReplacement() {
        when(jwtService.validateRefreshToken("old-refresh")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(EMAIL)));

        service.refresh("old-refresh");

        InOrder order = inOrder(jwtService);
        order.verify(jwtService).validateRefreshToken("old-refresh");
        order.verify(jwtService).revokeRefreshToken("old-refresh");
        order.verify(jwtService).generateRefreshToken(any(User.class));
    }

    @Test
    void refresh_rejectsAnUnknownTokenAndAudits() {
        when(jwtService.validateRefreshToken("bogus")).thenThrow(new InvalidCredentialsException());

        assertThatThrownBy(() -> service.refresh("bogus")).isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateAccessToken(any());
        verify(auditLog).refreshRejected();
    }

    /** A token valid in Redis but whose user has since been deleted must not mint a session. */
    @Test
    void refresh_rejectsATokenWhoseUserNoLongerExists() {
        when(jwtService.validateRefreshToken("orphan")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("orphan")).isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateAccessToken(any());
    }

    // ------------------------------------------------------------------
    // Logout / me
    // ------------------------------------------------------------------

    @Test
    void logout_revokesTheRefreshTokenAndAudits() {
        service.logout("some-refresh");

        verify(jwtService).revokeRefreshToken("some-refresh");
        verify(auditLog).logout();
    }

    @Test
    void getMe_returnsTheUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(EMAIL)));

        assertThat(service.getMe(userId).getEmail()).isEqualTo(EMAIL);
    }

    @Test
    void getMe_rejectsAnUnknownUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMe(userId)).isInstanceOf(InvalidCredentialsException.class);
    }
}
