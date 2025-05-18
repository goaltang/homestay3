package com.homestay3.homestaybackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ResourceInUseException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

    public ResourceInUseException(String message) {
        super(message);
    }
    
    public ResourceInUseException(String message, Throwable cause) {
        super(message, cause);
    }
} 