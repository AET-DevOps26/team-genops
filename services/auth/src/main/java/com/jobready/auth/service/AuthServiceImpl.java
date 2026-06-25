package com.jobready.auth.service;

import com.jobready.auth.exception.EmailAlreadyTakenException;
import com.jobready.auth.exception.InvalidCredentialsException;
import com.jobready.auth.generated.modelDto.LoginRequest;
import com.jobready.auth.generated.modelDto.RegisterRequest;
import com.jobready.auth.generated.modelDto.TokenResponse;
import com.jobready.auth.generated.modelDto.UserResponse;
import com.jobready.auth.modelEntity.User;
import com.jobready.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyTakenException(request.getEmail());
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        return issueTokens(user);
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return issueTokens(user);
    }

    @Override
    public void logout(String refreshToken) {
        jwtService.revokeRefreshToken(refreshToken);
    }

    @Override
    public TokenResponse refresh(String refreshToken) {
        UUID userId = jwtService.validateRefreshToken(refreshToken);
        jwtService.revokeRefreshToken(refreshToken);
        User user = userRepository.findById(userId)
            .orElseThrow(InvalidCredentialsException::new);
        return issueTokens(user);
    }

    @Override
    public UserResponse getMe(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(InvalidCredentialsException::new);
        return new UserResponse()
            .id(user.getId())
            .email(user.getEmail())
            .createdAt(user.getCreatedAt());
    }

    private TokenResponse issueTokens(User user) {
        return new TokenResponse()
            .accessToken(jwtService.generateAccessToken(user))
            .refreshToken(jwtService.generateRefreshToken(user))
            .tokenType(TokenResponse.TokenTypeEnum.BEARER)
            .expiresIn((int) 900);
    }
}
