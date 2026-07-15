package com.jobready.auth.service;

import com.jobready.auth.config.JwtProperties;
import com.jobready.auth.exception.EmailAlreadyTakenException;
import com.jobready.auth.exception.InvalidCredentialsException;
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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @Override
    public IssuedSession register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyTakenException(request.getEmail());
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        return issueSession(user);
    }

    @Override
    public IssuedSession login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return issueSession(user);
    }

    @Override
    public void logout(String refreshToken) {
        jwtService.revokeRefreshToken(refreshToken);
    }

    @Override
    public IssuedSession refresh(String refreshToken) {
        UUID userId = jwtService.validateRefreshToken(refreshToken);
        jwtService.revokeRefreshToken(refreshToken);
        User user = userRepository.findById(userId).orElseThrow(InvalidCredentialsException::new);
        return issueSession(user);
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
