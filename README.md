# 民宿预订平台 (Homestay Booking Platform)

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0.2-brightgreen.svg)
![Java](https://img.shields.io/badge/Java-17-orange.svg)
![Vue](https://img.shields.io/badge/Vue-3-42b883.svg)

连接房东与旅客的综合性民宿服务系统，提供从房源发布、在线预订到服务评价的完整业务闭环。

## 技术栈

| 层 | 技术 |
|---|---|
| **后端** | Java 17 · Spring Boot 3.0.2 · Spring Data JPA · MySQL 8.0 · Redis |
| **前端** | Vue 3 + TypeScript · Element Plus · Pinia · Vite · Axios · ECharts |
| **安全** | Spring Security + JWT（无状态认证） |
| **其他** | Flyway（数据库迁移）· MapStruct（DTO映射）· WebSocket（实时聊天）· 支付宝SDK |

## 项目结构

```
homestay3/
├── homestay-front/    # 用户端 + 房东端（Vue 3）
├── homestay-admin/    # 管理员端（Vue 3）
├── homestay-backend/  # 后端 API（Spring Boot）
└── tools/             # 工具脚本
```

### 后端分层架构

```
com.homestay3.homestaybackend
├── config/        配置层 — Security、Redis、WebSocket、CORS、支付宝等
├── controller/    控制层 — RESTful API（40+ 控制器）
├── service/       服务层 — 核心业务逻辑
├── repository/    数据访问层 — JPA Repository
├── entity/        实体层 — 数据库表映射
├── dto/           数据传输对象
├── mapper/        MapStruct 自动化转换
├── model/         枚举与常量
├── exception/     异常处理
├── security/      JWT Token
└── util/          工具类
```

## 快速开始

### 环境要求

| 环境 | 版本 |
|---|---|
| JDK | 17+ |
| Maven | 3.6+ |
| MySQL | 8.0+ |
| Redis | 6.0+ |
| Node.js | 14.18+ |

### 启动服务

```bash
# 后端
cd homestay-backend
mvn clean compile
mvn spring-boot:run          # Flyway 自动执行数据库迁移

# 用户前端
cd homestay-front && npm install && npm run dev

# 管理前端
cd homestay-admin && npm install && npm run dev
```

### 数据库配置

```bash
mysql -u root -p -e "CREATE DATABASE homestay_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

配置文件：`homestay-backend/src/main/resources/application.properties`

## 核心功能

### 用户端（homestay-front）

| 模块 | 功能 |
|---|---|
| **认证** | JWT 登录/注册、邮箱验证、密码重置 |
| **首页** | 热门民宿推荐、个性化推荐、基于位置的推荐 |
| **房源搜索** | 多维度筛选（地点/价格/设施/评分）、搜索防抖 + AbortController、URL 参数持久化、分页排序、相似民宿推荐 |
| **房源详情** | 图片展示、设施列表、位置地图（高德地图）、房东信息、评价列表 |
| **预订流程** | 日期选择、价格实时计算、订单预览、支付宝支付（二维码/跳转） |
| **订单管理** | 全状态流转（待付款→已确认→已入住→已完成）、退款申请、取消订单 |
| **收藏** | 收藏/取消收藏、收藏列表 |
| **评价** | 订单完成后评价、我的评价列表 |
| **消息中心** | 房东-房客实时聊天（WebSocket）、未读消息提醒 |
| **个人中心** | 个人信息管理、订单历史 |

### 房东端（homestay-front）

| 模块 | 功能 |
|---|---|
| **入驻引导** | 房东入驻流程（个人介绍 → 身份认证） |
| **控制台** | 房源/订单/收入/评价统计、最近订单快速预览 |
| **房源管理** | 5步创建表单（基本信息→位置→设施→描述→图片）、草稿自动保存、批量上下架/删除、审核状态流转（草稿→待审核→已上线/已拒绝）、房源分组管理 |
| **入住管理** | 准备入住（生成入住凭证）、办理入住/退房、自助入住（入住码）、退房结算、押金管理、额外费用 |
| **订单管理** | 全状态流转、自动入住（22:00自动已入住）、自动完成（次日06:00）、退款审核、争议处理 |
| **收益管理** | 收益概览（总收益/本月/上月/未结算）、ECharts 日/月/年趋势图、数据导出（Excel/CSV） |
| **提现管理** | 多账户管理（银行卡/支付宝/微信）、提现申请（最低100元） |
| **评价管理** | 好评/中评/差评分布、房东回复、未回复高亮提醒 |
| **消息中心** | 聊天记录、会话列表、消息状态 |
| **通知管理** | 预订请求/评价/审核结果/系统公告分类 |
| **个人资料** | 基本信息、头像上传、密码修改 |

### 管理员端（homestay-admin）

| 模块 | 功能 |
|---|---|
| **审核工作台** | 待审核房源卡片、批量审核、自动刷新、审核历史、审核统计 |
| **房源管理** | 房源列表、强制下架（带违规记录）、房源类型/设施管理 |
| **分组管理** | 房源分组（分组名称、分组描述、分组排序） |
| **用户管理** | 用户列表 CRUD、启用/禁用、身份认证审核 |
| **订单管理** | 多条件筛选、退款审核、争议解决 |
| **违规管理** | 举报列表、待处理举报、处理/忽略操作、违规扫描、多次举报房源统计 |
| **数据统计** | 订单/收入/用户/房源概览、趋势图、民宿分布 |
| **系统配置** | 平台配置、政策配置、费用配置 |
| **操作日志** | 管理员操作行为记录 |
| **登录日志** | 登录历史（IP/设备/时间） |
| **公告管理** | 系统公告发布（分类：系统通知/活动公告） |

## 订单完整生命周期

```
用户提交订单 → 待付款 → 已支付 → 已确认 → 准备入住 → 已入住 → 已退房 → 已完成
                ↓                                              ↓
            已取消                                         有异议→争议→待处理
                ↓                                              ↓
            退款申请 → 房东审批同意 → 退款中 → 退款完成
                       ↓
                  拒绝退款 → 有异议 → 争议
```

## 技术特点

- **统一响应格式** — `ApiResponse<T>` 封装（success/code/message/data/timestamp）
- **全局异常处理** — `@RestControllerAdvice` 按类型返回 HTTP 状态码
- **MapStruct DTO映射** — 声明式 Entity ↔ DTO 转换
- **细粒度权限控制** — `@PreAuthorize` 方法级角色鉴权（ROLE_HOST/ROLE_ADMIN）
- **@Transactional** — 数据库事务一致性保障
- **JPA Specification** — 多条件动态查询
- **Redis 缓存** — 热点数据缓存（推荐数据等）
- **WebSocket** — 实时聊天消息推送
- **支付宝支付集成** — 当面付（生成支付表单/二维码）
- **环境感知日志** — dev 环境返回详细堆栈，生产环境隐藏敏感信息
- **@Async 异步日志** — 操作日志异步记录

## 许可证

MIT License
