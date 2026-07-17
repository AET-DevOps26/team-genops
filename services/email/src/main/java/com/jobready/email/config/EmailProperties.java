package com.jobready.email.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * All email-service settings, bound from {@code email.*} properties (which in turn read the
 * env vars documented in .env.example: GOOGLE_CLIENT_ID/SECRET, GOOGLE_REDIRECT_URI,
 * FRONTEND_REDIRECT_URL, EMAIL_TOKEN_ENC_KEY, STATE_SIGNING_KEY, ...).
 *
 * <p>The Google endpoint URLs are configurable only so tests can point the clients at a local
 * stub server; production always uses the defaults.
 */
@ConfigurationProperties(prefix = "email")
public record EmailProperties(
        Google google,
        State state,
        /** URL the OAuth callback redirects the browser back to (the web client). */
        @DefaultValue("http://localhost:5173") String frontendRedirectUrl,
        /** Symmetric key used to AES-GCM-encrypt stored OAuth tokens at rest. */
        String tokenEncKey,
        /** How many recent Gmail messages each poll pass fetches per connection. */
        @DefaultValue("25") int gmailMaxResults) {

    public record Google(
            String clientId,
            String clientSecret,
            /** Must exactly match a redirect URI registered in the Google Cloud console. */
            String redirectUri,
            @DefaultValue("https://accounts.google.com/o/oauth2/auth") String authUri,
            @DefaultValue("https://oauth2.googleapis.com/token") String tokenUri,
            @DefaultValue("https://www.googleapis.com/oauth2/v2/userinfo") String userinfoUri,
            @DefaultValue("https://gmail.googleapis.com") String gmailBaseUrl) {}

    public record State(
            /** HS256 key for the signed single-use OAuth `state` token. */
            String signingKey, @DefaultValue("600") long ttlSeconds) {}
}
