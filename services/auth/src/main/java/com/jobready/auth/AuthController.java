package com.jobready.auth;

import com.jobready.auth.exception.EmailAlreadyTakenException;
import com.jobready.auth.generated.api.AuthApi;
import com.jobready.auth.generated.model.RegisterRequest;
import com.jobready.auth.generated.model.TokenResponse;
import com.jobready.auth.user.User;
import com.jobready.auth.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ResponseEntity<TokenResponse> register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyTakenException(request.getEmail());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        // JWT issuance is not yet implemented — stub tokens returned
        TokenResponse tokens = new TokenResponse()
            .accessToken("stub-access-token")
            .refreshToken("stub-refresh-token")
            .tokenType(TokenResponse.TokenTypeEnum.BEARER)
            .expiresIn(900);

        return ResponseEntity.status(201).body(tokens);
    }
}
