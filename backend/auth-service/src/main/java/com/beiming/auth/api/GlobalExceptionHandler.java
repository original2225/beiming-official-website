package com.beiming.auth.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiResponse<Object>> handleApiException(ApiException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.status())
                .body(ApiResponse.error(exception.code(), exception.getMessage(), exception.errors(), requestId(request)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<ApiError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError(error.getField(), error.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(40001, "invalid request", errors, requestId(request)));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(50000, "internal server error", List.of(), requestId(request)));
    }

    private String requestId(HttpServletRequest request) {
        Object requestId = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        return requestId == null ? null : requestId.toString();
    }
}
