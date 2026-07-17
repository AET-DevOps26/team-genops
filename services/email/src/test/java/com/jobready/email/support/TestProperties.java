package com.jobready.email.support;

import com.jobready.email.config.EmailProperties;

/** Builds fully-populated {@link EmailProperties} for unit tests. */
public final class TestProperties {

    private TestProperties() {}

    public static EmailProperties emailProperties() {
        return emailProperties("test-state-signing-key");
    }

    public static EmailProperties emailProperties(String stateSigningKey) {
        return new EmailProperties(
                new EmailProperties.Google(
                        "client-id",
                        "client-secret",
                        "http://localhost:8001/api/v1/email/connections/gmail/callback",
                        "https://accounts.google.com/o/oauth2/auth",
                        "https://oauth2.googleapis.com/token",
                        "https://www.googleapis.com/oauth2/v2/userinfo",
                        "https://gmail.googleapis.com"),
                new EmailProperties.State(stateSigningKey, 600),
                "http://localhost:5173",
                "test-token-enc-key",
                25);
    }
}
