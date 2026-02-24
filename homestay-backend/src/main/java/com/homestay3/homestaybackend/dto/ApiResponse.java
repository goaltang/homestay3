package com.homestay3.homestaybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private Integer code;        // HTTP 状态码
    private Long timestamp;      // 时间戳（毫秒）

    // 成功响应的静态方法
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "操作成功", data, 200, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, 200, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null, 200, System.currentTimeMillis());
    }

    // 失败响应的静态方法
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, null, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, message, data, null, System.currentTimeMillis());
    }

    // 带 HTTP 状态码的错误响应
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(false, message, null, code, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return new ApiResponse<>(false, message, data, code, System.currentTimeMillis());
    }
}