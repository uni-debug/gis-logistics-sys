-- V6__im_sessions_created_at.sql
-- im_sessions 增加 created_at（实体校验对齐）
ALTER TABLE im_sessions
    ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;