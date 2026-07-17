package com.jobready.auth.service;

import com.jobready.auth.modelEntity.User;
import java.util.UUID;

public interface JwtService {
    String generateAccessToken(User user);

    String generateRefreshToken(User user);

    UUID validateRefreshToken(String token);

    void revokeRefreshToken(String token);
}
