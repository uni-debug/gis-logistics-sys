-- V4__reviews_deleted_flag.sql
-- 评论软删标志位（恶意评论删除）
ALTER TABLE reviews
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
