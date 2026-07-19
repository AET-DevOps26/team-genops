package com.jobready.auth.service;

import com.jobready.auth.config.JwtProperties;
import com.jobready.auth.exception.InvalidCredentialsException;
import com.jobready.auth.modelEntity.User;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private final JwtEncoder jwtEncoder;
    private final StringRedisTemplate redisTemplate;
    private final JwtProperties props;

    @Override
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .issuer(props.getIssuer())
                .audience(List.of(props.getAudience()))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(props.getAccessTokenExpiry()))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    @Override
    public String generateRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        redisTemplate
                .opsForValue()
                .set(refreshKey(token), user.getId().toString(), Duration.ofSeconds(props.getRefreshTokenExpiry()));
        return token;
    }

    @Override
    public UUID validateRefreshToken(String token) {
        String userId = redisTemplate.opsForValue().get(refreshKey(token));
        if (userId == null) {
            throw new InvalidCredentialsException();
        }
        return UUID.fromString(userId);
    }

    @Override
    public void revokeRefreshToken(String token) {
        redisTemplate.delete(refreshKey(token));
    }

    private String refreshKey(String token) {
        return "refresh:" + token;
    }
}
