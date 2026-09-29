package com.gis.logistics.common.log;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * MDC traceId 工具：每个请求链路注入唯一 traceId，贯穿日志与关键路径异常。
 */
public final class MdcUtils {

    public static final String TRACE_ID_KEY = "traceId";

    private MdcUtils() {
    }

    public static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static void putTraceId(String traceId) {
        MDC.put(TRACE_ID_KEY, traceId);
    }

    public static String getTraceId() {
        String value = MDC.get(TRACE_ID_KEY);
        return value == null ? "-" : value;
    }

    public static void clear() {
        MDC.clear();
    }
}
