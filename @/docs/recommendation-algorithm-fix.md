# 推荐民宿显示数量不足问题修复

## 问题分析

用户反馈首页推荐民宿只显示了 4 个，而不是预期的 6 个。经过代码分析发现问题出现在`HomestayRecommendationServiceImpl.getRecommendedHomestays()`方法中。

### 原始算法的问题

原始算法的逻辑：

1. 取质量优先房源：`limit * 2 / 3` = 4 个（当 limit=6 时）
2. 取新房源：最多 2 个
3. 合并后最终结果

**问题**：

- 如果数据库中活跃房源数量不足 6 个，会导致显示数量少于 6 个
- 质量优先房源被硬性限制在 4 个，即使有更多符合条件的房源
- 新房源筛选可能找不到足够的符合条件的房源（60 天内创建）

## 修复方案

### 改进后的算法逻辑

```java
// 1. 计算所有房源的推荐分数
List<RecommendationScore> allScores = activeHomestays.stream()
    .map(this::calculateRecommendationScore)
    .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
    .collect(Collectors.toList());

// 2. 如果总数不足请求数量，直接返回所有的
if (allScores.size() <= limit) {
    return allScores.stream()
        .map(score -> convertToDTO(score.getHomestay()))
        .collect(Collectors.toList());
}

// 3. 数量足够时，进行多样性筛选
// - 质量优先房源（最多2/3）
// - 新房源（1/3）
// - 如果还不够，从剩余房源补充
```

### 关键改进点

1. **保证数量**：优先确保返回足够的数量，再考虑多样性
2. **动态补充**：如果分类筛选后数量不足，自动从剩余房源补充
3. **避免重复**：使用`selectedIds`集合确保不重复选择同一房源
4. **算法一致性**：保持相同的评分算法，确保质量

### 代码修改要点

```java
// 如果总数不足，直接返回所有
if (allScores.size() <= limit) {
    return allScores.stream()
        .map(score -> convertToDTO(score.getHomestay()))
        .collect(Collectors.toList());
}

// 动态补充机制
if (result.size() < limit) {
    result.addAll(allScores.stream()
        .filter(score -> !selectedIds.contains(score.getHomestay().getId()))
        .limit(limit - result.size())
        .map(score -> convertToDTO(score.getHomestay()))
        .collect(Collectors.toList()));
}
```

## 测试验证

修复后的算法会：

1. ✅ 在房源充足时返回 6 个推荐民宿
2. ✅ 在房源不足时返回所有可用的推荐民宿
3. ✅ 保持多样性筛选逻辑（当数量充足时）
4. ✅ 避免重复推荐同一房源

## 影响范围

- **首页推荐民宿组件**：现在会显示更多推荐内容
- **API 响应**：`/api/recommendations/recommended?limit=6`会返回最多 6 个结果
- **缓存**：建议清除推荐缓存以应用新算法

## 部署建议

1. 重新编译部署后端应用
2. 清除推荐相关缓存：`/api/recommendations/refresh-cache`
3. 验证首页推荐民宿是否显示 6 个（或数据库中的最大可用数量）
