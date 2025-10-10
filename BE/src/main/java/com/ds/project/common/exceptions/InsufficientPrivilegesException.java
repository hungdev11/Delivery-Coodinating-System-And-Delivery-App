package com.ds.project.common.exceptions;

/**
 * Exception thrown when user lacks required privileges/roles
 */
public class InsufficientPrivilegesException extends RuntimeException {
    
    public InsufficientPrivilegesException(String message) {
        super(message);
    }
    
    public InsufficientPrivilegesException(String message, Throwable cause) {
        super(message, cause);
    }
}
