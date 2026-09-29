package com.gis.logistics.common.security;

import com.gis.logistics.common.exception.BizException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 三端 JWT 身份校验（权限关键路径，需人工复核）。
 * 默认仅接受 Authorization: Bearer header；仅对 SSE 端点 (?track-stream)
 * 额外允许 ?token= query 参数（EventSource 无法自定义 header）。
 * 无效/缺失 token 不在此拦截（交由 Spring Security 配置按路径放行或 401）。
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null) {
            try {
                JwtService.Claims claims = jwtService.parse(token);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                claims.getUserId(),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + claims.getRole().toUpperCase())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (BizException ignored) {
                // 无效 token 不设置认证，由后续路径守卫决定是否 401/403
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * 提取 JWT：优先 Authorization: Bearer header；
     * 仅当请求路径以 "/track-stream" 结尾时，回落到 ?token= query 参数。
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        if (request.getRequestURI().endsWith("/track-stream")) {
            String queryToken = request.getParameter("token");
            if (queryToken != null && !queryToken.isBlank()) {
                return queryToken;
            }
        }
        return null;
    }
}
