# Redis 分页缓存序列化问题修复指南

## 问题描述

系统在推荐民宿分页查询时出现 Redis 缓存序列化错误：

```
org.springframework.data.redis.serializer.SerializationException:
Could not read JSON: Cannot construct instance of `org.springframework.data.domain.PageImpl`
(no Creators, like default constructor, exist): cannot deserialize from Object value
```

**问题根本原因**：

- Spring Data 的`PageImpl`类设计时没有考虑序列化/反序列化需求
- 该类缺少默认构造函数，Jackson 无法正确反序列化
- Redis 缓存试图存储和恢复`Page<HomestayDTO>`对象时失败

## 受影响的 API 接口

- `GET /api/recommendations/popular?page=X&size=Y` - 热门民宿分页
- `GET /api/recommendations/recommended?page=X&size=Y` - 推荐民宿分页

## 修复方案

### 1. 创建可序列化的分页响应 DTO

创建了`PagedResponse<T>`类来替代`PageImpl`：

```java
@Data
public class PagedResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean empty;

    // 包含默认构造函数和@JsonCreator注解
    // 提供便捷构造函数用于快速创建分页响应
}
```

### 2. 修改推荐服务接口

**修改前**：

```java
Page<HomestayDTO> getRecommendedHomestaysPage(Pageable pageable);
Page<HomestayDTO> getPopularHomestaysPage(Pageable pageable);
```

**修改后**：

```java
PagedResponse<HomestayDTO> getRecommendedHomestaysPage(Pageable pageable);
PagedResponse<HomestayDTO> getPopularHomestaysPage(Pageable pageable);
```

### 3. 禁用分页方法的缓存

由于分页数据变化频繁且缓存意义不大，注释掉了分页方法的`@Cacheable`注解：

```java
// @Cacheable(value = "recommendedHomestaysPage", key = "#pageable.pageNumber + '_' + #pageable.pageSize")
public PagedResponse<HomestayDTO> getRecommendedHomestaysPage(Pageable pageable) {
    // ... 实现代码
}
```

### 4. 更新缓存配置

在`CacheConfig.java`中保留了分页缓存配置（备用），但实际不会使用：

```java
// 推荐民宿分页缓存 - 30分钟（暂不启用）
cacheConfigurations.put("recommendedHomestaysPage", createCacheConfiguration(Duration.ofMinutes(30)));
// 热门民宿分页缓存 - 15分钟（暂不启用）
cacheConfigurations.put("popularHomestaysPage", createCacheConfiguration(Duration.ofMinutes(15)));
```

### 5. 控制器层适配

控制器将`PagedResponse`转换为标准的`Page`对象返回给前端：

```java
PagedResponse<HomestayDTO> pagedResponse = recommendationService.getRecommendedHomestaysPage(pageable);
Page<HomestayDTO> page = pagedResponse.toPage(pageable);
return ResponseEntity.ok(page);
```

## 立即修复步骤

### 1. 清理问题缓存

**方法一：使用 API 清理**

```bash
# 清理所有推荐相关缓存
curl -X POST "http://localhost:8080/api/cache/clear-recommendation"

# 或清理所有缓存
curl -X POST "http://localhost:8080/api/cache/clear-all"
```

**方法二：重启 Redis 服务**

```bash
# Windows
net stop redis
net start redis

# 或直接重启Redis服务
```

### 2. 重启应用服务

重启后端服务以应用所有修复：

```bash
# 如果正在运行，先停止服务
# 然后重新启动
mvn spring-boot:run
```

### 3. 验证修复

测试推荐民宿分页 API：

```bash
# 测试推荐民宿分页
curl "http://localhost:8080/api/recommendations/recommended?page=0&size=6"

# 测试热门民宿分页
curl "http://localhost:8080/api/recommendations/popular?page=0&size=6"
```

## 技术方案优势

### 1. 彻底解决序列化问题

- `PagedResponse`类专为序列化设计，包含所有必要的注解
- 提供多种构造函数，支持各种创建场景

### 2. 保持 API 兼容性

- 前端仍然接收标准的`Page`对象格式
- 无需修改前端代码

### 3. 性能优化

- 分页查询不再缓存，避免缓存一致性问题
- 减少 Redis 存储压力

### 4. 灵活的缓存策略

- 保留非分页方法的缓存（如`getRecommendedHomestays(limit)`）
- 可根据需要重新启用分页缓存

## 监控和维护

### 1. 缓存监控

使用缓存统计 API 监控缓存状态：

```bash
curl "http://localhost:8080/api/cache/stats"
```

### 2. 性能监控

关注以下指标：

- 推荐 API 响应时间
- Redis 连接状态
- 内存使用情况

### 3. 日志监控

监控以下日志：

- `HomestayRecommendationController` 的错误日志
- Redis 序列化相关错误
- 缓存命中率日志

## 预防措施

### 1. 避免缓存复杂对象

- 不要缓存包含内部类或循环引用的对象
- 优先缓存简单的 DTO 对象

### 2. 缓存键设计

- 使用有意义的缓存键
- 避免键名冲突

### 3. 定期清理

- 设置合理的 TTL（生存时间）
- 定期清理过期缓存

## 故障排查

如果问题仍然存在：

1. **检查 Redis 连接**：确认 Redis 服务正常运行
2. **查看应用日志**：检查启动和运行时错误
3. **验证缓存清理**：确认问题缓存已清除
4. **测试简单 API**：先测试非分页接口

## 备用方案

如果主方案失败，可以考虑：

1. **完全禁用推荐缓存**：在配置中移除所有推荐相关缓存
2. **使用内存缓存**：改用 Caffeine 等内存缓存替代 Redis
3. **自定义序列化器**：为 PageImpl 创建专用的序列化器

---

**修复完成时间**：2025-01-07  
**影响范围**：推荐民宿分页功能  
**风险等级**：低（不影响核心业务功能）
