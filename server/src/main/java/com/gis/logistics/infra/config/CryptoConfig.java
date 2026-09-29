package com.gis.logistics.infra.config;

import com.gis.logistics.common.crypto.PhoneCipher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

/**
 * 手机号加密密钥装配（数据安全关键路径，需人工复核）。
 * 密钥仅从环境变量 PHONE_ENCRYPTION_KEY 注入，禁止入库/进日志；缺失则启动失败（fail-fast），
 * 避免使用随机兜底密钥导致已加密数据在重启后不可逆。
 */
@Configuration
public class CryptoConfig {

    private static final int KEY_LEN = 32;

    @Bean
    public PhoneCipher phoneCipher() {
        String key = System.getenv("PHONE_ENCRYPTION_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "PHONE_ENCRYPTION_KEY env var is required to start the service");
        }
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        // 归一化为固定 32 字节：不足补 0、超出截断。生产应直接提供 32 字节密钥。
        byte[] normalized = new byte[KEY_LEN];
        System.arraycopy(keyBytes, 0, normalized, 0, Math.min(keyBytes.length, KEY_LEN));
        return PhoneCipher.fromKeyBytes(normalized);
    }
}
