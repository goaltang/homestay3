# 民宿预订平台 (Homestay Booking Platform)

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Version](https://img.shields.io/badge/version-1.0.0-green.svg)
![Java](https://img.shields.io/badge/Java-17-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0.2-brightgreen.svg)
![Vue](https://img.shields.io/badge/Vue-3-42b883.svg)

## 项目简介

民宿预订平台是一个连接房东与旅客的综合性服务系统，致力于为旅客提供多样化的民宿选择，同时为房东提供高效的房源管理工具。系统包含三个主要部分：面向用户的前端界面、面向房东与管理员的后台管理系统，以及支撑整体业务的服务器端 API，构建了从房源发布、在线预订到服务评价的完整业务闭环。

## 技术栈

### 前端

| 类别 | 技术 |
|---|---|
| 框架 | Vue 3 + TypeScript |
| UI 组件库 | Element Plus |
| 状态管理 | Pinia |
| 构建工具 | Vite |
| HTTP 请求 | Axios |
| 数据可视化 | ECharts |

### 后端

| 类别 | 技术 |
|---|---|
| 语言 / 框架 | Java 17 · Spring Boot 3.0.2 |
| 数据访问 | Spring Data JPA |
| 数据库 | MySQL 8.0 |
| 数据库迁移 | Flyway |
| 安全认证 | Spring Security + JWT（无状态会话） |
| 缓存 | Redis + Spring Boot Cache |
| DTO 映射 | MapStruct 1.5.5 |
| 支付集成 | 支付宝 SDK |
| 邮件服务 | Spring Boot Starter Mail |
| JSON 处理 | Fastjson2 |
| 构建工具 | Maven |

## 项目结构

```
homestay3/
├── homestay-front/       # 用户前端（Vue 3）
├── homestay-admin/       # 房东与管理员前端（Vue 3）
├── homestay-backend/     # 后端服务 API（Spring Boot）
└── docs/                 # 项目文档
```

### 后端分层架构

项目采用经典的 Spring Boot 三层架构（MVC 变种），结构清晰：

```
com.homestay3.homestaybackend
├── config/          配置层 — Security, Cache, Payment, Mail, CORS 等
├── controller/      控制层 — RESTful 接口，参数校验与路由
├── service/         服务层 — 核心业务逻辑（接口 + impl 实现）
├── repository/      数据访问层 — JPA Repository + 自定义 @Query
├── entity/          实体层 — 数据库表映射
├── dto/             数据传输对象 — ApiResponse<T>、PagedResponse<T> 等
├── mapper/          映射层 — MapStruct 自动化 Entity ↔ DTO 转换
├── model/           模型层 — 枚举与常量（如 HomestayStatus）
├── exception/       异常处理 — @RestControllerAdvice 全局统一异常响应
├── security/        安全层 — JWT Token 生成与校验
└── util/            工具类 — 数据库健康检查、图片URL处理、IP获取等
```

## 核心功能模块

- **用户认证与权限管理**
  - 基于 JWT 的无状态身份验证与授权
  - 用户注册与邮箱验证
  - 多角色权限控制（普通用户、房东、管理员）
  - 密码重置与修改

- **房源管理模块**
  - 房源创建与编辑
  - 多维度房源信息维护（基本信息、价格日历、设施配置、地理位置）
  - 房源状态全生命周期管理（草稿 → 待审核 → 已上架 / 已拒绝 → 已下架 / 已暂停）
  - 丰富的查询筛选机制（地点、价格、房型、设施、可用日期等）
  - 房源类型与设施分类管理

- **订单管理模块**
  - 完整的订单生命周期管理
  - 多状态订单处理（待付款、已确认、已入住、已完成、已取消等）
  - 智能订单冲突检测与日期不可用校验
  - 退款与取消政策实现
  - 订单统计与导出

- **评价系统模块**
  - 多维度评分机制
  - 用户评价与房东回复
  - 评价管理与筛选
  - 评价统计分析与展示

- **通知系统**
  - 多类型通知（订单状态、评价回复、房源动态等）
  - 消息状态管理（已读/未读）
  - 批量查询优化，避免 N+1 问题
  - 细粒度权限控制，用户只能操作自己的通知
  - 分页查询与多维度筛选支持

- **支付集成**
  - 支付宝支付处理与回调
  - 退款流程处理
  - 交易记录与财务管理

- **收藏功能**
  - 用户收藏与取消收藏
  - 收藏列表管理

## 技术特点

- **RESTful API 设计**：标准化接口规范
- **统一响应格式**：全局 `ApiResponse<T>` 封装（含 `success`、`code`、`message`、`data`、`timestamp`）
- **全局异常处理**：`@RestControllerAdvice` 统一捕获，按异常类型返回对应 HTTP 状态码
- **MapStruct DTO 映射**：自动化 Entity ↔ DTO 转换，减少手写样板代码
- **数据库版本控制**：Flyway 管理数据库迁移脚本
- **Redis 缓存**：热点数据缓存，提升查询性能
- **细粒度权限控制**：`@PreAuthorize` 注解实现方法级别的角色鉴权
- **数据库事务一致性保障**：`@Transactional` 管理关键业务操作
- **多条件动态查询**：JPA Specification + 自定义 @Query 灵活组合
- **环境感知异常信息**：dev 环境返回详细堆栈，生产环境隐藏敏感信息

## 安装与使用

### 环境要求

| 环境 | 版本要求 |
|---|---|
| JDK | 17+ |
| Maven | 3.6+ |
| MySQL | 8.0+ |
| Redis | 6.0+ |
| Node.js | 14.18+ |
| npm | 6.0+ |

### 设置与运行

**1. 克隆仓库**

```bash
git clone https://github.com/yourusername/homestay3.git
cd homestay3
```

**2. 数据库设置**

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE homestay_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

配置 `homestay-backend/src/main/resources/application.properties` 中的数据库连接参数。

**3. 后端启动**

```bash
cd homestay-backend
mvn clean compile
mvn spring-boot:run
```

> Flyway 会在启动时自动执行数据库迁移脚本。

**4. 前端启动**

```bash
# 用户前端
cd homestay-front
npm install
npm run dev

# 房东管理前端
cd ../homestay-admin
npm install
npm run dev
```

## 开发指南

### 代码规范

- 前端遵循 Vue 3 官方风格指南
- 后端遵循 Java 代码规范和 RESTful API 设计原则
- 工具类统一放置于 `util` 包
- DTO 映射使用 MapStruct 接口声明式定义

### 分支管理

- `main`: 稳定版本
- `develop`: 开发版本
- 功能分支: `feature/功能名称`
- 修复分支: `bugfix/问题描述`

## 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

## 许可证

本项目采用 MIT 许可证 - 详情请参阅 [LICENSE](LICENSE) 文件
