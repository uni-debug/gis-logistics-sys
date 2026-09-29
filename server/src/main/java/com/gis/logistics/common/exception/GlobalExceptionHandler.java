package com.gis.logistics.common.exception;

import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.log.MdcUtils;
import com.gis.logistics.common.web.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器：统一捕获并包装为 ApiResponse，注入 traceId 日志。
 * 关键路径（支付/权限/脱敏/GIS）异常一律 error 级 + traceId + 业务单号。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleBiz(BizException ex) {
        log.error("biz exception code={} traceId={} msg={}",
                ex.getCode(), MdcUtils.getTraceId(), ex.getMessage());
        return ApiResponse.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("validation failed traceId={} detail={}", MdcUtils.getTraceId(), detail);
        return ApiResponse.error(ErrorCode.GENERIC_BAD_REQUEST, detail);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDenied(AccessDeniedException ex) {
        log.error("access denied traceId={}", MdcUtils.getTraceId());
        return ApiResponse.error(ErrorCode.ROLE_FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(NoHandlerFoundException ex) {
        log.warn("404 traceId={}", MdcUtils.getTraceId());
        return ApiResponse.error(ErrorCode.GENERIC_NOT_FOUND, "resource not found");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGeneric(Exception ex) {
        log.error("unhandled exception traceId={}", MdcUtils.getTraceId(), ex);
        return ApiResponse.error(ErrorCode.GENERIC_INTERNAL, "internal error");
    }
}

