package com.ds.session.session_service.common.exceptions;

public class ResourceNotFound extends RuntimeException{
    public ResourceNotFound(String string) {
        super(string);
    }
}
