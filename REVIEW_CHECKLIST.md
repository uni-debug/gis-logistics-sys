# 关键路径人工复核清单（上线前必须过）

按项目红线，以下模块已实现并配单测，但属关键路径，最终代码需人工复核确认后方可上线。
每项给出：复核要点 / 主要风险 / 建议补充的测试用例。

---

## 1. 权限（三端身份校验 / 接口拦截 / 管理员权限）

### JwtService（common/security/JwtService.java，79 行）
- **要点**：HS256；密钥从 `app.jwt.secret` 或环境变量 `JWT_SECRET` 注入，缺失 fail-fast；短密钥补 0 到 32 字节；`issuer` 强校验；载荷 `sub=userId, role, iss, iat, exp`。
- **风险**：密钥归一化（补 0）若运维误填 <32 字节弱密钥，签名强度下降；`role` 写死在 token，角色变更后旧 token 仍有效直至过期。
- **建议用例**：短密钥归一化正确性；role 变更需重签（业务层校验 token 与 DB role 一致）。

### JwtAuthFilter（common/security/JwtAuthFilter.java，46 行）
- **要点**：`Bearer` 解析 → `parse` → 写 `SecurityContext`；无效/缺失 token 不在此拦截（交后续路径守卫 401/403）。
- **风险**：无效 token 静默放行可能让"带坏 token 的受保护接口"走到 401 而非提前拒绝——需确认 `SecurityConfig` 的 `anyRequest().authenticated()` 兜底。
- **建议用例**：带无效 token 访问 `/api/v1/user/**` → 401；带有效 token 但 role 不符 → 403。

### SecurityConfig（common/security/SecurityConfig.java，68 行）
- **要点**：无状态 + CSRF 关；`/auth/**`、`/error` 放行；`/api/v1/{user,staff,admin}` 按 `hasRole`；`/api/v1/gis/**` 需认证；401/403 返回 `ApiResponse`。
- **风险**：`hasRole` 前缀 `ROLE_` 与 `JwtAuthFilter` 写的 `ROLE_<role>` 大小写必须一致；`/gis/**` 仅 `authenticated()`，三端任意角色都能调 GIS，若 GIS 应限员工需收紧。
- **建议用例**：USER 调 `/api/v1/staff/**` → 403；STAFF 调 `/api/v1/admin/**` → 403。

---

## 2. 数据安全（脱敏 / 一致性 / 迁移）

### PhoneCipher（common/crypto/PhoneCipher.java，57 行）
- **要点**：AES-256-GCM（iv 12B + tag 128bit），输出 `base64(iv||ct+tag)`；密钥 32 字节强校验。
- **风险**：GCM 同密钥+同 IV 重放危险——每次随机 IV（已用 `SecureRandom`，需确认）；密钥一旦泄露需全量重加密。
- **建议用例**：同一明文两次加密结果不同（IV 随机）；篡改 ct 被拒（已有）；错密钥被拒（已有）。

### CryptoConfig（infra/config/CryptoConfig.java，27 行）
- **要点**：仅从 `PHONE_ENCRYPTION_KEY` 取密钥，缺失 `IllegalStateException` fail-fast；归一化 32 字节。
- **风险**：生产必须设该变量，否则启动失败（正确行为）；但**重启后若换密钥，历史密文无法解密**——密钥需 KMS/长期稳定。
- **建议用例**：缺密钥启动失败；密钥变更后旧数据迁移脚本。

### 迁移脚本（V1..V4）
- **要点**：V1 建 14 表 + 空间索引；V2 加 `users.password_hash`；V3 加 `demands.quoted_by/quoted_at`；V4 加 `reviews.deleted`。
- **风险**：MySQL `GEOMETRY` + `SPATIAL INDEX` 需确认实例已启用；`V1` 不可变（已走 V2/V3/V4 增量，合规）。
- **建议用例**：全新库 `flyway migrate` 成功；`V1` 已跑过的库 `V2..V4` 幂等增量。

---

## 3. 物流一致性

### OrderService（domain/order/OrderService.java，43 行）
- **要点**：`transition` 单一入口；校验 `canTransitionTo`；`PAID` 置 `paidAt`、`DELIVERED` 置 `deliveredAt`；`@Version` 乐观锁冲突映射 `ORDER_VERSION_CONFLICT`。
- **风险**：并发双迁移靠乐观锁兜底但需上层重试。
- **已补**：`transition` 已加 `assertOwnership`——`operatorId` 为订单责任员工本人或 `0`（管理哨兵），否则抛 `ORDER_NOT_OWNED`（2004）。管理端仍用 0 哨兵绕过，若需真实角色校验请改传 admin profile。
- **建议用例**：非法迁移拒绝（已有）；乐观锁冲突（已有）；**归属校验**（员工只能迁移自己订单）——当前缺，建议补。

