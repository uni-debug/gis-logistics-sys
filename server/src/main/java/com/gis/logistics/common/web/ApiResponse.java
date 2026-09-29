package com.gis.logistics.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gis.logistics.common.errorcode.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应包装：{ code, data, message }。code=0 成功，非 0 为 5 位错误码。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int code;
    private T data;
    private String message;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS, data, null);
    }

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(ErrorCode.SUCCESS, null, null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, null, message);
    }
}
