package com.beiming.auth.api;

import java.util.List;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final int code;
    private final HttpStatus status;
    private final List<ApiError> errors;

    public ApiException(int code, HttpStatus status, String message) {
        this(code, status, message, List.of());
    }

    public ApiException(int code, HttpStatus status, String message, List<ApiError> errors) {
        super(message);
        this.code = code;
        this.status = status;
        this.errors = errors;
    }

    public int code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    public List<ApiError> errors() {
        return errors;
    }
}
