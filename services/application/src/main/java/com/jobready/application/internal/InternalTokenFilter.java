package com.jobready.application.internal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Guards {@code /internal/**} with a static shared-secret bearer token ({@code
 * INTERNAL_SERVICE_TOKEN}), used by trusted backend services (the email service) that act
 * without a live user request. These paths are the one sanctioned exception to the
 * "user_id from JWT only" rule — the caller names the user explicitly, so the token must
 * never be exposed outside the cluster. Requests are rejected outright when no token is
 * configured, so the internal API is fail-closed.
 *
 * <p>Constructed by {@code SecurityConfig} (not component-scanned) so it is registered only
 * inside the internal security chain, never as a global servlet filter.</p>
 */
public class InternalTokenFilter extends OncePerRequestFilter {

    private final String expectedToken;

    public InternalTokenFilter(String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        String presented = header != null && header.startsWith("Bearer ") ? header.substring(7) : "";
        if (expectedToken.isBlank() || !constantTimeEquals(expectedToken, presented)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter()
                    .write(
                            "{\"code\":\"UNAUTHORIZED\",\"message\":\"Missing or invalid internal service token\",\"details\":null}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
