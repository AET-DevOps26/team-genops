package com.jobready.email.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobready.email.config.SecurityConfig;
import com.jobready.email.exception.GlobalExceptionHandler;
import com.jobready.email.generated.modelDto.EmailConnectionStatus;
import com.jobready.email.generated.modelDto.EmailMessage;
import com.jobready.email.generated.modelDto.EmailMessageList;
import com.jobready.email.generated.modelDto.GmailAuthorizeResponse;
import com.jobready.email.service.EmailConnectionService;
import com.jobready.email.service.EmailMessageService;
import jakarta.servlet.http.Cookie;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests: the real SecurityFilterChain runs (JwtDecoder mocked), the services are
 * mocked. Asserts the HTTP contract: 401 without a token, 401 on a non-UUID subject, the
 * unauthenticated OAuth callback, and jr_access-cookie support.
 */
@WebMvcTest(EmailController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
// Boot 4's @WebMvcTest slice doesn't auto-configure servlet security (no HttpSecurity bean).
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
class EmailControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EmailConnectionService connectionService;

    @MockitoBean
    private EmailMessageService messageService;

    @MockitoBean
    private JwtDecoder jwtDecoder; // satisfies the resource-server config without a JWKS endpoint

    private final UUID userId = UUID.randomUUID();

    @Test
    void withoutToken_rejectsWith401() throws Exception {
        mvc.perform(get("/api/v1/email/connections")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/email/connections/gmail/authorize")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/email/messages")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/email/connections")).andExpect(status().isUnauthorized());
    }

    @Test
    void nonUuidSubject_rejectsWith401() throws Exception {
        mvc.perform(get("/api/v1/email/connections").with(jwt().jwt(j -> j.subject("not-a-uuid"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authorize_returnsConsentUrl() throws Exception {
        when(connectionService.authorize(userId))
                .thenReturn(new GmailAuthorizeResponse()
                        .authorizationUrl(URI.create("https://accounts.google.com/o/oauth2/auth?x=1")));

        mvc.perform(post("/api/v1/email/connections/gmail/authorize")
                        .with(jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorization_url").value("https://accounts.google.com/o/oauth2/auth?x=1"));
    }

    @Test
    void callback_isReachableWithoutTokenAndRedirects() throws Exception {
        when(connectionService.handleCallback("the-code", "the-state"))
                .thenReturn(URI.create("http://localhost:5173?email_connected=1"));

        mvc.perform(get("/api/v1/email/connections/gmail/callback")
                        .param("code", "the-code")
                        .param("state", "the-state"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://localhost:5173?email_connected=1"));
    }

    @Test
    void callback_withoutParams_returns400() throws Exception {
        mvc.perform(get("/api/v1/email/connections/gmail/callback"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"));
    }

    @Test
    void status_acceptsJrAccessCookie() throws Exception {
        Jwt jwt = Jwt.withTokenValue("cookie-token")
                .header("alg", "RS256")
                .subject(userId.toString())
                .build();
        when(jwtDecoder.decode("cookie-token")).thenReturn(jwt);
        when(connectionService.getStatus(userId)).thenReturn(new EmailConnectionStatus().connected(false));

        mvc.perform(get("/api/v1/email/connections").cookie(new Cookie("jr_access", "cookie-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(false));
    }

    @Test
    void listMessages_returnsPageWithDefaults() throws Exception {
        when(messageService.list(userId, 20, 0))
                .thenReturn(new EmailMessageList()
                        .items(List.of(new EmailMessage().messageId("m1").subject("Hi")))
                        .limit(20)
                        .offset(0));

        mvc.perform(get("/api/v1/email/messages").with(jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].message_id").value("m1"))
                .andExpect(jsonPath("$.limit").value(20))
                .andExpect(jsonPath("$.offset").value(0));
    }

    @Test
    void listMessages_rejectsOutOfRangeLimit() throws Exception {
        mvc.perform(get("/api/v1/email/messages")
                        .param("limit", "500")
                        .with(jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void disconnect_returns204() throws Exception {
        mvc.perform(delete("/api/v1/email/connections").with(jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isNoContent());
        verify(connectionService).disconnect(userId);
    }
}
