package com.jobready.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobready.application.config.SecurityConfig;
import com.jobready.application.config.WebConfig;
import com.jobready.application.exception.ApplicationNotFoundException;
import com.jobready.application.exception.GlobalExceptionHandler;
import com.jobready.application.generated.modelDto.ApplicationStage;
import com.jobready.application.generated.modelDto.JobApplication;
import com.jobready.application.service.ApplicationService;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests: the real SecurityFilterChain runs (JwtDecoder mocked), the service is mocked.
 * Owner isolation lives in the service/repository (userId-scoped queries → 404), so here we
 * assert the HTTP contract: 401 without a token, 401 on a non-UUID subject, 404 pass-through.
 */
@WebMvcTest(ApplicationController.class)
@Import({SecurityConfig.class, WebConfig.class, GlobalExceptionHandler.class})
// Boot 4's @WebMvcTest slice doesn't auto-configure servlet security (no HttpSecurity bean).
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
class ApplicationControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ApplicationService applicationService;

    @MockitoBean
    private JwtDecoder jwtDecoder; // satisfies the resource-server config without a JWKS endpoint

    private final UUID userId = UUID.randomUUID();

    @Test
    void withoutToken_rejectsWith401() throws Exception {
        mvc.perform(get("/api/v1/applications")).andExpect(status().isUnauthorized());
    }

    @Test
    void nonUuidSubject_rejectsWith401() throws Exception {
        mvc.perform(get("/api/v1/applications").with(jwt().jwt(j -> j.subject("not-a-uuid"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_returns201WithBody() throws Exception {
        when(applicationService.create(eq(userId), any()))
                .thenReturn(new JobApplication()
                        .id(UUID.randomUUID())
                        .company("Acme")
                        .jobTitle("Engineer")
                        .stage(ApplicationStage.DRAFT)
                        .appliedAt(OffsetDateTime.now())
                        .updatedAt(OffsetDateTime.now()));

        mvc.perform(
                        post("/api/v1/applications")
                                .with(jwt().jwt(j -> j.subject(userId.toString())))
                                .contentType("application/json")
                                .content(
                                        "{\"company\":\"Acme\",\"job_title\":\"Engineer\",\"job_description\":\"Build things\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.company").value("Acme"));
    }

    @Test
    void create_blankCompany_returns422() throws Exception {
        mvc.perform(post("/api/v1/applications")
                        .with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType("application/json")
                        .content("{\"company\":\"\",\"job_title\":\"Engineer\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void foreignApplication_surfacesAs404() throws Exception {
        UUID foreignId = UUID.randomUUID();
        when(applicationService.get(userId, foreignId)).thenThrow(new ApplicationNotFoundException());

        mvc.perform(get("/api/v1/applications/{id}", foreignId).with(jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("APPLICATION_NOT_FOUND"));
    }

    @Test
    void recommendationsOnForeignApplication_surfaceAs404() throws Exception {
        UUID foreignId = UUID.randomUUID();
        when(applicationService.listRecommendations(userId, foreignId)).thenThrow(new ApplicationNotFoundException());

        mvc.perform(get("/api/v1/applications/{id}/recommendations", foreignId)
                        .with(jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRecommendation_returns204() throws Exception {
        mvc.perform(delete("/api/v1/applications/{id}/recommendations/{rid}", UUID.randomUUID(), UUID.randomUUID())
                        .with(jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isNoContent());
    }

    @Test
    void list_bindsLowercaseStageQueryParam() throws Exception {
        when(applicationService.list(eq(userId), eq(ApplicationStage.FOLLOW_UP), eq(1), eq(0)))
                .thenReturn(new com.jobready.application.generated.modelDto.ApplicationList().total(0L));

        mvc.perform(get("/api/v1/applications?stage=follow_up&limit=1&offset=0")
                        .with(jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isOk());
    }

    @Test
    void summary_isReachableAndNotShadowedByIdRoute() throws Exception {
        when(applicationService.summary(userId))
                .thenReturn(new com.jobready.application.generated.modelDto.ApplicationSummary()
                        .applied(1L)
                        .followUp(0L)
                        .interview(0L)
                        .offer(0L)
                        .closed(0L)
                        .total(1L));

        mvc.perform(get("/api/v1/applications/summary").with(jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
    }
}
