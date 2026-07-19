package com.jobready.auth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class OriginValidationFilterTest {

    private static final String APP_ORIGIN = "https://jobready.stud.k8s.aet.cit.tum.de";

    private final OriginValidationFilter filter = new OriginValidationFilter(Set.of(APP_ORIGIN));

    private MockHttpServletResponse run(String method, String origin, String referer) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/v1/auth/logout");
        if (origin != null) {
            request.addHeader("Origin", origin);
        }
        if (referer != null) {
            request.addHeader("Referer", referer);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        // The chain's request is non-null only when the filter let the call through.
        response.setHeader("X-Chain-Invoked", String.valueOf(chain.getRequest() != null));
        return response;
    }

    private void assertAllowed(MockHttpServletResponse response) {
        assertTrue(Boolean.parseBoolean(response.getHeader("X-Chain-Invoked")));
        assertEquals(200, response.getStatus());
    }

    private void assertRejected(MockHttpServletResponse response) throws Exception {
        assertFalse(Boolean.parseBoolean(response.getHeader("X-Chain-Invoked")));
        assertEquals(403, response.getStatus());
        assertNotNull(response.getContentAsString());
        assertTrue(response.getContentAsString().contains("ORIGIN_FORBIDDEN"));
    }

    @Test
    void allowedOriginPasses() throws Exception {
        assertAllowed(run("POST", APP_ORIGIN, null));
    }

    @Test
    void foreignOriginIsRejected() throws Exception {
        assertRejected(run("POST", "https://evil.example.com", null));
    }

    @Test
    void samesiteSiblingSubdomainIsRejected() throws Exception {
        // The attack SameSite=Strict does NOT stop: another app on the shared TUM domain.
        assertRejected(run("POST", "https://other-team.stud.k8s.aet.cit.tum.de", null));
    }

    @Test
    void nullOpaqueOriginIsRejected() throws Exception {
        // Sandboxed iframes / privacy redirects send the literal string "null".
        assertRejected(run("POST", "null", null));
    }

    @Test
    void refererFallbackMatchesPrefix() throws Exception {
        assertAllowed(run("POST", null, APP_ORIGIN + "/applications"));
    }

    @Test
    void refererPrefixCannotBeSpoofedByLongerHost() throws Exception {
        // https://<allowed-host>.evil.com must not pass a naive startsWith check.
        assertRejected(run("POST", null, APP_ORIGIN + ".evil.com/x"));
    }

    @Test
    void foreignRefererIsRejected() throws Exception {
        assertRejected(run("POST", null, "https://evil.example.com/page"));
    }

    @Test
    void headerlessNonBrowserClientPasses() throws Exception {
        assertAllowed(run("POST", null, null));
    }

    @Test
    void safeMethodsSkipTheCheckEntirely() throws Exception {
        assertAllowed(run("GET", "https://evil.example.com", null));
        assertAllowed(run("OPTIONS", "https://evil.example.com", null));
    }

    @Test
    void putAndDeleteAreChecked() throws Exception {
        assertRejected(run("PUT", "https://evil.example.com", null));
        assertRejected(run("DELETE", "https://evil.example.com", null));
    }
}