### LogisticsEventService（domain/logistics/LogisticsEventService.java，52 行）
- **要点**：发布动态 → 按 `EventType` 映射订单状态并调 `transition`；`IN_TRANSIT` 逐点落 `LogisticsEvent`。
- **风险**：事件类型到状态的映射（`PICKED→PICKED` 等）若与订单实际状态不匹配会抛 `INVALID_TRANSITION`；GPS 点为 null 时不建 Point（已判空）。
- **建议用例**：订单 `PAID` 时发 `PICKED` 事件成功；订单 `DELIVERED` 后再发事件应拒绝。

### DemandService（domain/demand/DemandService.java，87 行）
- **要点**：发布（重量/体积/标题校验）；报价（状态 `PENDING` + 金额 >0）；确认（`QUOTED` 态）；`close`（下架）；`pageMine/pageByStatus`。
- **风险**：报价金额无上限校验（防恶意大额）。
- **已补**：`confirm` 已联动创建 `Order`（金额=`quoted_price`，状态 `PENDING`，回填 `staffId/demandId/userId`），返回 `ConfirmResult(demand, order)`。
- **建议用例**：报价金额上限；确认自动建订单并回填 `demand.quoted_price`。

---

## 4. GIS 核心

### OsrmClient（gis/osrm/OsrmClient.java，95 行）
- **要点**：`car` profile；`planRoute` 调 routing-v2，`matchTrack` 调 match-v5；失败映射 `ROUTE_OSRM_UNAVAILABLE`。
- **风险**：**三策略都用 `car` 默认 duration 权重**——`SHORTEST` 非真实距离最优；`CHEAPEST` 成本公式（距离×单价 + 规避段×罚分）是**启发式，需按真实燃油/过路费/城市费率标定**。
- **已补**：成本权重已参数化到 `application.yml`：`app.osrm.cost-fen-per-km`（默认 200）、`app.osrm.cost-fen-per-avoid-segment`（默认 50），可经 `OSRM_COST_FEN_PER_KM` 等环境变量覆盖。
- **建议用例**：三策略返回 geometry 合法性；成本权重参数化（建议抽到配置）。

### RoutePlanService（gis/route/RoutePlanService.java，53 行）
- **要点**：调 OSRM → GeoJSON LineString 经 `GeometryUtil`（WKT 中转）转 JTS → 落 `route_tasks`。
- **风险**：`GeometryUtil` 用 `WKTReader` 解析，坐标数极大时 WKT 字符串膨胀；`MultiLineString` 支持但部分下游可能只接 `LineString`。
- **建议用例**：超大路线（万级坐标）性能；`MultiLineString` 序列化回 GeoJSON 给前端。

### TrackMatchService（gis/track/TrackMatchService.java，55 行）
- **要点**：原始 GPS ≥2 点 → OSRM match 吸附 LineString + 逐点 `IN_TRANSIT` 事件。
- **风险**：逐点写事件在轨迹点很多时 N 次 DB 写，建议批量；match 无匹配时返回 `TRACK_MATCH_FAILED`。
- **建议用例**：100 点轨迹吸附正确性；**批量写入**优化（当前逐条）。

### PointMatchService（gis/point/PointMatchService.java，64 行）
- **要点**：`STRtree` 空间索引 + Haversine 最近邻 + 半径阈值（米→度近似 111320）。
- **风险**：`radiusM/111320` 的赤道近似在中高纬度偏大，建议按纬度修正；候选集大时 `STRtree.query` 返回多需二次筛（已做）。
- **建议用例**：纬度 60° 处半径精度；大候选集 KNN 正确性。

---

## 5. 支付（金额一致性 / 幂等 / 状态推进 / 渠道可插拔）

