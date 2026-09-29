-- 为订单补充起终点坐标（WGS-84），路线规划与地图展示直接使用
ALTER TABLE orders
    ADD COLUMN from_lon DECIMAL(9,6) NULL,
    ADD COLUMN from_lat DECIMAL(9,6) NULL,
    ADD COLUMN to_lon   DECIMAL(9,6) NULL,
    ADD COLUMN to_lat   DECIMAL(9,6) NULL;