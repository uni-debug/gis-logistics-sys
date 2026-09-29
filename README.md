# 基于 GIS 的电商跨区域物流配送路线系统

前后端分离：Spring Boot 3.3 后端 + Vue 3 / MapLibre GL JS v5 前端，核心能力是 GIS 路线规划与物流轨迹追踪。

## 目录

```
.
├── CLAUDE.md                              # 项目规则（技术栈/命名/错误处理/代码风格）
├── start.ps1                               # 一键启动（MySQL -> OSRM -> 后端 -> 前端）
├── REVIEW_CHECKLIST.md                      # 关键路径人工复核清单
├── tools/                                   # 辅助脚本：smoke.ps1(端到端冒烟)、run-backend.ps1、run-tests.ps1
├── server/                                 # 后端 (Spring Boot + MySQL 8)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/gis/logistics/
│       │   ├── LogisticsApplication.java
│       │   ├── common/                     # 错误码/异常/响应/MDC/traceId
│       │   ├── gis/                        # GIS 核心 (OsrmClient/RoutePlan/TrackMatch/PointMatch)
│       │   ├── domain/                     # 业务模块
│       │   │   ├── auth/ user/ staff/ admin/ demand/ order/
│       │   │   ├── payment/                # 支付 (PaymentService + 可插拔 PaymentGateway)
│       │   │   └── logistics/              # 轨迹事件 + TrackStreamService (SSE 实时推送)
│       │   └── infra/                      # 配置 (Crypto/PasswordEncoder/Http)
│       └── main/resources/
│           ├── application.yml
│           └── db/migration/               # Flyway V1..V5 + R__seed
└── web/                                    # 前端 (Vue 3 + Vite + MapLibre GL JS)
    ├── package.json
    └── src/
        ├── main.ts / App.vue
        ├── api/ router/ stores/ types/ composables/  (useTrackStream SSE 封装)
        ├── components/gis/GisMap.vue       # MapLibre 地图/路线/轨迹彩色点+当前点脉冲+图例
        └── pages/{user,staff,admin}/
```

## 前置依赖

- Java 21、Maven 3.9+
- MySQL 8.x（含空间扩展，`GEOMETRY` 字段 + `SPATIAL INDEX`）
- OSRM 2.x（路线规划引擎，独立进程，默认 `http://127.0.0.1:5000`）
- Node 18+（前端构建）

## 环境变量（敏感项，禁止入库）

| 变量 | 用途 | 示例 |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | MySQL 连接 | `127.0.0.1` / `3306` / `logistics_db` / `root` / `<REDACTED>` |
| `JWT_SECRET` | JWT 签名密钥（>=32 字节） | 任意 32+ 字符 |
| `PHONE_ENCRYPTION_KEY` | 手机号 AES-256 密钥（缺失则启动失败） | 任意字符串（内部归一化为 32 字节） |
| `OSRM_BASE_URL` | OSRM 引擎地址 | `http://127.0.0.1:5000` |

> 上述变量在 `application.yml` 中均以 `${VAR:default}` 占位，真实值只走环境变量，不写进代码或日志。

## 启动顺序

1. **MySQL**：创建库 `logistics_db`，确保启用空间类型。
   ```sql
   CREATE DATABASE logistics_db CHARACTER SET utf8mb4;
   ```
2. **OSRM**：起 `osrm-router`（默认 5000 端口）。开发可用官方 Docker 镜像。
3. **后端**：
   ```bash
   cd server
   export DB_NAME=logistics_db DB_USER=root DB_PASSWORD=<REDACTED>
   export JWT_SECRET=<REDACTED> PHONE_ENCRYPTION_KEY=<REDACTED>
   mvn spring-boot:run
   # Flyway 自动执行 V1..V5 建表 + R__seed 种子数据
   ```
4. **前端**：
   ```bash
   cd web
   npm install
   npm run dev          # http://127.0.0.1:5173，/api 代理到 8080
   ```

## 一键启动

```powershell
.\start.ps1              # 全栈：MySQL(docker) -> OSRM -> 后端 -> 前端
.\start.ps1 -SkipOsmr    # 跳过 OSRM
.\start.ps1 -DbPassword <pass>   # 覆盖数据库口令
```

