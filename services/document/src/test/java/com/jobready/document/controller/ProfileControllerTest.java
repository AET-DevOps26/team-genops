package com.jobready.document.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobready.document.config.SecurityConfig;
import com.jobready.document.exception.GlobalExceptionHandler;
import com.jobready.document.exception.ProfileNotFoundException;
import com.jobready.document.exception.ResourceNotFoundException;
import com.jobready.document.generated.modelDto.GeneratedDocument;
import com.jobready.document.generated.modelDto.GeneratedDocumentType;
import com.jobready.document.generated.modelDto.Profile;
import com.jobready.document.generated.modelDto.ProfileAggregateResponse;
import com.jobready.document.generated.modelDto.Skill;
import com.jobready.document.generated.modelDto.SkillLevel;
import com.jobready.document.service.DocumentService;
import java.time.OffsetDateTime;
import java.util.List;
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
 * assert the HTTP contract: 401 without a token, 401 on a non-UUID subject, the 404/422/400
 * mappings from {@link GlobalExceptionHandler}, and the 200/201/204 happy paths.
 */
@WebMvcTest(ProfileController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
// Boot 4's @WebMvcTest slice doesn't auto-configure servlet security (no HttpSecurity bean).
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)
class ProfileControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DocumentService documentService;

    @MockitoBean
    private JwtDecoder jwtDecoder; // satisfies the resource-server config without a JWKS endpoint

    private final UUID userId = UUID.randomUUID();

    // ------------------------------------------------------------------
    // Authentication contract
    // ------------------------------------------------------------------

    @Test
    void withoutToken_rejectsWith401() throws Exception {
        mvc.perform(get("/api/v1/profile")).andExpect(status().isUnauthorized());
    }

    @Test
    void nonUuidSubject_rejectsWith401() throws Exception {
        mvc.perform(get("/api/v1/profile").with(jwt().jwt(j -> j.subject("not-a-uuid"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // ------------------------------------------------------------------
    // Profile aggregate
    // ------------------------------------------------------------------

    @Test
    void getProfile_returns200WithAggregate() throws Exception {
        when(documentService.getProfile(userId))
                .thenReturn(new ProfileAggregateResponse()
                        .profile(new Profile(
                                UUID.randomUUID(), "Jane", "Doe", OffsetDateTime.now(), OffsetDateTime.now()))
                        .workExperiences(List.of())
                        .educations(List.of())
                        .skills(List.of())
                        .languages(List.of()));

        mvc.perform(get("/api/v1/profile").with(jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.first_name").value("Jane"));
    }

    @Test
    void getProfile_whenMissing_surfacesAs404() throws Exception {
        when(documentService.getProfile(userId)).thenThrow(new ProfileNotFoundException());

        mvc.perform(get("/api/v1/profile").with(jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_FOUND"));
    }

    @Test
    void upsertProfile_blankFirstName_returns422() throws Exception {
        mvc.perform(put("/api/v1/profile")
                        .with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType("application/json")
                        .content("{\"first_name\":\"\",\"last_name\":\"Doe\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void upsertProfile_malformedJson_returns400() throws Exception {
        mvc.perform(put("/api/v1/profile")
                        .with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType("application/json")
                        .content("{not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    // ------------------------------------------------------------------
    // Sub-resources
    // ------------------------------------------------------------------

    @Test
    void createSkill_returns201WithBody() throws Exception {
        when(documentService.createSkill(eq(userId), any()))
                .thenReturn(new Skill(UUID.randomUUID(), "Java", SkillLevel.ADVANCED, OffsetDateTime.now()));

        mvc.perform(post("/api/v1/profile/skills")
                        .with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType("application/json")
                        .content("{\"name\":\"Java\",\"level\":\"advanced\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Java"))
                .andExpect(jsonPath("$.level").value("advanced"));
    }

    @Test
    void deleteSkill_whenNotOwned_surfacesAs404() throws Exception {
        UUID foreignId = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Skill not found"))
                .when(documentService)
                .deleteSkill(userId, foreignId);

        mvc.perform(delete("/api/v1/profile/skills/{id}", foreignId).with(jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void deleteSkill_whenOwned_returns204() throws Exception {
        mvc.perform(delete("/api/v1/profile/skills/{id}", UUID.randomUUID())
                        .with(jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isNoContent());
    }

    @Test
    void nonUuidPathParam_returns400() throws Exception {
        mvc.perform(delete("/api/v1/profile/skills/{id}", "not-a-uuid")
                        .with(jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    // ------------------------------------------------------------------
    // Generated documents
    // ------------------------------------------------------------------

    @Test
    void createDocument_returns201() throws Exception {
        when(documentService.createDocument(eq(userId), any()))
                .thenReturn(new GeneratedDocument(
                        UUID.randomUUID(), GeneratedDocumentType.COVER_LETTER, "Dear team", OffsetDateTime.now()));

        mvc.perform(post("/api/v1/documents")
                        .with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType("application/json")
                        .content("{\"type\":\"cover_letter\",\"content\":\"Dear team\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("cover_letter"));
    }
}
