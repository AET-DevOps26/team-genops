package com.jobready.email.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-process record of consumed OAuth-state nonces (nonce → state expiry). Entries past their
 * expiry are pruned opportunistically — an expired state token fails validation anyway, so the
 * record is no longer needed. Single-instance only, like the poller; a multi-replica deployment
 * would move this to the shared Redis instance.
 */
@Component
public class NonceStore {

    private final Map<String, Instant> consumed = new ConcurrentHashMap<>();

    public boolean isConsumed(String nonce) {
        prune();
        return consumed.containsKey(nonce);
    }

    /**
     * Atomically reserve a nonce before starting fallible work (the code exchange), so two
     * concurrent callbacks replaying the same state can never both proceed. Returns false if the
     * nonce is already reserved/consumed.
     */
    public boolean reserve(String nonce, Instant expiresAt) {
        prune();
        return consumed.putIfAbsent(nonce, expiresAt) == null;
    }

    /** Release a reservation after a recoverable failure, leaving the state token reusable. */
    public void release(String nonce) {
        consumed.remove(nonce);
    }

    private void prune() {
        Instant now = Instant.now();
        consumed.entrySet().removeIf(e -> e.getValue().isBefore(now));
    }
}
