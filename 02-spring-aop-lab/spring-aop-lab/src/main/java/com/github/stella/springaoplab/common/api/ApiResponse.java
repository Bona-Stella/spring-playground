package com.github.stella.springaoplab.common.api;

import java.time.ZonedDateTime;

public record ApiResponse<T>(
        boolean success,
        int status,
        String code,
        String message,
        T data,
        String timestamp,
        String path
) {

    public static <T> ApiResponse<T> success(T data, String path) {
        return new ApiResponse<>(
                true,
                200,
                "OK",
                "Success",
                data,
                ZonedDateTime.now().toString(),
                path
        );
    }

    public static <T> ApiResponse<T> created(T data, String path) {
        return new ApiResponse<>(
                true,
                201,
                "CREATED",
                "Resource created successfully.",
                data,
                ZonedDateTime.now().toString(),
                path
        );
    }

    public static <T> ApiResponse<T> of(int status, String code, String message, T data, String path) {
        return new ApiResponse<>(
                true,
                status,
                code,
                message,
                data,
                ZonedDateTime.now().toString(),
                path
        );
    }
}
