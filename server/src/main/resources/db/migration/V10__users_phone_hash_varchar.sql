-- V10__users_phone_hash_varchar.sql
-- users.phone_hash CHAR(64) -> VARCHAR(255)，与实体 String 对齐
ALTER TABLE users
    MODIFY COLUMN phone_hash VARCHAR(255) NOT NULL;