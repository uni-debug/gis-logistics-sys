-- V11__seed_business.sql
-- 演示种子数据：账号 / 站点 / 员工档案 / 管理员档案 / 需求 / 订单 / 支付 / 路线 / 物流事件 / 仓库 / 公告 / 反馈
-- 自包含：Flyway 先跑 V1..V11（版本化）再跑 R__seed（repeatable），
-- 所以本迁移必须自己先把 3 个账号 + 2 个站点 + 2 个档案插好，业务数据才有 FK 可用。
-- 幂等：所有 INSERT 用 NOT EXISTS / ON DUPLICATE KEY 保护，重复执行安全。

-- 1) 站点
INSERT INTO sites (code, name, point, capacity, status) VALUES
    ('HUB_BJ', '北京中转站', ST_GeomFromText('POINT(116.4074 39.9042)'), 1000, 1),
    ('HUB_SH', '上海中转站', ST_GeomFromText('POINT(121.4737 31.2304)'), 800, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 2) 三个账号（phone_hash 为 SHA256 明文哈希，R__seed 会补 bcrypt password_hash）
INSERT INTO users (phone_enc, phone_hash, name, role, status, password_hash)
VALUES
    (UNHEX(REPEAT('00',128)), SHA2('13800000001',256), '演示用户',   'USER',  1, NULL),
    (UNHEX(REPEAT('00',128)), SHA2('13800000002',256), '演示员工',   'STAFF', 1, NULL),
    (UNHEX(REPEAT('00',128)), SHA2('13800000003',256), '演示管理员', 'ADMIN', 1, NULL)
ON DUPLICATE KEY UPDATE name = VALUES(name);

SET @user1 := (SELECT id FROM users WHERE phone_hash = SHA2('13800000001',256) LIMIT 1);

-- 3) 员工档案 / 管理员档案
INSERT INTO staff_profiles (user_id, site_id, license_no, delivery_area, status)
SELECT u.id, (SELECT id FROM sites WHERE code = 'HUB_BJ'), 'LIC-SEED-001',
       ST_GeomFromText('POINT(116.4074 39.9042)'), 1
FROM users u WHERE u.phone_hash = SHA2('13800000002',256)
  AND NOT EXISTS (SELECT 1 FROM staff_profiles sp WHERE sp.user_id = u.id);

INSERT INTO admin_profiles (user_id, perm_mask, is_super)
SELECT u.id, 0, TRUE
FROM users u WHERE u.phone_hash = SHA2('13800000003',256)
  AND NOT EXISTS (SELECT 1 FROM admin_profiles ap WHERE ap.user_id = u.id);

SET @staff1 := (SELECT id FROM staff_profiles WHERE user_id =
    (SELECT id FROM users WHERE phone_hash = SHA2('13800000002',256)) LIMIT 1);

-- 4) 需求单（3 条，BJ <-> SH 干线，覆盖 待接单/已报价/已受理）
INSERT INTO demands (user_id, title, weight_g, volume_cm3, fragile, origin_region, origin_addr,
                    target_region, target_addr, status, quoted_price, quoted_by, quoted_at, created_at)
SELECT @user1, '北京->上海 数码配件', 1500, 4000, 0, 'BJ', '北京市朝阳区望京 SOHO',
       'SH', '上海市浦东新区张江路', 'PENDING', 0, NULL, NULL, NOW()
WHERE NOT EXISTS (SELECT 1 FROM demands WHERE user_id = @user1 AND title = '北京->上海 数码配件');

INSERT INTO demands (user_id, title, weight_g, volume_cm3, fragile, origin_region, origin_addr,
                    target_region, target_addr, status, quoted_price, quoted_by, quoted_at, created_at)
SELECT @user1, '北京->上海 服装样品', 800, 2500, 0, 'BJ', '北京市朝阳区三里屯',
       'SH', '上海市黄浦区南京东路', 'QUOTED', 1280, @staff1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM demands WHERE user_id = @user1 AND title = '北京->上海 服装样品');

