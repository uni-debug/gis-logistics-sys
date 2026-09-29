# 01wuliupeisong 项目规则（基于 GIS 的电商跨区域物流配送系统）

## 1. 技术栈规范
- 后端：Java 21 + Spring Boot 3.3.x + Spring Security 6 + Spring Data JPA 3.2 + Lombok 1.18 + MapStruct 1.5
- 数据库：MySQL 8.x（Connector/J 8）；空间字段 GEOMETRY(SRID 4326) + SPATIAL INDEX
- 空间计算：JTS 1.19（面包含/缓冲/点匹配在 Java 层）；路线引擎 OSRM 2.x（独立进程）
- 迁移：Flyway（db/migration 下按 V1__xxx.sql 命名）
- 测试：JUnit 5 + MockMvc + AssertJ + Testcontainers(MySQL)
- 前端：Vue ^3.4 + Vite ^5 + TypeScript ^5；Pinia ^2；Element Plus ^2；axios ^1
- 地图：MapLibre GL JS v5（开源免 key，矢量/栅格瓦片）；路线/轨迹用 LineLayer + GeoJSON
- 底图：默认 OpenStreetMap 样式；商用底图 key 只走环境变量，禁止入库
- 坐标一律 WGS84 (EPSG:4326)，与 MySQL GEOMETRY(SRID 4326) 对齐
- 金额/重量用最小整数单位（分/克）
- 引入新依赖前必须报备 名称/版本/用途，审批后允许

## 2. 命名规范
- Java 类：PascalCase；方法/变量：camelCase；常量：UPPER_SNAKE_CASE
- 包：com.xxx.logistics.domain.{module}
- 数据库 表名/字段：snake_case；空间字段 *_point / *_geometry
- REST 路径：/api/v1 + 名词复数；状态迁移用 PATCH；禁止动词嵌路径
- 前端 文件/组件：kebab-case（order-list.vue）；类型/接口：PascalCase
- 错误码：5 位，前 3 位模块码（1xx 通用 / 2xx 订单 / 3xx 支付 / 4xx GIS / 5xx 权限）

## 3. 错误处理规范
- 业务异常抛 BizException(code, message, data?)；全局 @RestControllerAdvice 统一捕获
- 响应统一包装 { code, data, message }；code=0 成功，非 0 为错误码
- 日志用 MDC 注入 traceId；级别 trace/debug/info/warn/error
- 关键路径（支付/权限/脱敏/GIS）异常必须 error 级 + traceId + 业务单号

## 4. 代码风格规范
- 后端 4 空格缩进；每行 <= 120 列；import 不用通配符
- 前端 2 空格缩进；单引号；语句分号；行宽 100
- 注释只写"为什么"，不写"做什么"
- 后端分包：common(基础)/gis(空间)/domain(业务)/infra(配置与外部 client)
- 单模块内 controller/service/repository/entity/dto 同包
- 单测覆盖率 >= 80%，关键路径 100%；提交前 checkstyle + 编译 + 测试全绿

## 5. 关键路径红线（只出方案，代码需人工复核）
- 支付：initiate（订单归属/状态/金额一致性）、回调验签+幂等、订单状态推进、可插拔 PaymentGateway（沙箱/真实渠道）
- 实时轨迹：TrackStreamService 按订单 SSE 广播（进程内局限、连接配额、EventSource 无 Auth 头）
- 权限：三端 JWT 身份校验、RoleGuard 拦截、admin perm_mask 分配、订单归属校验（OrderService.assertOwnership）
- 数据安全：手机号加密/脱敏、物流状态一致性(乐观锁)、迁移与种子脚本（V5 坐标列、R__seed 仅演示用）
- GIS：路线策略权重（成本已参数化）、轨迹 match 吸附、点位 KNN 匹配

## 6. 依赖管控
- 新增第三方依赖：先列 名称/版本/用途 → 审批 → 再引入
- 禁止引入未验证/版本不明确的包；pom/package.json 锁定版本号

## 7. 提交与迭代
- 小粒度提交：一个功能点/一个修复一个 commit，可独立回滚
- 出问题优先回滚提交，不在线上修补
- 每完成一个大模块做一次重构清理，清技术债


