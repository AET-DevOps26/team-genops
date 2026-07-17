package com.jobready.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class AuthApplicationTests {

    @DynamicPropertySource
    static void jwtKeys(DynamicPropertyRegistry registry) {
        registry.add("jwt.private-key", TestJwtKeys::privateKeyPem);
        registry.add("jwt.public-key", TestJwtKeys::publicKeyPem);
    }

    @Test
    void contextLoads() {}
}