### PaymentService（domain/payment/PaymentService.java）
- **要点**：`initiate` 校验订单归属 + 仅 `PENDING` 可支付 + 金额=订单金额（分）；生成 `txnNo`（网关）。`handleCallback` 先验签（`signedOk`）、再金额一致、再幂等（已 `PAID` 直接返回）、推进订单 `PENDING->PAID` 并写 `payment_logs`。
- **风险**：沙箱回调直接传 `signedOk=true`，**生产必须真实验签**；`txnNo` 幂等依赖 `uk_pay_order_txn(order_id,txn_no)`，若同一订单多次 `initiate` 会新增行而非复用——需确认业务是否要"一单一支付单"。
- **建议用例**：金额不符拒绝（已有）；坏签名拒绝（已有）；重复回调幂等不重复推进（已有）；非本人订单拒绝（已有）；非 `PENDING` 订单拒绝（已有）。

### PaymentGateway / SandboxPaymentGateway（domain/payment/）
- **要点**：接口抽象渠道，`SandboxPaymentGateway` 无外部依赖用于联调/测试；真实渠道实现后替换 Bean。
- **风险**：真实渠道需管理密钥（走环境变量，禁止入库）；回调异步 + 重试语义需与渠道对齐；退款路径（`REFUNDING/REFUNDED`）当前未实现网关方法。
- **建议用例**：真实网关的 `newTxnNo` 唯一性；网关超时/失败的降级。

### 迁移与种子（db/migration/）
- **要点**：`V5__orders_endpoint_coords` 给 `orders` 加起终点坐标（WGS-84，`DECIMAL(9,6)`）；`R__seed.sql` 幂等种入站点/用户/员工/管理员。
- **风险**：`R__seed` 的 `phone_hash` 用 `SHA2(明文,256)` 占位、`phone_enc` 补零——**仅演示用**，真实用户必须走应用加密写入；`R__` 迁移可重复执行，但改种子逻辑需评估历史库影响。
- **建议用例**：全新库 `flyway migrate` 全量成功（V1..V5 + R__seed）；已跑库重执行 `R__seed` 无副作用。

---

## 6. 实时轨迹（SSE 按订单广播）

### TrackStreamService（domain/logistics/TrackStreamService.java）
- **要点**：按 `orderId` 维护 `SseEmitter` 订阅表；`subscribe` 先推 `SNAPSHOT`（全量 WKT 事件）再增量；`LogisticsEventService.publish` 成功后 `broadcast`；5min 超时 + complete/error 自动清理。推送用 `WktPayload(id,type,pointWkt,occurredAt)` 避免序列化 JTS `Geometry`。
- **风险**：**进程内广播**——多实例部署时订阅与发布不在同进程，需换 Redis pub/sub 或消息队列；连接数无上限，恶意长连接占满需加配额；`EventSource` 无法带 `Authorization` 头，若鉴权仅靠 Bearer 则 401（需改 cookie/token-query 方案）。
- **建议用例**：订阅后收到快照（已有）；无订阅者广播为空操作（已有）；超时清理（建议补）。

### 前端 useTrackStream（web/src/composables/useTrackStream.ts）
- **要点**：`EventSource` 封装，指数退避重连（1s→30s，最多 5 次），`SNAPSHOT` 全量 + 分类型增量；`orderId` 为响应式引用，切换订单自动重订。
- **风险**：断网期间增量事件丢失（靠重连后 SNAPSHOT 兜底，但中间窗口可能缺事件）；标签页休眠 `EventSource` 自动断开需前端兜底。
- **建议用例**：重连后快照一致性；多标签页同订单互不干扰。

---

## 7. 依赖报备（已审批）
- `com.squareup.okhttp3:okhttp:4.12.0`（OSRM REST 客户端）
- `com.squareup.okhttp3:mockwebserver:4.12.0`（test，与 okhttp 同版本）
- `org.locationtech.jts:jts-core:1.19.0`（空间计算；实际拉取 1.18.2 含 WKT/STRtree，无 GeoJSON Reader → 用 WKT 中转）
- `io.jsonwebtoken:jjwt:0.12.6`（JWT）

## 8. 前端（web/）
- `vue-tsc` 0 错误、`vite build` 通过；`GisMap.vue` 用 MapLibre 渲染路线/轨迹。
- **风险**：底图源用公共样式，生产换自有瓦片。
- **已补**：员工端路线规划不再硬编码——`orders` 表加 `from_lon/from_lat/to_lon/to_lat`（V5），`PATCH /staff/orders/{id}/endpoints` 由员工首录一次，`planRoute` 缺省回退订单坐标；GisMap 现渲染路线线 + 彩色轨迹点 + 当前点脉冲 + 图例；SSE `useTrackStream` 实时推送。