## 8. 前端规范（Vue 3 + Vite + MapLibre GL JS）
- 框架 Vue ^3.4 + Vite ^5 + TypeScript ^5；状态 Pinia ^2；UI Element Plus ^2；HTTP axios ^1
- 地图 MapLibre GL JS v5（开源免 key）；路线/轨迹用 LineString + GeoJSON 源
- 底图源默认 MapLibre 公共样式，生产 key 只走环境变量（VITE_MAP_STYLE），禁止入库
- 文件：kebab-case 组件（gis-map.vue）；类型/接口 PascalCase；store camelCase
- 请求统一走 src/api/http.ts（axios 实例 + JWT 拦截器 + ApiResponse 解包）
- 坐标 WGS84 (EPSG:4326)；金额/重量最小整数单位

## 9. 关键路径人工复核清单
以下模块已实现并配单测，按红线需人工复核后方可上线：
- [ ] 权限：JwtService（密钥归一化/issuer 强校验）、JwtAuthFilter（无效 token 不拦截逻辑）、SecurityConfig（路径-角色映射/无状态会话）
- [ ] 数据安全：PhoneCipher（AES-GCM 参数）、CryptoConfig（密钥 fail-fast，杜绝随机兜底导致数据不可逆）
- [ ] 物流一致性：OrderService.transition（状态机合法性 + @Version 乐观锁 + 时间戳落位 + 归属校验 ORDER_NOT_OWNED）、LogisticsEventService（事件→状态映射）、DemandService（报价金额>0、confirm 联动建单）
- [ ] 支付：PaymentService（金额一致性/幂等/坏签名拒绝/非 PENDING 拒绝/归属）、PaymentGateway（沙箱→真实渠道，回调前必须验签）
- [ ] 实时轨迹：TrackStreamService（快照+增量广播、进程内多实例局限、超时清理）、前端 useTrackStream（指数退避重连、断网快照兜底）
- [ ] GIS 核心：OsrmClient（car profile 三策略映射、成本权重已参数化到 app.osrm.* 需按真实费率标定）、RoutePlanService（GeoJSON→WKT→JTS）、TrackMatchService（吸附+逐点事件）、PointMatchService（STRtree KNN 半径阈值/Haversine）
- [ ] 依赖：mockwebserver:4.12.0（test，与 okhttp 同版本）已报备
- [ ] 迁移/种子：V5__orders_endpoint_coords（订单起终点坐标）、R__seed（幂等演示种子，phone_hash 占位仅演示，生产须走加密写入）

## 10. Dev 种子账号与 prod 密码守护
- 3 个演示账号由 V11__seed_business.sql 插入（无密码），bcrypt password_hash 由
  `DevSeedPasswordBootstrap`（infra/seed, Spring ApplicationRunner）在每次启动时幂等回填。
- 读 `spring.profiles.active`：含 `prod` 时直接跳过（日志打印 skip），prod 账号密码必须走注册/登录流程。
- 密码为预生成 bcrypt($2a$10$) 常量，与 dev 账号明文对应（dev-only，源码内可见，prod 永不写库）：
  - 13800000001 smoke123456 USER
  - 13800000002 staff123456 STAFF
  - 13800000003 admin123456 ADMIN
- R__seed.sql 已剥离 bcrypt 块，仅保留账号/站点兜底插入（无密码），安全网语义不变。
- 启动验证基线：dev/default profile 下 3 账号均可登录；`-Dspring.profiles.active=prod` 下不写入任何密码。

## 11. 路线引擎切换（折线 / OSRM）
- `app.osrm.engine` 配置：`line`（默认，折线引擎）/ `osrm`（真实 OSRM 服务）
- 折线引擎 `PolylineRouteEngine`：按起终点/途经点坐标生成 GeoJSON LineString + Haversine 距离 + 固定成本模型（`app.osrm.cost-fen-per-km` / `cost-fen-per-avoid-segment`）
- OSRM 部署后：`tools/osrm/` 下二进制已就位（v26.9.0），缺真实路网 PBF 数据；网络可达时下载 GeoFabrik 京沪 PBF 重建索引，`OSRM_ENGINE=osrm` 切回
- via_points 列 NOT NULL：无途经点时存最小点占位 `POINT(0 0)`（空集合会触发 geolatte BufferAccessException）

## 12. 既有阻塞项（非本轮引入，需人工决策）
- geolatte 递归 envelope 序列化 bug：`RouteTask`/`Site`/`LogisticsEvent` 等实体的 Geometry 字段经 Jackson 输出时产生 `envelope.envelope.envelope...` 嵌套，前端拿到的是畸形对象而非坐标。DB 层数据正确（`ST_AsText` 可正常读 LINESTRING），仅 JSON 序列化层受影响。修复需改 4 个实体的 Geometry 字段为 `String wkt` + 辅助方法，属大范围改动，留待单独处理。
- 真实支付渠道未接入（沙箱 PaymentGateway）。
