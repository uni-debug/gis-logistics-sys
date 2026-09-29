package com.gis.logistics.domain.auth;

import com.gis.logistics.common.crypto.PhoneCipher;
import com.gis.logistics.common.errorcode.ErrorCode;
import com.gis.logistics.common.exception.BizException;
import com.gis.logistics.common.security.JwtService;
import com.gis.logistics.domain.user.User;
import com.gis.logistics.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private JwtService jwtService = new JwtService("unit-test-secret-key-0123456789", "gis-logistics", 3600);

    private final byte[] key = new byte[32];
    private PhoneCipher phoneCipher;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        new SecureRandom().nextBytes(key);
        phoneCipher = PhoneCipher.fromKeyBytes(key);
        authService = new AuthService(userRepository, phoneCipher, jwtService, passwordEncoder);
    }

    @Test
    void loginRoleMismatchRejected() {
        // 账号实际角色 USER，但前端期望 STAFF -> 拒绝登录（防越权）
        when(userRepository.findByPhoneHash(anyString())).thenAnswer(inv -> {
            User u = new User();
            u.setId(1L);
            u.setRole(User.Role.USER);
            u.setPhoneHash(inv.getArgument(0, String.class));
            u.setPasswordHash(passwordEncoder.encode("secret"));
            u.setStatus(1);
            return java.util.Optional.of(u);
        });
        assertThrows(com.gis.logistics.common.exception.BizException.class,
                () -> authService.login(new AuthService.LoginRequest("13812341234", "secret", "STAFF")));
    }

    @Test
    void registerPersistsUserWithEncryptedPhone() {
        when(userRepository.existsByPhoneHash(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        User saved = authService.register(new AuthService.RegisterRequest("13812341234", "tester", "secret"));

        assertEquals(1L, saved.getId());
        assertEquals(User.Role.USER, saved.getRole());
        assertNotNull(saved.getPhoneEnc());
        assertNotNull(saved.getPasswordHash());
        String decryptedPhone = phoneCipher.decrypt(new String(saved.getPhoneEnc(), java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("13812341234", decryptedPhone);
    }

    @Test
    void registerRejectsDuplicatePhone() {
        when(userRepository.existsByPhoneHash(anyString())).thenReturn(true);
        assertThrows(BizException.class,
                () -> authService.register(new AuthService.RegisterRequest("13812341234", "a", "b")));
    }

    @Test
    void loginSuccessIssuesToken() {
        User u = buildUser(5L, "13812341234", "secret", 1);
        when(userRepository.findByPhoneHash(anyString())).thenReturn(Optional.of(u));

        AuthService.LoginResponse resp =
                authService.login(new AuthService.LoginRequest("13812341234", "secret", null));
        assertNotNull(resp.token());
        assertEquals(5L, resp.userId());
        assertEquals("USER", resp.role());
    }

    @Test
    void loginWrongPasswordRejected() {
        User u = buildUser(5L, "13812341234", "secret", 1);
        when(userRepository.findByPhoneHash(anyString())).thenReturn(Optional.of(u));
        assertThrows(BizException.class,
                () -> authService.login(new AuthService.LoginRequest("13812341234", "nope", null)));
    }

    @Test
    void loginDisabledAccountRejected() {
        User u = buildUser(5L, "13812341234", "secret", 0);
        when(userRepository.findByPhoneHash(anyString())).thenReturn(Optional.of(u));
        BizException ex = assertThrows(BizException.class,
                () -> authService.login(new AuthService.LoginRequest("13812341234", "secret", null)));
        assertEquals(ErrorCode.ROLE_FORBIDDEN, ex.getCode());
    }

    private User buildUser(Long id, String phone, String password, int status) {
        User u = new User();
        u.setId(id);
        u.setPhoneHash(com.gis.logistics.common.crypto.PhoneHasher.sha256Hex(phone));
        u.setPhoneEnc(phoneCipher.encrypt(phone).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setStatus(status);
        u.setRole(User.Role.USER);
        return u;
    }
}



