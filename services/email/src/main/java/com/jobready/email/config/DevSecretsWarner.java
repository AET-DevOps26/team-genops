package com.jobready.email.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Warns loudly at startup when the insecure dev-only secrets are in use — parity with the old
 * Python service's {@code warn_on_dev_secrets}. Dev defaults keep local runs frictionless, but
 * any deployed environment MUST set real values for {@code EMAIL_TOKEN_ENC_KEY} and
 * {@code STATE_SIGNING_KEY}.
 */
@Component
public class DevSecretsWarner {

    static final String DEV_DEFAULT = "dev-only-change-me";

    private static final Logger log = LoggerFactory.getLogger(DevSecretsWarner.class);

    private final EmailProperties properties;

    public DevSecretsWarner(EmailProperties properties) {
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warnOnDevSecrets() {
        if (DEV_DEFAULT.equals(properties.tokenEncKey())) {
            log.warn("EMAIL_TOKEN_ENC_KEY is the insecure dev default — set a real key in any deployed environment");
        }
        if (DEV_DEFAULT.equals(properties.state().signingKey())) {
            log.warn("STATE_SIGNING_KEY is the insecure dev default — set a real key in any deployed environment");
        }
    }
}
