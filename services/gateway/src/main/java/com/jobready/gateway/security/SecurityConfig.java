package com.jobready.gateway.security;

import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * <p>Request lifecycle at the gateway:
 * <ol>
 *   <li>{@link CookieToBearerFilter} (order {@code -101}, before Spring Security) turns the
 *       {@code jr_access} cookie into an {@code Authorization: Bearer} header.</li>
 *   <li>Spring Security's resource server validates the JWT's RS256 signature and expiry against
 *       auth's published JWK Set (configured via {@code spring.security.oauth2.resourceserver.jwt}).</li>
 *   <li>Authorized requests are proxied on by the gateway routes; the bearer header rides along, so
 *       downstream services receive the mesh-standard {@code Authorization} header.</li>
 * </ol>
 *
 * <p>This is the outer layer of defense in depth — each backend service still re-verifies the same
 * signature independently, so a request that somehow bypasses the gateway is not implicitly trusted.
 *
 * <p>Note: {@code iss}/{@code aud} are not yet enforced here. Auth does not emit those claims today;
 * adding enforcement now would reject every token. Enable it (in both the gateway and the services)
 * once auth starts issuing them — a non-breaking, coordinated change tracked in the auth README.
 */
@Configuration
public class SecurityConfig {

    /** Split-token BFF access cookie set by the auth service. */
    private static final String ACCESS_COOKIE = "jr_access";

    /** Auth endpoints that must stay open — a caller cannot yet have a token when hitting these. */
    private static final String[] PUBLIC_AUTH_POST = {
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
    };

    /** Public key material other services fetch to verify tokens; must be reachable unauthenticated. */
    // keep in sync with auth JwksController.PATH
    private static final String JWKS_PATH = "/api/v1/auth/.well-known/jwks.json";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // No browser form/session state at the gateway: it is a stateless token checker.
                // CSRF protection is irrelevant because auth is by bearer token, not an ambient
                // session cookie the browser attaches automatically.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // k8s liveness/readiness probes must never require a token.
                        .requestMatchers("/actuator/health/**").permitAll()
                        // Anonymous auth flows: you cannot present a token to obtain your first token.
                        .requestMatchers(HttpMethod.POST, PUBLIC_AUTH_POST).permitAll()
                        // JWKS is public verification material by design.
                        .requestMatchers(HttpMethod.GET, JWKS_PATH).permitAll()
                        // Only the probe group above is public. Everything else under /actuator 
                        // (notably /actuator/gateway/routes, which enumerates the internal topology) 
                        // must never be reachable through the ingress. denyAll rather than authenticated 
                        // — no end-user JWT should grant access to the gateway's own operational surface.
                        .requestMatchers("/actuator/**").denyAll()
                        // Everything else under /api requires a valid access token at the edge.
                        .requestMatchers("/api/**").authenticated()
                        // Non-API traffic (SPA shell, static assets) is served openly.
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    FilterRegistrationBean<CookieToBearerFilter> cookieToBearerFilter() {
        FilterRegistrationBean<CookieToBearerFilter> registration =
                new FilterRegistrationBean<>(new CookieToBearerFilter(ACCESS_COOKIE));
        // Run just before Spring Security's filter chain so the synthesized Authorization header
        // exists when the resource server looks for it. Spring Security's chain registers at
        // SecurityFilterProperties.DEFAULT_FILTER_ORDER; one slot earlier guarantees we run first.
        registration.setOrder(SecurityFilterProperties.DEFAULT_FILTER_ORDER - 1);
        return registration;
    }
}
