package com.jobready.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String privateKey;
    private String publicKey;
    private long accessTokenExpiry;
    private long refreshTokenExpiry;
    private String keyId;
    private String issuer;
    private String audience;
}
