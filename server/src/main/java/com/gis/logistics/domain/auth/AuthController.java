package com.gis.logistics.domain.auth;

import com.gis.logistics.common.web.ApiResponse;
import com.gis.logistics.domain.user.User;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<User> register(@RequestBody @jakarta.validation.Valid RegisterBody body) {
        return ApiResponse.ok(authService.register(new AuthService.RegisterRequest(
                body.phone(), body.name(), body.password())));
    }

    @PostMapping("/login")
    public ApiResponse<AuthService.LoginResponse> login(@RequestBody @jakarta.validation.Valid LoginBody body) {
        return ApiResponse.ok(authService.login(new AuthService.LoginRequest(body.phone(), body.password(), body.expectedRole())));
    }

    @GetMapping("/me")
    public ApiResponse<AuthService.MeResponse> me(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        var user = authService.me(userId);
        return ApiResponse.ok(new AuthService.MeResponse(user.getId(), user.getName(), user.getRole().name()));
    }

    public record RegisterBody(
            @NotBlank String phone,
            @NotBlank String name,
            @NotBlank String password) {}

    public record LoginBody(
            @NotBlank String phone,
            @NotBlank String password,
            String expectedRole) {}
}
