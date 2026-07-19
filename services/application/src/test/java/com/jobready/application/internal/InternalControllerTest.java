package com.jobready.application.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobready.application.config.SecurityConfig;
import com.jobready.application.config.WebConfig;
import com.jobready.application.exception.GlobalExceptionHandler;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for the internal machine-to-machine API: the static-token filter chain runs
 * for real, the service is mocked. Asserts the auth contract (401 without/with a wrong token)
 * and the request/response shape with the correct token.
 */
@WebMvcTest(InternalController.class)
@Import({SecurityConfig.class, WebConfig.class, GlobalExceptionHandler.class})
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
@TestPropertySource(properties = "internal.service-token=test-internal-token")
class InternalControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EmailUpdateService emailUpdateService;

    @MockitoBean
    private JwtDecoder jwtDecoder; // satisfies the resource-server config without a JWKS endpoint

    private final UUID userId = UUID.randomUUID();
    private final UUID applicationId = UUID.randomUUID();

    private static final String BODY =
            """
        {
          "userId": "%s",
          "sourceMessageId": "gmail-msg-1",
          "suggestedStage": "interview",
          "event": {
            "eventType": "interview_scheduled",
            "title": "Interview invitation",
            "description": "Acme invited you to interview",
            "occurredAt": "2026-07-14T10:00:00Z"
          },
          "recommendations": [
            {"insight": "Interview on Friday", "recommendedAction": "Prepare for the interview"}
          ]
        }""";

    @Test
    void withoutToken_rejectsWith401() throws Exception {
        mvc.perform(get("/internal/v1/users/" + userId + "/applications")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongToken_rejectsWith401() throws Exception {
        mvc.perform(get("/internal/v1/users/" + userId + "/applications").header("Authorization", "Bearer wrong-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listCandidates_withCorrectToken_returnsSlimApplications() throws Exception {
        when(emailUpdateService.listCandidates(userId))
                .thenReturn(List.of(
                        new InternalDtos.ApplicationCandidate(applicationId, "Acme", "Engineer", "applied", null)));

        mvc.perform(get("/internal/v1/users/" + userId + "/applications")
                        .header("Authorization", "Bearer test-internal-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].company").value("Acme"))
                .andExpect(jsonPath("$[0].stage").value("applied"));
    }

    @Test
    void emailUpdate_withCorrectToken_appliesAndReportsOutcome() throws Exception {
        when(emailUpdateService.apply(eq(applicationId), any())).thenReturn(true);

        mvc.perform(post("/internal/v1/applications/" + applicationId + "/email-update")
                        .header("Authorization", "Bearer test-internal-token")
                        .contentType("application/json")
                        .content(BODY.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(true));
    }

    @Test
    void emailUpdate_missingEvent_returns422() throws Exception {
        mvc.perform(post("/internal/v1/applications/" + applicationId + "/email-update")
                        .header("Authorization", "Bearer test-internal-token")
                        .contentType("application/json")
                        .content("{\"userId\":\"" + userId + "\",\"sourceMessageId\":\"m1\"}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