INSERT INTO demands (user_id, title, weight_g, volume_cm3, fragile, origin_region, origin_addr,
                    target_region, target_addr, status, quoted_price, quoted_by, quoted_at, created_at)
SELECT @user1, '上海->北京 精密仪器（易碎）', 3200, 8000, 1, 'SH', '上海市闵行区虹桥',
       'BJ', '北京市海淀区中关村', 'ACCEPTED', 2600, @staff1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM demands WHERE user_id = @user1 AND title = '上海->北京 精密仪器（易碎）');

-- 5) 订单（3 条：待支付 / 在途 / 已签收；带起终点坐标）
INSERT INTO orders (demand_id, staff_id, user_id, amount, status, version, paid_at, delivered_at,
                   from_lon, from_lat, to_lon, to_lat, created_at)
SELECT d.id, @staff1, @user1, 1280, 'PENDING', 0, NULL, NULL,
       116.4074, 39.9042, 121.4737, 31.2304, NOW()
FROM demands d WHERE d.title = '北京->上海 服装样品'
  AND NOT EXISTS (SELECT 1 FROM orders WHERE demand_id = d.id AND status = 'PENDING');

INSERT INTO orders (demand_id, staff_id, user_id, amount, status, version, paid_at, delivered_at,
                   from_lon, from_lat, to_lon, to_lat, created_at)
SELECT d.id, @staff1, @user1, 2600, 'IN_TRANSIT', 0, NOW() - INTERVAL 2 HOUR, NULL,
       121.4737, 31.2304, 116.4074, 39.9042, NOW() - INTERVAL 3 HOUR
FROM demands d WHERE d.title = '上海->北京 精密仪器（易碎）'
  AND NOT EXISTS (SELECT 1 FROM orders WHERE demand_id = d.id AND status = 'IN_TRANSIT');

INSERT INTO orders (demand_id, staff_id, user_id, amount, status, version, paid_at, delivered_at,
                   from_lon, from_lat, to_lon, to_lat, created_at)
SELECT d.id, @staff1, @user1, 990, 'DELIVERED', 1, NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 28 DAY,
       116.4074, 39.9042, 121.4737, 31.2304, NOW() - INTERVAL 30 DAY
FROM demands d WHERE d.title = '北京->上海 数码配件'
  AND NOT EXISTS (SELECT 1 FROM orders WHERE demand_id = d.id AND status = 'DELIVERED');

-- 6) 支付（在途 / 已签收 两单为 PAID；txn_no 全局唯一）
INSERT INTO payments (order_id, channel, txn_no, amount, status, paid_at, created_at)
SELECT o.id, 'ALIPAY', 'SEED-TXN-0002', 2600, 'PAID', NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 2 HOUR
FROM orders o
JOIN demands d ON d.id = o.demand_id
WHERE d.title = '上海->北京 精密仪器（易碎）' AND o.status = 'IN_TRANSIT'
  AND NOT EXISTS (SELECT 1 FROM payments p WHERE p.order_id = o.id);

INSERT INTO payments (order_id, channel, txn_no, amount, status, paid_at, created_at)
SELECT o.id, 'WECHAT', 'SEED-TXN-0003', 990, 'PAID', NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 30 DAY
FROM orders o
JOIN demands d ON d.id = o.demand_id
WHERE d.title = '北京->上海 数码配件' AND o.status = 'DELIVERED'
  AND NOT EXISTS (SELECT 1 FROM payments p WHERE p.order_id = o.id);

-- 7) 路线任务（在途 / 已签收 两单；BJ<->SH 干线 3 点折线）
INSERT INTO route_tasks (order_id, strategy, from_point, to_point, via_points, avoid_segments,
                        geometry, distance_m, eta_s, cost, created_at)
