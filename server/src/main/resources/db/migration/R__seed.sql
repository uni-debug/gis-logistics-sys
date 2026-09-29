-- R__seed.sql : 账号/站点兜底插入（幂等，无密码）
-- 账号/站点/档案的插入主责在 V11__seed_business.sql（版本化，先于本 repeatable 执行）。
-- 本 repeatable 仅做安全网：若账号/站点缺失则补插（与 V11 一致，phone_enc 占位 0x00 128B）。
-- bcrypt password_hash 兜底已迁移至 DevSeedPasswordBootstrap（Spring 层按 profile 决定写入，prod 跳过）。

-- 兜底：账号不存在时插入（与 V11 保持一致的 phone_hash / name / role）
INSERT INTO users (phone_enc, phone_hash, name, role, status)
VALUES
    (UNHEX(REPEAT('00',128)), SHA2('13800000001',256), '演示用户',   'USER',  1),
    (UNHEX(REPEAT('00',128)), SHA2('13800000002',256), '演示员工',   'STAFF', 1),
    (UNHEX(REPEAT('00',128)), SHA2('13800000003',256), '演示管理员', 'ADMIN', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sites (code, name, point, capacity, status) VALUES
    ('HUB_BJ', '北京中转站', ST_GeomFromText('POINT(116.4074 39.9042)'), 1000, 1),
    ('HUB_SH', '上海中转站', ST_GeomFromText('POINT(121.4737 31.2304)'), 800, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- bcrypt password_hash 兜底已迁移至 DevSeedPasswordBootstrap（Spring 层，按 profile 决定是否写入；prod 跳过）。
