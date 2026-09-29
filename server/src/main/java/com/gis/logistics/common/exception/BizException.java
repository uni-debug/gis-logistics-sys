package com.gis.logistics.common.exception;

import lombok.Getter;

/**
 * 业务异常：携带 5 位错误码与可选业务数据。
 * 由全局异常处理器统一捕获并包装为 { code, data, message }。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;
    private final Object data;

    public BizException(int code, String message) {
        this(code, message, null);
    }

    public BizException(int code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    public static BizException of(int code, String message, Object data) {
        return new BizException(code, message, data);
    }
}
