-- V3__demands_quote_fields.sql
-- 需求单报价闭环：记录报价员工、报价时间
ALTER TABLE demands
    ADD COLUMN quoted_by  BIGINT NULL,
    ADD COLUMN quoted_at  DATETIME NULL;
ALTER TABLE demands
    ADD CONSTRAINT fk_demand_quoted_by FOREIGN KEY (quoted_by) REFERENCES staff_profiles (id);