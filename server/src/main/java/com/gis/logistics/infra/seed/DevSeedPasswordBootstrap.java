package com.gis.logistics.infra.seed;

import com.gis.logistics.common.crypto.PhoneHasher;
import com.gis.logistics.domain.user.User;
import com.gis.logistics.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Dev-only bootstrap: backfills well-known bcrypt password hashes for the 3 seed
 * accounts so the three front-ends can log in out-of-the-box.
 * <p>
 * 预生成的 bcrypt($2a$10$) 值与 R__seed.sql 兜底逻辑一致（同一批 dev 密码），
 * 运行于每次启动，幂等；prod profile 直接跳过，生产账号密码必须走注册/登录流程。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DevSeedPasswordBootstrap implements ApplicationRunner {

    /** seed account phone_hash(sha256) -> bcrypt hash 预生成常量（dev 专用）。 */
    private static final List<String[]> SEED_ACCOUNTS = List.of(
            // 13800000001 USER  smoke123456
            new String[]{PhoneHasher.sha256Hex("13800000001"),
                    "$2a$10$BWNdCzgC5huvpE5D9WzgBOuZJENSieH8Gho96JglgJRquWo524YMG"},
            // 13800000002 STAFF  staff123456
            new String[]{PhoneHasher.sha256Hex("13800000002"),
                    "$2a$10$GxndFU05To5/jGWDtqrfzOGsklWMhAuRTuGz9789wuXo6Hh1ONcci"},
            // 13800000003 ADMIN  admin123456
            new String[]{PhoneHasher.sha256Hex("13800000003"),
                    "$2a$10$IWS1/AxoNAJsFi5sbcEoaOhRTMSVzAc9oOT/m9fQWY36XvDZl.yPG"});

    private final UserRepository userRepository;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            profiles = new String[]{"dev(default)"};
        }
        if (Arrays.asList(profiles).contains("prod")) {
            log.info("[seed] active profiles={} contains prod, skip dev password backfill", String.join(",", profiles));
            return;
        }
        int written = 0;
        for (String[] seed : SEED_ACCOUNTS) {
            String phoneHash = seed[0];
            String bcryptHash = seed[1];
            User user = userRepository.findByPhoneHash(phoneHash).orElse(null);
            if (user == null) {
                continue; // 账号由 V11 迁移负责插入，这里不造账号
            }
            if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
                user.setPasswordHash(bcryptHash);
                userRepository.save(user);
                written++;
                log.info("[seed] backfilled bcrypt password for seed account phoneHash={}...", phoneHash.substring(0, 8));
            }
        }
        log.info("[seed] dev password bootstrap complete, profiles={}, written={}", String.join(",", profiles), written);
    }
}
