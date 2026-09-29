package com.gis.logistics.common.security;

import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.web.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * Spring Security 装配（权限关键路径，需人工复核）。
 * - 无状态会话 + CSRF 关闭（REST + JWT）
 * - 放行 /auth/** 与 /error；按 role 拦截三端与 GIS 公共接口
 * - 401/403 统一返回 ApiResponse
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, ObjectMapper objectMapper) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.deny()))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(this::onUnauthorized)
                        .accessDeniedHandler(this::onForbidden))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/api/v1/auth/**", "/error", "/actuator/health").permitAll()
                        .requestMatchers("/api/v1/user/**").hasRole("USER")
                        .requestMatchers("/api/v1/staff/**").hasRole("STAFF")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/gis/**").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void onUnauthorized(jakarta.servlet.http.HttpServletRequest request,
                                jakarta.servlet.http.HttpServletResponse response,
                                org.springframework.security.core.AuthenticationException ex) throws IOException {
        writeJson(response, HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_REQUIRED, "authentication required");
    }

    private void onForbidden(jakarta.servlet.http.HttpServletRequest request,
                             jakarta.servlet.http.HttpServletResponse response,
                             org.springframework.security.access.AccessDeniedException ex) throws IOException {
        writeJson(response, HttpStatus.FORBIDDEN, ErrorCode.ROLE_FORBIDDEN, "forbidden");
    }

    private void writeJson(jakarta.servlet.http.HttpServletResponse response,
                           HttpStatus status, int code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        ApiResponse<Void> body = ApiResponse.error(code, message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

