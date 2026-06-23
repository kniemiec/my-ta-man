package com.mytaman.service;

/** Maps to HTTP 409 (e.g. deleting a project that still has tasks). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