启动后：后端 `http://127.0.0.1:8080`，前端 `http://127.0.0.1:5173`。
`R__seed.sql` 会幂等地种入演示用户/员工/管理员与中转站点。

## 三端功能清单

| 端 | 核心功能 |
|---|---|
| 用户端 | 注册登录 / 个人中心 / 公告查看 / 发布需求 / 支付 / 查看订单 / 物流轨迹(SSE 实时) / 订单评论 / 留言反馈 / 在线客服 |
| 员工端 | 登录 / 个人中心 / 需求查看 / 接取报价 / 路线查看 / 路线搜索 / 订单管理 / 评论管理 / 物流信息发布 |
| 管理端 | 登录 / 个人中心 / 公告管理 / 用户管理 / 员工管理 / 需求审核 / 物流监控 / 订单管理 / 路线管理(GIS) / 订单统计(ECharts) / 评论管理 / 仓储管理 / 留言反馈 / 在线客服 |

## 演示账号（种子数据）

| 角色 | 手机号 | 密码 |
|---|---|---|
| USER | `13800000001` | `smoke123456` |
| STAFF | `13800000002` | `staff123456` |
| ADMIN | `13800000003` | `admin123456` |

登录页 `http://localhost:5173/user/login`，切角色 Tab 后填手机号+密码，前端传 `expectedRole`，后端按数据库角色校验（不匹配返回 `5004 role mismatch`）。

## 支付


- 默认使用 `SandboxPaymentGateway`（无外部依赖，本地联调/测试用）。
- 接入真实渠道（支付宝/微信/银联）：实现 `PaymentGateway` 接口并替换 Bean；回调前必须在网关内完成签名校验，再调 `PaymentService.handleCallback`。
- 用户端 `POST /user/orders/{id}/pay` 发起、`/pay/callback` 模拟回调（沙箱）。

## 测试

```bash
cd server
mvn test               # 136 个单测（离线：mvn -o test；含 RouteStatsServiceTest 路线聚合 6 用例）
```

## 端到端冒烟（smoke）

对已运行的全栈跑主链路：register -> 发需求 -> 员工报价 -> 确认下单 -> 沙箱支付 -> 回调 -> 配路线 -> 发在途事件 -> 查轨迹 -> SSE。

```powershell
# 用户侧链路（无需 staff token）
pwsh -File tools/smoke.ps1

# 完整 E2E：先取一个 staff JWT，再传 -StaffToken
$ST = (Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:8080/auth/login' -Body '{"phone":"13900000002","password":"smoke123456"}' -ContentType 'application/json').data.token
pwsh -File tools/smoke.ps1 -StaffToken $ST
```

前置条件：MySQL + 后端(:8080) 已启动；OSRM(:5000) 可选（缺则路线步骤输出 WARN）。

> SSE 在本脚本中为**尽力而为**探测；真正的 SSE 投递逻辑由后端 `TrackStreamServiceTest` 覆盖（`EventSource` 无法带 `Authorization` 头，鉴权走同源/cookie）。

## 关键路径（需人工复核）

以下模块按项目红线仅给出实现与方案，最终代码需人工复核：

- **权限**：`common/security/JwtService`、`JwtAuthFilter`、`SecurityConfig`
- **数据安全**：`common/crypto/PhoneCipher`、`infra/config/CryptoConfig`（fail-fast）
- **物流一致性**：`domain/order/OrderService`（乐观锁状态机）、`domain/logistics/LogisticsEventService`、`domain/demand/DemandService`（报价金额）
- **支付**：`domain/payment/PaymentService`（金额一致性/幂等/状态推进）+ `PaymentGateway` 可插拔渠道
- **实时轨迹**：`domain/logistics/TrackStreamService`（按订单 SSE 广播，进程内；多实例需换消息中间件）
- **GIS 核心**：`gis/osrm/OsrmClient`、`gis/route/RoutePlanService`、`gis/track/TrackMatchService`、`gis/point/PointMatchService`、`domain/admin/RouteStatsService`（路线聚合统计）

详见 `CLAUDE.md` 第 5 节与 `REVIEW_CHECKLIST.md`。