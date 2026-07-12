package com.jobready.document.exception;

/**
 * Thrown when a profile sub-resource or generated document does not exist for the authenticated
 * user. Surfaces as 404 — we never distinguish "not found" from "not yours", so users cannot
 * probe for other users' resource IDs.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String what) {
        super(what + " not found");
    }
}