SELECT o.id, 'FASTEST',
       ST_GeomFromText('POINT(121.4737 31.2304)'),
       ST_GeomFromText('POINT(116.4074 39.9042)'),
       ST_GeomFromText('MULTIPOINT((118.7879 34.2601))'),
       NULL,
       ST_GeomFromText('LINESTRING(121.4737 31.2304, 118.7879 34.2601, 116.4074 39.9042)'),
       1067000, 4310, 213400, NOW() - INTERVAL 3 HOUR
FROM orders o
JOIN demands d ON d.id = o.demand_id
WHERE d.title = '上海->北京 精密仪器（易碎）' AND o.status = 'IN_TRANSIT'
  AND NOT EXISTS (SELECT 1 FROM route_tasks rt WHERE rt.order_id = o.id);

INSERT INTO route_tasks (order_id, strategy, from_point, to_point, via_points, avoid_segments,
                        geometry, distance_m, eta_s, cost, created_at)
SELECT o.id, 'SHORTEST',
       ST_GeomFromText('POINT(116.4074 39.9042)'),
       ST_GeomFromText('POINT(121.4737 31.2304)'),
       ST_GeomFromText('MULTIPOINT((118.7879 34.2601))'),
       NULL,
       ST_GeomFromText('LINESTRING(116.4074 39.9042, 118.7879 34.2601, 121.4737 31.2304)'),
       1067000, 4310, 213400, NOW() - INTERVAL 30 DAY
FROM orders o
JOIN demands d ON d.id = o.demand_id
WHERE d.title = '北京->上海 数码配件' AND o.status = 'DELIVERED'
  AND NOT EXISTS (SELECT 1 FROM route_tasks rt WHERE rt.order_id = o.id);

-- 8) 物流事件（在途单 3 点 + 已签收单 4 点）
INSERT INTO logistics_events (order_id, type, point, occurred_at, operator_id)
SELECT o.id, 'PICKED', ST_GeomFromText('POINT(121.4737 31.2304)'),
       NOW() - INTERVAL 2 HOUR, @staff1
FROM orders o
JOIN demands d ON d.id = o.demand_id
WHERE d.title = '上海->北京 精密仪器（易碎）' AND o.status = 'IN_TRANSIT'
  AND NOT EXISTS (SELECT 1 FROM logistics_events e WHERE e.order_id = o.id AND e.type = 'PICKED');

INSERT INTO logistics_events (order_id, type, point, occurred_at, operator_id)
SELECT o.id, 'IN_TRANSIT', ST_GeomFromText('POINT(117.1850 34.2599)'),
       NOW() - INTERVAL 1 HOUR, @staff1
FROM orders o
JOIN demands d ON d.id = o.demand_id
WHERE d.title = '上海->北京 精密仪器（易碎）' AND o.status = 'IN_TRANSIT'
  AND NOT EXISTS (SELECT 1 FROM logistics_events e WHERE e.order_id = o.id AND e.type = 'IN_TRANSIT');

INSERT INTO logistics_events (order_id, type, point, occurred_at, operator_id)
SELECT o.id, 'ARRIVED_DELIVERY', ST_GeomFromText('POINT(116.4074 39.9042)'),
       NOW() - INTERVAL 30 MINUTE, @staff1
FROM orders o
JOIN demands d ON d.id = o.demand_id
WHERE d.title = '上海->北京 精密仪器（易碎）' AND o.status = 'IN_TRANSIT'
  AND NOT EXISTS (SELECT 1 FROM logistics_events e WHERE e.order_id = o.id AND e.type = 'ARRIVED_DELIVERY');

INSERT INTO logistics_events (order_id, type, point, occurred_at, operator_id)
SELECT o.id, 'PICKED', ST_GeomFromText('POINT(116.4074 39.9042)'),
       NOW() - INTERVAL 30 DAY, @staff1
FROM orders o
JOIN demands d ON d.id = o.demand_id
WHERE d.title = '北京->上海 数码配件' AND o.status = 'DELIVERED'
  AND NOT EXISTS (SELECT 1 FROM logistics_events e WHERE e.order_id = o.id AND e.type = 'PICKED');

