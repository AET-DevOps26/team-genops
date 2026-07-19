package com.jobready.auth.config;

import com.jobready.auth.controller.JwksController;
import com.jobready.auth.generated.api.AuthApi;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties(CookieProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CookieProperties cookieProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(
                                AuthApi.PATH_REGISTER,
                                AuthApi.PATH_LOGIN,
                                AuthApi.PATH_REFRESH_TOKEN,
                                JwksController.PATH,
                                "/actuator/health/**",
                                "/actuator/prometheus")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.bearerTokenResolver(cookieOrHeaderTokenResolver())
                        .jwt(Customizer.withDefaults()));
        return http.build();
    }

    /**
     * Resolves the access token from the {@code jr_access} HttpOnly cookie, falling
     * back to the standard {@code Authorization: Bearer} header.
     *
     * Public endpoints (login, register, refresh, JWKS) are skipped entirely so that
     * a stale or invalidated cookie never causes a 401 on those paths. Spring Security
     * validates any resolved token before the controller runs; if we return a token for
     * a permitAll endpoint and the JWT is invalid (e.g. key rotated on restart), the
     * request is rejected before reaching the controller — hence the early-return null.
     */
    private static final java.util.Set<String> PUBLIC_PATHS = java.util.Set.of(
            AuthApi.PATH_LOGIN,
            AuthApi.PATH_REGISTER,
            AuthApi.PATH_REFRESH_TOKEN,
            JwksController.PATH,
            "/actuator/health");

    @Bean
    public BearerTokenResolver cookieOrHeaderTokenResolver() {
        DefaultBearerTokenResolver headerResolver = new DefaultBearerTokenResolver();
        return request -> {
            if (PUBLIC_PATHS.contains(request.getRequestURI())
                    || request.getRequestURI().startsWith("/actuator/health/")) {
                return null;
            }
            String fromHeader = headerResolver.resolve(request);
            if (fromHeader != null) {
                return fromHeader;
            }
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if (cookieProperties.getAccessName().equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }
            return null;
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CSRF origin check (see {@link OriginValidationFilter}). Registered ahead of
     * Spring Security so forged cross-origin requests are rejected before any
     * authentication work runs. Spring's token-based CSRF protection stays disabled
     * on purpose — this filter plus SameSite=Strict cookies is the CSRF posture.
     */
    @Bean
    public FilterRegistrationBean<OriginValidationFilter> originValidationFilter(
            @Value("${auth.security.allowed-origins}") List<String> allowedOrigins) {
        FilterRegistrationBean<OriginValidationFilter> registration =
                new FilterRegistrationBean<>(new OriginValidationFilter(Set.copyOf(allowedOrigins)));
        registration.setOrder(SecurityFilterProperties.DEFAULT_FILTER_ORDER - 1);
        return registration;
    }
}
