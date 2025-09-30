package com.ds.gateway.common.exceptions;

/**
 * Exception thrown when user is not authenticated
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
