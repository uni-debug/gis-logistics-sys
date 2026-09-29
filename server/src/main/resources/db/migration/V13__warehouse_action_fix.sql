-- 修正 V11 种子数据：action 值与实体枚举对齐（INBOUND->IN, OUTBOUND->OUT）
UPDATE warehouse_records SET action = 'IN' WHERE action = 'INBOUND';
UPDATE warehouse_records SET action = 'OUT' WHERE action = 'OUTBOUND';

-- 手动出入库不关联订单，order_id 允许为空
ALTER TABLE warehouse_records MODIFY order_id bigint NULL;
