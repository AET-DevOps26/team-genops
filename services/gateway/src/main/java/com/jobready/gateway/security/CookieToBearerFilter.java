package com.jobready.gateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Translates the split-token BFF cookie into a standard bearer header.
 *
 * <p>The browser edge speaks cookies ({@code jr_access}, HttpOnly). The mesh contract is
 * {@code Authorization: Bearer <jwt>} — services never see cookies. This filter bridges the two:
 * if a request arrives with the access cookie but no {@code Authorization} header, it wraps the
 * request so the header appears to be present.
 *
 * <p>It runs <em>before</em> Spring Security (see {@code SecurityConfig}), so the synthesized header
 * is visible both to the resource-server JWT validation and to the downstream gateway proxy that
 * forwards the request to backend services.
 *
 * <p>Deliberately a plain class (not {@code @Component}) so it is registered exactly once via an
 * explicit {@code FilterRegistrationBean}; component-scanning would also auto-register it and run it twice.
 */
public class CookieToBearerFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final String cookieName;

    private final Set<String> skipPaths;

    public CookieToBearerFilter(String cookieName, Set<String> skipPaths) {
        this.cookieName = cookieName;
        this.skipPaths = skipPaths;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // An explicit Authorization header always wins — never override a caller that already
        // speaks the mesh contract (e.g. service-to-service or a test harness).
        if (request.getHeader(HttpHeaders.AUTHORIZATION) == null && !skipPaths.contains(request.getRequestURI())) {
            String token = accessCookie(request);
            if (token != null && !token.isBlank()) {
                chain.doFilter(new BearerHeaderRequest(request, BEARER_PREFIX + token), response);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private String accessCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * Request wrapper that injects a single {@code Authorization} header on top of the real request.
     * All other headers pass through untouched.
     */
    private static final class BearerHeaderRequest extends HttpServletRequestWrapper {

        private final String authorization;

        BearerHeaderRequest(HttpServletRequest request, String authorization) {
            super(request);
            this.authorization = authorization;
        }

        @Override
        public String getHeader(String name) {
            if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                return authorization;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                return Collections.enumeration(List.of(authorization));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = Collections.list(super.getHeaderNames());
            boolean present = names.stream().anyMatch(HttpHeaders.AUTHORIZATION::equalsIgnoreCase);
            if (!present) {
                names.add(HttpHeaders.AUTHORIZATION);
            }
            return Collections.enumeration(names);
        }
    }
}
