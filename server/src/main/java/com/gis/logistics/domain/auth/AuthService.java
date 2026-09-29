package com.gis.logistics.domain.auth;

import com.gis.logistics.common.crypto.PhoneCipher;
import com.gis.logistics.common.crypto.PhoneHasher;
import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.exception.BizException;
import com.gis.logistics.common.security.JwtService;
import com.gis.logistics.domain.user.User;
import com.gis.logistics.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PhoneCipher phoneCipher;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(RegisterRequest req) {
        String phoneHash = PhoneHasher.sha256Hex(req.phone());
        if (userRepository.existsByPhoneHash(phoneHash)) {
            throw new BizException(ErrorCode.GENERIC_BAD_REQUEST, "phone already registered");
        }
        User user = new User();
        user.setPhoneHash(phoneHash);
        user.setPhoneEnc(phoneCipher.encrypt(req.phone()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setName(req.name());
        user.setRole(User.Role.USER);
        user.setStatus(1);
        return userRepository.save(user);
    }

    public LoginResponse login(LoginRequest req) {
        String phoneHash = PhoneHasher.sha256Hex(req.phone());
        User user = userRepository.findByPhoneHash(phoneHash)
                .orElseThrow(() -> new BizException(ErrorCode.AUTH_TOKEN_INVALID, "user not found"));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.AUTH_TOKEN_INVALID, "invalid credentials");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BizException(ErrorCode.ROLE_FORBIDDEN, "account disabled");
        }
        // 方案A：前端所选端 role 仅作标记，真实角色以数据库账号为准；不匹配则拒绝登录（防越权）
        if (req.expectedRole() != null && !req.expectedRole().isBlank()) {
            String expected = req.expectedRole().trim().toUpperCase();
            String actual = user.getRole().name();
            if (!expected.equals(actual)) {
                throw new BizException(ErrorCode.ROLE_FORBIDDEN,
                        "role mismatch: account is " + actual + ", expected " + expected);
            }
        }
        String token = jwtService.issue(user.getId(), user.getRole().name());
        return new LoginResponse(token, user.getId(), user.getRole().name());
    }

    public User me(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.GENERIC_NOT_FOUND, "user not found: " + userId));

    }
    public record RegisterRequest(String phone, String name, String password) {}
    public record LoginRequest(String phone, String password, String expectedRole) {}
    public record LoginResponse(String token, Long userId, String role) {}
    public record MeResponse(Long userId, String name, String role) {}
}