INSERT INTO logistics_events (order_id, type, point, occurred_at, operator_id)
SELECT o.id, 'IN_TRANSIT', ST_GeomFromText('POINT(117.1850 34.2599)'),
       NOW() - INTERVAL 29 DAY, @staff1
FROM orders o
JOIN demands d ON d.id = o.demand_id
WHERE d.title = '北京->上海 数码配件' AND o.status = 'DELIVERED'
  AND NOT EXISTS (SELECT 1 FROM logistics_events e WHERE e.order_id = o.id AND e.type = 'IN_TRANSIT');

INSERT INTO logistics_events (order_id, type, point, occurred_at, operator_id)
SELECT o.id, 'ARRIVED_DELIVERY', ST_GeomFromText('POINT(121.4737 31.2304)'),
       NOW() - INTERVAL 28 DAY - INTERVAL 6 HOUR, @staff1
FROM orders o
JOIN demands d ON d.id = o.demand_id
WHERE d.title = '北京->上海 数码配件' AND o.status = 'DELIVERED'
  AND NOT EXISTS (SELECT 1 FROM logistics_events e WHERE e.order_id = o.id AND e.type = 'ARRIVED_DELIVERY');

INSERT INTO logistics_events (order_id, type, point, occurred_at, operator_id)
SELECT o.id, 'DELIVERED', ST_GeomFromText('POINT(121.4737 31.2304)'),
       NOW() - INTERVAL 28 DAY, @staff1
FROM orders o
JOIN demands d ON d.id = o.demand_id
WHERE d.title = '北京->上海 数码配件' AND o.status = 'DELIVERED'
  AND NOT EXISTS (SELECT 1 FROM logistics_events e WHERE e.order_id = o.id AND e.type = 'DELIVERED');

-- 9) 仓库出入库（BJ/SH 两站各一条）
INSERT INTO warehouse_records (site_id, order_id, action, quantity, occurred_at)
SELECT s.id, o.id, 'INBOUND', 1, NOW() - INTERVAL 30 DAY
FROM orders o
JOIN demands d ON d.id = o.demand_id
JOIN sites s ON s.code = 'HUB_BJ'
WHERE d.title = '北京->上海 数码配件' AND o.status = 'DELIVERED'
  AND NOT EXISTS (SELECT 1 FROM warehouse_records w WHERE w.order_id = o.id AND w.site_id = s.id);

INSERT INTO warehouse_records (site_id, order_id, action, quantity, occurred_at)
SELECT s.id, o.id, 'OUTBOUND', 1, NOW() - INTERVAL 28 DAY
FROM orders o
JOIN demands d ON d.id = o.demand_id
JOIN sites s ON s.code = 'HUB_SH'
WHERE d.title = '北京->上海 数码配件' AND o.status = 'DELIVERED'
  AND NOT EXISTS (SELECT 1 FROM warehouse_records w WHERE w.order_id = o.id AND w.site_id = s.id);

-- 10) 公告（管理员端可见）
INSERT INTO notices (title, body, status, pinned, created_at)
SELECT '干线时效公告', '京沪干线本周时效提升，平均在途时间缩短 12%。', 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM notices WHERE title = '干线时效公告');

INSERT INTO notices (title, body, status, pinned, created_at)
SELECT '上海中转站扩容完成', 'HUB_SH 处理能力提升至 800 单/日。', 1, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM notices WHERE title = '上海中转站扩容完成');

-- 11) 用户反馈（管理员端审核入口有数据）
INSERT INTO feedbacks (user_id, type, content, status, reply, created_at)
SELECT @user1, 'CONSULT', '在途订单 2 小时未更新位置，麻烦核实。', 'OPEN', NULL, NOW()
WHERE NOT EXISTS (SELECT 1 FROM feedbacks f WHERE f.user_id = @user1 AND f.type = 'CONSULT');
