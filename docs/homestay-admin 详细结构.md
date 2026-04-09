# homestay-admin 管理后台详细结构

> [!info] 管理后台概览
> 基于 Vue 3 + TypeScript + Vite 构建的现代化管理后台

## 目录结构

```
homestay-admin/
├── src/
│   ├── api/              # API 接口定义
│   ├── assets/           # 静态资源（图片、样式等）
│   ├── components/       # 可复用组件
│   ├── config/           # 配置文件
│   ├── docs/             # 文档
│   ├── layout/           # 布局组件
│   ├── router/           # 路由配置
│   ├── stores/           # 状态管理（Pinia）
│   ├── types/            # TypeScript 类型定义
│   ├── utils/            # 工具函数
│   ├── views/            # 页面视图
│   ├── App.vue           # 根组件
│   └── main.ts           # 入口文件
├── public/               # 公共资源
├── auto-imports.d.ts     # 自动导入类型声明
├── components.d.ts       # 组件类型声明
├── index.html            # HTML 模板
├── package.json          # 项目依赖
├── vite.config.ts        # Vite 配置
└── tsconfig.json         # TypeScript 配置
```

## 核心目录说明

### 📡 api/
- 存放所有后端 API 接口调用
- 统一的请求封装和响应处理

### 🧩 components/
- 可复用的 Vue 组件
- 按功能模块组织

### 🗺️ router/
- Vue Router 路由配置
- 权限控制和路由守卫

### 📦 stores/
- Pinia 状态管理
- 全局状态和业务逻辑

### 📄 views/
- 页面级组件
- 按业务模块划分

### 🔧 utils/
- 通用工具函数
- 辅助方法和常量定义

## 技术特点

- ✅ **Composition API** - 使用 `<script setup>` 语法
- ✅ **TypeScript** - 完整的类型支持
- ✅ **自动导入** - unplugin-auto-import 集成
- ✅ **组件注册** - unplugin-vue-components 集成
- ✅ **Vite** - 快速的热重载和构建

## 相关文档

- [[项目结构总览]]
- [[开发环境配置指南]]