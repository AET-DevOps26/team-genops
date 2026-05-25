package com.jobready.auth.service;

import com.jobready.auth.exception.EmailAlreadyTakenException;
import com.jobready.auth.generated.modelDto.RegisterRequest;
import com.jobready.auth.generated.modelDto.TokenResponse;
import com.jobready.auth.modelEntity.User;
import com.jobready.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyTakenException(request.getEmail());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        // JWT issuance is not yet implemented — stub tokens returned
        return new TokenResponse()
            .accessToken("stub-access-token")
            .refreshToken("stub-refresh-token")
            .tokenType(TokenResponse.TokenTypeEnum.BEARER)
            .expiresIn(900);
    }
}
