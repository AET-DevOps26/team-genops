package com.jobready.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * The browser edge speaks HttpOnly cookies; the mesh contract is {@code Authorization: Bearer}.
 * This filter is the only thing bridging the two, and it runs ahead of Spring Security — so if it
 * stops synthesizing the header, every authenticated request through the gateway fails, and if it
 * synthesizes one it should not, the resource server validates a token the caller never presented.
 */
class CookieToBearerFilterTest {

    private static final String ACCESS_COOKIE = "jr_access";

    /** Mirrors SecurityConfig.SKIP_PATHS — the anonymous auth flows and public key material. */
    private static final Set<String> SKIP_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/api/v1/auth/.well-known/jwks.json");

    private final CookieToBearerFilter filter = new CookieToBearerFilter(ACCESS_COOKIE, SKIP_PATHS);

    private static MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        return request;
    }

    /** Runs the filter and returns the request the chain actually received. */
    private HttpServletRequest passThrough(MockHttpServletRequest request) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return (HttpServletRequest) chain.getRequest();
    }

    // ------------------------------------------------------------------
    // Translation
    // ------------------------------------------------------------------

    @Test
    void theAccessCookieBecomesABearerHeader() throws Exception {
        MockHttpServletRequest request = request("/api/v1/applications");
        request.setCookies(new Cookie(ACCESS_COOKIE, "the-jwt"));

        HttpServletRequest forwarded = passThrough(request);

        assertThat(forwarded.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer the-jwt");
    }

    @Test
    void theSynthesizedHeaderIsVisibleCaseInsensitively() throws Exception {
        // Servlet header lookup is case-insensitive; the resource server may ask either way.
        MockHttpServletRequest request = request("/api/v1/applications");
        request.setCookies(new Cookie(ACCESS_COOKIE, "the-jwt"));

        HttpServletRequest forwarded = passThrough(request);

        assertThat(forwarded.getHeader("authorization")).isEqualTo("Bearer the-jwt");
        assertThat(forwarded.getHeader("AUTHORIZATION")).isEqualTo("Bearer the-jwt");
    }

    @Test
    void theSynthesizedHeaderIsEnumeratedExactlyOnce() throws Exception {
        MockHttpServletRequest request = request("/api/v1/applications");
        request.setCookies(new Cookie(ACCESS_COOKIE, "the-jwt"));

        HttpServletRequest forwarded = passThrough(request);

        assertThat(Collections.list(forwarded.getHeaders(HttpHeaders.AUTHORIZATION)))
                .containsExactly("Bearer the-jwt");
        assertThat(Collections.list(forwarded.getHeaderNames()))
                .filteredOn(HttpHeaders.AUTHORIZATION::equalsIgnoreCase)
                .hasSize(1);
    }

    @Test
    void otherHeadersPassThroughUntouched() throws Exception {
        MockHttpServletRequest request = request("/api/v1/applications");
        request.setCookies(new Cookie(ACCESS_COOKIE, "the-jwt"));
        request.addHeader("X-Request-Id", "abc-123");

        HttpServletRequest forwarded = passThrough(request);

        assertThat(forwarded.getHeader("X-Request-Id")).isEqualTo("abc-123");
        assertThat(Collections.list(forwarded.getHeaderNames())).contains("X-Request-Id");
    }

    // ------------------------------------------------------------------
    // When it must NOT translate
    // ------------------------------------------------------------------

    @Test
    void anExplicitAuthorizationHeaderIsNeverOverridden() throws Exception {
        // Service-to-service callers already speak the mesh contract; the cookie must not win.
        MockHttpServletRequest request = request("/api/v1/applications");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer caller-supplied");
        request.setCookies(new Cookie(ACCESS_COOKIE, "cookie-token"));

        HttpServletRequest forwarded = passThrough(request);

        assertThat(forwarded.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer caller-supplied");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/api/v1/auth/login",
                "/api/v1/auth/register",
                "/api/v1/auth/refresh",
                "/api/v1/auth/.well-known/jwks.json"
            })
    void skipPathsAreLeftAlone(String uri) throws Exception {
        // These are the anonymous flows. A stale cookie must not turn them into authenticated
        // requests — logging in again while holding an expired token has to keep working.
        MockHttpServletRequest request = request(uri);
        request.setCookies(new Cookie(ACCESS_COOKIE, "stale-token"));

        HttpServletRequest forwarded = passThrough(request);

        assertThat(forwarded.getHeader(HttpHeaders.AUTHORIZATION)).isNull();
    }

    @Test
    void aRequestWithNoCookiesIsForwardedUnchanged() throws Exception {
        HttpServletRequest forwarded = passThrough(request("/api/v1/applications"));

        assertThat(forwarded.getHeader(HttpHeaders.AUTHORIZATION)).isNull();
    }

    @Test
    void anUnrelatedCookieIsIgnored() throws Exception {
        MockHttpServletRequest request = request("/api/v1/applications");
        request.setCookies(new Cookie("jr_refresh", "refresh-token"), new Cookie("theme", "dark"));

        HttpServletRequest forwarded = passThrough(request);

        assertThat(forwarded.getHeader(HttpHeaders.AUTHORIZATION)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void anEmptyCookieValueDoesNotProduceABearerHeader(String value) throws Exception {
        // "Bearer " with nothing after it would reach the resource server as a malformed token.
        MockHttpServletRequest request = request("/api/v1/applications");
        request.setCookies(new Cookie(ACCESS_COOKIE, value));

        HttpServletRequest forwarded = passThrough(request);

        assertThat(forwarded.getHeader(HttpHeaders.AUTHORIZATION)).isNull();
    }

    @Test
    void theCookieIsMatchedByExactName() throws Exception {
        // A prefix match would let "jr_access_stale" authenticate a request.
        MockHttpServletRequest request = request("/api/v1/applications");
        request.setCookies(new Cookie("jr_access_other", "the-jwt"));

        HttpServletRequest forwarded = passThrough(request);

        assertThat(forwarded.getHeader(HttpHeaders.AUTHORIZATION)).isNull();
    }
}
