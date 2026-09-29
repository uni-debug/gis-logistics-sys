package com.gis.logistics.common.web;

import com.gis.logistics.common.log.MdcUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 每个请求入口注入唯一 traceId 到 MDC，并回写响应头 X-Trace-Id 便于前后端链路排查。
 * 请求结束清空 MDC，避免线程复用串号。
 */
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String inbound = request.getHeader(TRACE_HEADER);
        String traceId = (inbound == null || inbound.isBlank()) ? MdcUtils.newTraceId() : inbound;
        MdcUtils.putTraceId(traceId);
        response.setHeader(TRACE_HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MdcUtils.clear();
        }
    }
}
