package com.jobready.email.controller;

import com.jobready.email.generated.api.EmailApi;
import com.jobready.email.generated.modelDto.EmailConnectionStatus;
import com.jobready.email.generated.modelDto.EmailMessageList;
import com.jobready.email.generated.modelDto.GmailAuthorizeResponse;
import com.jobready.email.service.EmailConnectionService;
import com.jobready.email.service.EmailMessageService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmailController implements EmailApi {

    private final EmailConnectionService connectionService;
    private final EmailMessageService messageService;

    public EmailController(EmailConnectionService connectionService, EmailMessageService messageService) {
        this.connectionService = connectionService;
        this.messageService = messageService;
    }

    @Override
    public ResponseEntity<GmailAuthorizeResponse> authorizeGmail() {
        return ResponseEntity.ok(connectionService.authorize(currentUserId()));
    }

    @Override
    public ResponseEntity<Void> gmailOAuthCallback(String code, String state) {
        URI target = connectionService.handleCallback(code, state);
        return ResponseEntity.status(HttpStatus.FOUND).location(target).build();
    }

    @Override
    public ResponseEntity<EmailConnectionStatus> getEmailConnection() {
        return ResponseEntity.ok(connectionService.getStatus(currentUserId()));
    }

    @Override
    public ResponseEntity<Void> deleteEmailConnection() {
        connectionService.disconnect(currentUserId());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<EmailMessageList> listEmailMessages(Integer limit, Integer offset) {
        return ResponseEntity.ok(messageService.list(currentUserId(), limit, offset));
    }

    /** The owner is always the verified JWT subject — never taken from the request. */
    private UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new BadCredentialsException("Missing or invalid access token");
        }
        try {
            return UUID.fromString(jwtAuth.getToken().getSubject());
        } catch (RuntimeException e) {
            throw new BadCredentialsException("Token subject is not a valid user id");
        }
    }
}
