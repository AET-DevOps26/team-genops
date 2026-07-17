package com.jobready.email.service;

import com.jobready.email.generated.modelDto.EmailMessage;
import com.jobready.email.generated.modelDto.EmailMessageList;
import com.jobready.email.repository.ProcessedEmailRepository;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read side: list the user's stored emails, newest first. */
@Service
public class EmailMessageService {

    private final ProcessedEmailRepository processedEmailRepository;

    public EmailMessageService(ProcessedEmailRepository processedEmailRepository) {
        this.processedEmailRepository = processedEmailRepository;
    }

    @Transactional(readOnly = true)
    public EmailMessageList list(UUID userId, int limit, int offset) {
        List<EmailMessage> items = processedEmailRepository.findPageForUser(userId, limit, offset).stream()
                .map(e -> new EmailMessage()
                        .messageId(e.getMessageId())
                        .subject(e.getSubject())
                        .sender(e.getSender())
                        .snippet(e.getSnippet())
                        .receivedAt(
                                e.getReceivedAt() == null
                                        ? null
                                        : e.getReceivedAt().atOffset(ZoneOffset.UTC)))
                .toList();
        return new EmailMessageList().items(items).limit(limit).offset(offset);
    }
}
