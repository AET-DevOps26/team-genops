package com.jobready.application.internal;

import com.jobready.application.internal.InternalDtos.ApplicationCandidate;
import com.jobready.application.internal.InternalDtos.EmailCreateRequest;
import com.jobready.application.internal.InternalDtos.EmailCreateResponse;
import com.jobready.application.internal.InternalDtos.EmailUpdateRequest;
import com.jobready.application.internal.InternalDtos.EmailUpdateResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Machine-to-machine API for the email service's application-detection pipeline. Guarded by
 * {@link InternalTokenFilter} (static service token) instead of user JWTs, so — uniquely in
 * this service — the user is identified explicitly by the caller. Not part of
 * {@code api/openapi.yaml}; see the service README for the contract.
 */
@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class InternalController {

    private final EmailUpdateService emailUpdateService;

    /** The user's applications, slimmed down to what email matching needs. */
    @GetMapping("/users/{userId}/applications")
    public ResponseEntity<List<ApplicationCandidate>> listCandidates(@PathVariable UUID userId) {
        return ResponseEntity.ok(emailUpdateService.listCandidates(userId));
    }

    /** Applies one email's derived update (stage + event + recommendations) atomically. */
    @PostMapping("/applications/{applicationId}/email-update")
    public ResponseEntity<EmailUpdateResponse> applyEmailUpdate(
            @PathVariable UUID applicationId, @Valid @RequestBody EmailUpdateRequest request) {
        return ResponseEntity.ok(new EmailUpdateResponse(emailUpdateService.apply(applicationId, request)));
    }

    /** Creates an application for a company the user does not track yet, derived from one email. */
    @PostMapping("/users/{userId}/applications/email-create")
    public ResponseEntity<EmailCreateResponse> createFromEmail(
            @PathVariable UUID userId, @Valid @RequestBody EmailCreateRequest request) {
        if (!userId.equals(request.userId())) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(emailUpdateService.createFromEmail(request));
    }
}
