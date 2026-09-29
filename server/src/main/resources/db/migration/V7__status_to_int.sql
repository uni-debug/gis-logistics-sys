-- V7__status_to_int.sql
-- status 列 TINYINT -> INT，与实体 Integer 对齐
ALTER TABLE users        MODIFY COLUMN status INT NOT NULL DEFAULT 1;
ALTER TABLE sites        MODIFY COLUMN status INT NOT NULL DEFAULT 1;
ALTER TABLE staff_profiles MODIFY COLUMN status INT NOT NULL DEFAULT 1;
ALTER TABLE notices      MODIFY COLUMN status INT NOT NULL DEFAULT 1;