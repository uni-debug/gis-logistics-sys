-- V8__payments_paid_at.sql
-- payments 增加 paid_at
ALTER TABLE payments
    ADD COLUMN paid_at DATETIME NULL;