-- V2__users_password_hash.sql
-- 为 users 增加 BCrypt 密码哈希列（注册/登录鉴权支撑）
ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(100) NULL;
