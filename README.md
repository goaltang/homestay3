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
├── homestay-admin/        # 房东与管理员前端（Vue 3）
├── homestay-backend/      # 后端服务 API（Spring Boot）
└── docs/                  # 项目文档
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

### 用户端功能 (homestay-front)

- **用户认证与权限管理**
  - 基于 JWT 的无状态身份验证与授权
  - 用户注册与邮箱验证
  - 密码重置与修改

- **房源浏览与搜索**
  - 多维度房源搜索筛选（地点、价格、房型、设施、评分）
  - 搜索参数 URL 持久化（支持分享链接、页面刷新恢复）
  - 搜索防抖（500ms）+ 请求取消机制（AbortController）
  - 分页组件（支持每页 12/24/36/48 条切换）
  - 排序功能（最新发布、价格低→高、价格高→低、评分高→低）
  - 热门目的地推荐 + 最近搜索历史（localStorage 持久化）
  - 智能错误处理（区分网络错误/超时/服务器错误，提供重试按钮）
  - 房源详情页与图片展示
  - 收藏与取消收藏

- **订单管理**
  - 民宿预订与支付
  - 订单状态跟踪（待付款、已确认、已入住、已完成、已取消）
  - 退款申请与取消

- **评价系统**
  - 订单完成后评价
  - 查看房源评价与评分

- **个人中心**
  - 个人信息管理
  - 订单历史
  - 收藏列表

### 房东端功能 (homestay-front)

- **房东入驻与引导**
  - 新房东入驻引导流程（个人介绍 → 身份验证）
  - 身份认证（身份证上传与审核状态管理）

- **控制台 Dashboard**
  - 房源数量、待处理订单、本月收入、新评价数统计卡片
  - 最近订单列表快速预览
  - 一键跳转至各管理模块

- **房源管理**
  - 房源创建与编辑（5步表单：基本信息 → 位置信息 → 设施与服务 → 描述信息 → 房源图片）
  - 房源状态管理（草稿 → 待审核 → 已上线 / 已拒绝 → 已下架 / 已暂停）
  - 批量操作（批量上线、下架、删除）
  - 草稿自动保存（每3分钟）与 localStorage 恢复
  - 审核记录查看与撤回审核申请

- **订单管理**
  - 全类型订单状态流转管理（待确认/已确认/已支付/待入住/已入住/已完成/已取消/已拒绝）
  - 自动状态管理（自动入住：当日22:00后自动设已入住；自动完成：次日06:00后自动设已完成）
  - 退款审核与争议处理
  - 订单详情查看（含旅客信息、价格明细、状态时间线）

- **收益管理**
  - 收益概览（总收益、本月收益、上月收益、未结算金额）
  - 收益趋势分析（ECharts图表，支持日/月/年视图）
  - 收益明细表格与数据导出（Excel/CSV/PDF）
  - 结算功能（手动触发收益结算）

- **提现管理**
  - 多账户管理（银行卡/支付宝/微信）
  - 提现申请（最低100元起提）
  - 提现记录与状态跟踪

- **评价管理**
  - 评价统计（好评/中评/差评分布）
  - 多维度筛选（按房源、评分、回复状态）
  - 房东回复评价与修改删除回复
  - 未回复评价高亮提醒

- **消息中心**
  - 房东-房客实时聊天功能
  - 未读消息徽章提示
  - 对话列表与消息状态管理

- **通知管理**
  - 多类型通知分类（预订请求/评价/房源审核结果/系统公告等）
  - 已读/未读状态管理
  - 通知快速操作与跳转

- **个人资料管理**
  - 基本信息编辑（用户名、昵称、邮箱、手机号）
  - 房东专属信息（个人介绍、职业、语言能力、接待伙伴）
  - 头像上传
  - 密码修改

### 管理员端功能 (homestay-admin)

- **审核工作台**
  - 待审核房源卡片展示
  - 批量审核与自动刷新
  - 审核历史记录
  - 审核数据统计分析

- **房源管理**
  - 房源列表与状态管理
  - 强制下架（带违规记录）
  - 房源类型与设施管理

- **用户管理**
  - 用户列表 CRUD、状态启用/禁用
  - 身份认证审核（实名认证资料）

- **订单管理**
  - 订单多条件筛选
  - 退款审核与争议解决

- **数据统计**
  - 概览统计（订单/收入/用户/房源）
  - 趋势图分析
  - 民宿分布

- **系统管理**
  - 系统配置（平台配置、政策配置、费用配置等）
  - 操作日志（记录管理员操作行为）
  - 登录日志（记录管理员登录历史：IP、设备、时间）
  - 公告管理（系统公告发布与管理，支持系统通知/活动公告分类）

### 通用功能

- **通知系统**
  - 多类型通知（订单状态、评价回复、房源动态等）
  - 消息状态管理（已读/未读）

- **支付集成**
  - 支付宝支付处理与回调
  - 退款流程处理
  - 交易记录与财务管理

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
- **异步日志记录**：`@Async` 注解实现操作日志异步记录，不阻塞主线程

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
