
# 民宿管理系统 (Homestay Management System)

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Version](https://img.shields.io/badge/version-1.0.0-green.svg)

## 项目简介

民宿管理系统是一个全面的解决方案，旨在帮助民宿经营者和管理员高效地管理房源、预订和客户。系统包含三个主要部分：前端用户界面、后端管理系统和服务器端 API，提供了从房源展示、在线预订到后台管理的完整功能链。

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

- Spring Boot
- MySQL
- Spring Data JPA
- Spring Security + JWT
- Maven

## 项目结构

```
homestay3/
├── homestay-front/       # 用户前端
├── homestay-admin/       # 管理员前端
├── homestay-backend/     # 后端服务API
└── homestay3/            # 项目核心模块
```

## 功能特点

- **用户端**

  - 房源浏览与搜索
  - 在线预订系统
  - 用户账户管理
  - 订单历史查询
  - 评价与反馈

- **管理员端**

  - 房源管理
  - 订单管理
  - 用户管理
  - 数据统计与分析
  - 系统设置

- **后端服务**
  - RESTful API
  - 用户认证与授权
  - 数据持久化
  - 业务逻辑处理
  - 文件上传与管理

## 安装与使用

### 前端环境要求

- Node.js 14.18+
- npm 或 yarn

### 后端环境要求

- JDK 11+
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

# 管理员前端
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

本项目采用 MIT 许可证 - 详情请参阅 [LICENSE](LICENSE) 文件
