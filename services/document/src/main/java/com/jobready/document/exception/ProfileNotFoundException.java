package com.jobready.document.exception;

/**
 * Thrown when the authenticated user has no profile yet. Surfaces as 404 — the frontend and the
 * genai service use this to detect that onboarding is still needed.
 */
public class ProfileNotFoundException extends RuntimeException {
    public ProfileNotFoundException() {
        super("Profile not found");
    }
}
