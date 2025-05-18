# 民宿预订平台 (Homestay Booking Platform)

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Version](https://img.shields.io/badge/version-1.0.0-green.svg)

## 项目简介

民宿预订平台是一个连接房东与旅客的综合性服务系统，致力于为旅客提供多样化的民宿选择，同时为房东提供高效的房源管理工具。系统包含三个主要部分：面向用户的前端界面、面向房东与管理员的后台管理系统，以及支撑整体业务的服务器端 API，构建了从房源发布、在线预订到服务评价的完整业务闭环。

## 技术栈

### 前端

- Vue 3
- Element Plus
- Pinia (状态管理)
- TypeScript
- Vite (构建工具)
- Axios (HTTP 请求)
- ECharts (数据可视化)

### 后端

- Spring Boot 2.7
- Java 17
- MySQL 8.0
- Spring Data JPA
- Spring Security + JWT
- Maven
- SendGrid (邮件服务)
- AWS S3 (文件存储)

## 项目结构

```
homestay3/
├── homestay-front/       # 用户前端
├── homestay-admin/       # 房东与管理员前端
└── homestay-backend/     # 后端服务API
```

## 核心功能模块

- **用户认证与权限管理**

  - 基于 JWT 的身份验证与授权
  - 用户注册与邮箱验证
  - 多角色权限控制 (普通用户、房东、管理员)
  - 密码重置与修改

- **房源管理模块**

  - 房源创建与编辑
  - 多维度房源信息维护 (基本信息、价格日历、设施配置、地理位置)
  - 房源状态管理 (上架、下架、审核中)
  - 丰富的查询筛选机制 (地点、价格、房型、设施、可用日期等)
  - 房源类型与设施分类管理

- **订单管理模块**

  - 完整的订单生命周期管理
  - 多状态订单处理 (待付款、已确认、已入住、已完成、已取消等)
  - 智能订单冲突检测
  - 退款与取消政策实现
  - 订单统计与导出

- **评价系统模块**

  - 多维度评分机制
  - 用户评价与房东回复
  - 评价管理与筛选
  - 评价分析与展示

- **通知系统**

  - 系统通知与用户消息
  - 实时通知提醒
  - 消息状态管理 (已读/未读)
  - 通知类型分类与筛选

- **支付集成**
  - 支付处理与订单确认
  - 退款流程处理
  - 交易记录与财务管理

## 技术特点

- RESTful API 设计
- 基于 Spring Security 的安全认证
- 数据库事务一致性保障
- 多条件动态查询构建
- 日志记录与异常处理
- 数据分页与高效检索

## 安装与使用

### 前端环境要求

- Node.js 14.18+
- npm 或 yarn

### 后端环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+

### 设置与运行

**1. 克隆仓库**

```bash
git clone https://github.com/yourusername/homestay3.git
cd homestay3
```

**2. 前端设置**

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

**3. 后端设置**

```bash
cd ../homestay-backend
mvn clean install
mvn spring-boot:run
```

**4. 数据库设置**

- 创建名为`homestay_db`的 MySQL 数据库
- 配置`application.properties`中的数据库连接参数

## 开发指南

### 代码规范

- 前端遵循 Vue 3 官方风格指南
- 后端遵循 Java 代码规范和 RESTful API 设计原则

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

本项目采用 MIT 许可证 - 详情请参阅[LICENSE](LICENSE)文件
