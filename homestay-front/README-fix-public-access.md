# 修复首页未登录用户访问问题

## 问题描述

未登录用户访问首页时，页面无法正常显示房源信息，推荐民宿和热门民宿区域显示异常。

## 问题根本原因

request.ts 中的 API 白名单配置不完整，导致首页需要的公共 API 被拦截器要求携带 token，未登录用户的请求被拒绝。

## 修复内容

### 1. 更新 API 白名单

在 `homestay-front/src/utils/request.ts` 中添加了以下公共访问的 API 端点：

```typescript
const whiteList = [
  "/api/auth/login",
  "/api/auth/register",
  "/api/auth/forgot-password",
  "/api/auth/reset-password",
  "/uploads/", // 静态资源路径
  "/api/uploads/", // API静态资源路径
  "/api/files/", // 文件资源路径
  "/api/homestays/search", // 房源搜索
  "/api/homestay-types", // 房源类型
  "/api/homestays/amenities", // 房源设施
  "/api/amenities/", // 设施相关API
  "/api/homestays/", // 房源详情和列表（GET请求）
  "/api/recommendations/", // 推荐相关API
  "/api/locations/", // 地区信息API
];
```

### 2. 优化只读权限控制

更新了只读路径的权限控制逻辑，确保某些路径只有 GET 请求才允许未登录访问：

```typescript
const readOnlyPaths = ["/api/files/", "/api/homestays/"];
if (readOnlyPaths.some((path) => url.startsWith(path)) && method !== "GET") {
  isPathWhitelisted = false;
  console.log(`写入操作需要认证: ${method} ${url}`);
}
```

### 3. 新增的公共 API 端点

- **推荐民宿 API**: `/api/recommendations/popular`, `/api/recommendations/recommended`
- **房源类型 API**: `/api/homestay-types`
- **设施信息 API**: `/api/amenities/by-categories`, `/api/homestays/amenities`
- **地区信息 API**: `/api/locations/provinces`, `/api/locations/cities`, `/api/locations/districts`
- **房源搜索 API**: `/api/homestays/search`
- **房源详情 API**: `/api/homestays/{id}` (仅 GET 请求)

### 4. 创建测试页面

创建了 `TestPublicAccess.vue` 测试页面，用于验证未登录用户是否能正常访问所有必要的公共 API。

访问路径: `/test-public-access`

## 修复效果

- ✅ 未登录用户可以正常访问首页
- ✅ 推荐民宿和热门民宿区域正常显示
- ✅ 搜索功能正常工作
- ✅ 房源详情页面可以正常访问
- ✅ 房源列表页面可以正常浏览
- ✅ 保持了必要的权限控制（写入操作仍需登录）

## 技术要点

1. **白名单机制**: 使用路径前缀匹配，支持精确控制哪些 API 无需认证
2. **读写分离**: GET 请求允许公共访问，POST/PUT/DELETE 仍需认证
3. **向下兼容**: 保持了原有的登录用户功能不受影响
4. **错误处理**: 改进了 API 调用的错误处理和降级方案

## 测试建议

1. 访问 `/test-public-access` 页面验证所有公共 API 是否正常
2. 未登录状态下访问首页，确认推荐和热门民宿正常显示
3. 测试搜索功能是否正常工作
4. 验证房源详情页面访问是否正常
5. 确认登录后的功能依然正常工作
