package com.jobready.auth.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * CSRF defense for the cookie-authenticated edge: state-changing requests must
 * originate from this app's own origin.
 *
 * <p>{@code SameSite=Strict} alone is not sufficient here — SameSite is scoped to
 * the registrable domain (eTLD+1), so on a shared university domain every sibling
 * subdomain is "same-site" and the browser still attaches our cookies to requests
 * forged from there. The {@code Origin} header is exact-origin and unforgeable
 * from a victim's browser, which closes that gap.
 *
 * <p>Decision table (state-changing methods only):
 * <ul>
 *   <li>{@code Origin} present and allow-listed → allow; otherwise 403.</li>
 *   <li>No {@code Origin} but {@code Referer} present → prefix-match against the list.</li>
 *   <li>Neither header → allow: CSRF is strictly a browser attack (it abuses automatic
 *       cookie attachment) and browsers always send {@code Origin} on cross-origin
 *       POSTs; header-less callers are curl/tests/services, which CSRF cannot reach.</li>
 * </ul>
 */
public class OriginValidationFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final Set<String> allowedOrigins;

    public OriginValidationFilter(Set<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SAFE_METHODS.contains(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        String origin = request.getHeader("Origin");
        if (origin != null) {
            if (allowedOrigins.contains(origin)) {
                chain.doFilter(request, response);
            } else {
                reject(response);
            }
            return;
        }
        String referer = request.getHeader("Referer");
        if (referer != null) {
            boolean allowed = allowedOrigins.stream().anyMatch(o -> referer.equals(o) || referer.startsWith(o + "/"));
            if (allowed) {
                chain.doFilter(request, response);
            } else {
                reject(response);
            }
            return;
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":\"ORIGIN_FORBIDDEN\",\"message\":\"Cross-origin request rejected\"}");
    }
}
