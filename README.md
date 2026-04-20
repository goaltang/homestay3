# 民宿预订平台 (Homestay Booking Platform)

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0.2-brightgreen.svg)
![Java](https://img.shields.io/badge/Java-17-orange.svg)
![Vue](https://img.shields.io/badge/Vue-3-42b883.svg)
![Vite](https://img.shields.io/badge/Vite-5%2F6-646CFF.svg)

民宿预订平台是一个连接房客、房东和平台管理员的综合业务系统，覆盖房源发布、平台审核、在线搜索、下单支付、入住退房、评价反馈、收益提现和后台监管等完整流程。

## 目录

- [项目亮点](#项目亮点)
- [技术栈](#技术栈)
- [系统角色](#系统角色)
- [功能概览](#功能概览)
- [核心业务流程](#核心业务流程)
- [项目结构](#项目结构)
- [本地运行](#本地运行)
- [配置说明](#配置说明)
- [测试与构建](#测试与构建)
- [文档索引](#文档索引)
- [Git 提交建议](#git-提交建议)
- [许可证](#许可证)

## 项目亮点

- **三端一体**：用户端、房东端、管理员端共用后端能力，分别覆盖消费、经营和平台治理场景。
- **完整订单链路**：支持待支付、已支付、已确认、入住、退房、完成、取消、退款和争议处理等状态。
- **房源审核闭环**：房东发布房源后进入审核流程，管理员可批量审核、强制下架和记录违规。
- **实时消息能力**：基于 WebSocket 支持房客与房东实时聊天和未读消息提醒。
- **地图找房体验**：支持位置搜索、周边检索、距离计算和地图展示。
- **支付与收益管理**：集成支付宝沙箱支付，支持房东收益统计、趋势图和提现管理。
- **工程化后端**：使用统一响应、全局异常、权限注解、DTO 映射、缓存和数据库迁移提升维护性。

## 技术栈

| 模块 | 技术 |
|---|---|
| 用户端 | Vue 3、TypeScript、Vite、Vue Router、Pinia、Element Plus、Axios、ECharts、高德地图 |
| 管理端 | Vue 3、TypeScript、Vite、Vue Router、Pinia、Element Plus、Axios、ECharts |
| 后端 | Java 17、Spring Boot 3.0.2、Spring Web、Spring Security、Spring Data JPA、Spring Validation |
| 数据与缓存 | MySQL 8.0、Redis、Redisson、Flyway |
| 通信与集成 | JWT、WebSocket、支付宝 SDK、SMTP 邮件 |
| 工程工具 | Maven、npm、MapStruct、Lombok |

## 系统角色

| 角色 | 说明 |
|---|---|
| 游客 | 可浏览首页、搜索房源、查看公开房源详情 |
| 房客 | 可收藏房源、提交订单、支付、退款、评价和发送消息 |
| 房东 | 可入驻平台、发布房源、管理订单、处理入住退房、查看收益和提现 |
| 管理员 | 可审核房源、管理用户和订单、处理举报争议、查看统计数据和配置平台规则 |

## 功能概览

### 用户端

| 模块 | 主要能力 |
|---|---|
| 账号认证 | 注册、登录、JWT 鉴权、密码重置、个人资料维护 |
| 首页推荐 | 热门民宿、个性化推荐、基于位置的推荐 |
| 房源搜索 | 关键词搜索、条件筛选、排序分页、URL 参数持久化、相似房源推荐 |
| 地图找房 | 高德地图展示、周边搜索、坐标定位、距离计算 |
| 房源详情 | 图片、设施、价格、位置、房东信息、评价列表 |
| 在线预订 | 日期选择、实时计价、订单预览、库存校验 |
| 在线支付 | 支付宝二维码或跳转支付、支付状态查询、支付成功回跳 |
| 订单管理 | 订单列表、订单详情、取消订单、退款申请、状态跟踪 |
| 收藏评价 | 收藏/取消收藏、完成订单后评价、查看我的评价 |
| 消息通知 | 房客与房东实时聊天、未读提醒、系统通知 |

### 房东端

| 模块 | 主要能力 |
|---|---|
| 房东入驻 | 入驻资料填写、身份认证、房东资料维护 |
| 房东控制台 | 房源、订单、收入、评价和最近订单概览 |
| 房源管理 | 创建、编辑、草稿保存、提交审核、上下架、删除、分组管理 |
| 房源发布 | 基本信息、位置、设施、描述、图片等分步录入 |
| 订单处理 | 查看订单、确认订单、拒绝订单、退款审核、争议处理 |
| 入住退房 | 生成入住凭证、自助入住码、办理入住、退房结算、押金和额外费用 |
| 收益管理 | 总收益、本月收益、未结算收益、日/月/年趋势统计、数据导出 |
| 提现管理 | 提现账户维护、提现申请、提现记录 |
| 评价管理 | 评分分布、评价列表、房东回复、未回复提醒 |
| 消息通知 | 会话列表、聊天记录、订单和审核通知 |

### 管理员端

| 模块 | 主要能力 |
|---|---|
| 审核工作台 | 待审核房源、批量审核、审核历史、审核统计 |
| 房源治理 | 房源列表、强制下架、违规记录、房源类型和设施管理 |
| 用户管理 | 用户列表、启用/禁用、身份认证审核 |
| 订单管理 | 多条件筛选、退款审核、争议解决 |
| 违规管理 | 举报列表、处理/忽略举报、违规扫描、重复举报统计 |
| 数据统计 | 订单、收入、用户、房源概览和趋势分析 |
| 系统配置 | 平台配置、政策配置、费用配置 |
| 公告管理 | 发布系统通知和活动公告 |
| 日志审计 | 管理员操作日志、登录日志 |

### 后端能力

| 能力 | 说明 |
|---|---|
| 统一响应 | 使用 `ApiResponse<T>` 统一返回 `success`、`code`、`message`、`data` 和 `timestamp` |
| 认证授权 | 使用 Spring Security + JWT 实现无状态认证，并按角色控制接口访问 |
| 数据访问 | 使用 Spring Data JPA、Repository 和 Specification 支持复杂查询 |
| 数据迁移 | 使用 Flyway 管理数据库结构演进 |
| 缓存加速 | 使用 Redis 缓存热点数据和推荐数据 |
| 实时通信 | 使用 WebSocket 支持聊天和消息推送 |
| 支付集成 | 接入支付宝沙箱支付，支持订单支付和异步通知 |
| 异常处理 | 使用 `@RestControllerAdvice` 统一处理业务异常和系统异常 |
| 对象映射 | 使用 MapStruct 完成 Entity、DTO、Request、Response 转换 |
| 审计记录 | 异步记录管理员操作、登录行为和关键业务状态变化 |

## 核心业务流程

### 房源发布与审核

```text
房东创建房源草稿
  -> 补充位置、设施、描述和图片
  -> 提交审核
  -> 管理员审核
  -> 审核通过后上线 / 审核拒绝后退回修改
```

### 订单生命周期

```text
房客提交订单
  -> 待支付
  -> 已支付
  -> 房东确认
  -> 准备入住
  -> 已入住
  -> 已退房
  -> 已完成
  -> 房客评价
```

### 退款与争议

```text
房客申请退款
  -> 房东审核
  -> 同意退款 -> 退款中 -> 退款完成
  -> 拒绝退款 -> 用户发起争议 -> 管理员介入处理
```

### 收益提现

```text
订单完成
  -> 生成房东收益
  -> 进入待结算金额
  -> 房东提交提现
  -> 平台处理提现
  -> 提现记录归档
```

## 项目结构

```text
homestay3/
├── homestay-front/          # 用户端 + 房东端，Vue 3 + Vite
├── homestay-admin/          # 管理员端，Vue 3 + Vite
├── homestay-backend/        # 后端 API，Spring Boot
├── docs/                    # 项目说明文档
├── tools/                   # 本地工具脚本或辅助工具
├── README.md                # 项目总览
└── .gitignore               # Git 忽略规则
```

后端主要分层：

```text
com.homestay3.homestaybackend
├── config/                  # 安全、缓存、跨域、WebSocket、支付等配置
├── controller/              # REST API 控制器
├── service/                 # 核心业务逻辑
├── repository/              # JPA 数据访问层
├── entity/                  # 数据库实体
├── dto/                     # 数据传输对象
├── mapper/                  # MapStruct 映射
├── model/                   # 枚举和常量
├── exception/               # 全局异常处理
├── security/                # JWT 与认证授权
└── util/                    # 通用工具类
```

## 本地运行

### 环境要求

| 环境 | 推荐版本 |
|---|---|
| JDK | 17+ |
| Maven | 3.6+ |
| MySQL | 8.0+ |
| Redis | 6.0+ |
| Node.js | 18+ |
| npm | 9+ |

### 1. 创建数据库

```bash
mysql -u root -p -e "CREATE DATABASE homestay_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

### 2. 启动后端

```bash
cd homestay-backend
mvn clean compile
mvn spring-boot:run
```

默认后端地址：

```text
http://localhost:8080
```

### 3. 启动用户端和房东端

```bash
cd homestay-front
npm install
npm run dev
```

默认访问地址：

```text
http://localhost:5173
```

### 4. 启动管理员端

```bash
cd homestay-admin
npm install
npm run dev
```

默认访问地址通常为：

```text
http://localhost:5174
```

如果端口被占用，Vite 会自动分配新的端口，请以终端输出为准。

## 配置说明

后端配置文件位于：

```text
homestay-backend/src/main/resources/application.properties
```

本地运行需要关注这些配置：

| 配置 | 说明 |
|---|---|
| `spring.datasource.*` | MySQL 连接地址、用户名和密码 |
| `spring.data.redis.*` | Redis 地址、端口、密码和数据库编号 |
| `jwt.secret` | JWT 签名密钥 |
| `spring.mail.*` | 邮件服务配置 |
| `payment.alipay.*` | 支付宝沙箱应用、公钥、私钥、网关和回调地址 |
| `file.upload-dir` | 上传文件保存目录 |

> 注意：不要把真实数据库密码、JWT 密钥、邮箱密码、支付宝私钥或生产环境回调地址提交到公开仓库。建议使用本地配置、环境变量或 `application-local.properties` 管理敏感信息，并只提交示例配置。

## 测试与构建

### 后端

```bash
cd homestay-backend
mvn test
mvn clean package
```

### 用户端

```bash
cd homestay-front
npm run build
```

### 管理员端

```bash
cd homestay-admin
npm run build
```

## 文档索引

| 文档 | 说明 |
|---|---|
| [项目结构总览](docs/项目结构总览.md) | 项目目录和模块职责 |
| [项目技术栈说明](docs/项目技术栈说明.md) | 技术选型和依赖说明 |
| [开发环境配置指南](docs/开发环境配置指南.md) | 本地开发环境准备 |
| [homestay-admin 详细结构](docs/homestay-admin%20详细结构.md) | 管理端目录结构说明 |
| [用户端说明](homestay-front/README.md) | 用户端和房东端前端说明 |
| [管理端说明](homestay-admin/README.md) | 管理端前端说明 |

## Git 提交建议

建议提交：

- `homestay-front/`：前端源码、路由、组件、API 封装、`package.json` 和 `package-lock.json`
- `homestay-admin/`：管理端源码、路由、组件、API 封装、`package.json` 和 `package-lock.json`
- `homestay-backend/src/main/java/`：后端业务源码
- `homestay-backend/src/main/resources/db/migration/`：Flyway 数据库迁移脚本
- `homestay-backend/src/test/`：测试代码
- `docs/`：项目说明文档
- `README.md`、`.gitignore` 等项目级配置文件

不建议提交：

- `node_modules/`、`dist/`、`target/`、`build/` 等依赖和构建产物
- `.env`、`.env.*`、真实的 `application.properties`、密钥文件、证书和私钥
- `uploads/`、临时图片、用户上传文件和本地测试数据
- `.vscode/`、`.idea/`、`.cursor/`、`.claude/`、`.qwen/` 等个人编辑器或 AI 工具配置
- `graphify-out/`、`obsidian-vault/` 等本地分析输出或个人知识库内容
- `.graphifyignore`、`CLAUDE.md`、`QWEN.md` 等本地辅助工具配置
- `*.log`、`test_output*.txt`、`compile_output*.txt`、`build_output*.txt` 等运行日志

如果必须保留配置格式，建议提交 `application.example.properties` 或 `.env.example`，并在其中使用占位符：

```properties
spring.datasource.password=CHANGE_ME
jwt.secret=CHANGE_ME
payment.alipay.private-key=CHANGE_ME
```

## 许可证

MIT License
