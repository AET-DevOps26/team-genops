package com.jobready.application.exception;

/**
 * Thrown when an application does not exist for the authenticated user. Surfaces as 404 — we never
 * distinguish "not found" from "not yours", so users cannot probe for others' application IDs.
 */
public class ApplicationNotFoundException extends RuntimeException {
    public ApplicationNotFoundException() {
        super("Application not found");
    }
}
