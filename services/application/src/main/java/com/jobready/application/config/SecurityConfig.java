package com.jobready.application.config;

import com.jobready.application.internal.InternalTokenFilter;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    /** Name of the HttpOnly access-token cookie set by the auth service at the browser edge. */
    private static final String ACCESS_COOKIE = "jr_access";

    /**
     * {@code /internal/**} is authenticated by {@link InternalTokenFilter} (static service
     * token), not by user JWTs — it exists for trusted backend callers like the email service.
     * The filter rejects every request without the correct token, so {@code permitAll} here
     * only hands the decision to the filter.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain internalFilterChain(
            HttpSecurity http, @Value("${internal.service-token:}") String internalServiceToken) throws Exception {
        http.securityMatcher("/internal/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(
                        new InternalTokenFilter(internalServiceToken), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/prometheus",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.bearerTokenResolver(cookieOrHeaderTokenResolver())
                        .jwt(Customizer.withDefaults()));
        return http.build();
    }

    /**
     * Resolves the access token from the {@code jr_access} HttpOnly cookie, falling back to the
     * standard {@code Authorization: Bearer} header (the form the gateway forwards internally).
     */
    @Bean
    public BearerTokenResolver cookieOrHeaderTokenResolver() {
        DefaultBearerTokenResolver headerResolver = new DefaultBearerTokenResolver();
        return request -> {
            String fromHeader = headerResolver.resolve(request);
            if (fromHeader != null) {
                return fromHeader;
            }
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if (ACCESS_COOKIE.equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }
            return null;
        };
    }
}
