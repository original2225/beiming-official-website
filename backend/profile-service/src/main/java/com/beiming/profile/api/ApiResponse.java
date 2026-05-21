package com.beiming.profile.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(int code, String message, T data, List<ApiError> errors, String requestId) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data, null, null);
    }

    public static ApiResponse<Object> error(int code, String message, List<ApiError> errors, String requestId) {
        return new ApiResponse<>(code, message, null, errors == null || errors.isEmpty() ? null : errors, requestId);
    }
}
