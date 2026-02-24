# 房东信息 API 需求文档

## 问题现状

前端获取到的房东信息很少，主要问题包括：

1. **现有 API 返回的房东信息有限**

   - 仅包含基本的 `ownerName`, `ownerUsername`, `ownerPhone`
   - 缺少房东 ID、注册时间、房源数量等重要信息

2. **数据结构不统一**

   - 有时返回 `owner` 对象，有时返回扁平字段
   - 房东信息可能在 `owner`, `user`, `host` 等不同字段中

3. **关联数据缺失**
   - 房东账户可能已删除，但房源仍存在
   - 数据同步异常导致关联关系错误

## 解决方案

### 1. 新增专用 API 接口

**建议 API 路径:**

```
GET /api/admin/homestays/{id}/with-owner
```

**返回数据结构:**

```json
{
  "id": 123,
  "title": "房源标题",
  "status": "PENDING",
  // ... 其他房源信息

  // 房东信息 (嵌套对象)
  "owner": {
    "id": 456,
    "username": "landlord001",
    "realName": "张三",
    "nickname": "房东小张",
    "phone": "13800138000",
    "email": "landlord@example.com",
    "createdAt": "2023-01-15T10:30:00Z",
    "status": "ACTIVE",
    "verificationStatus": "VERIFIED",
    "homestayCount": 5,
    "totalReviews": 25,
    "averageRating": 4.8
  },

  // 扁平化房东信息 (兼容现有前端)
  "ownerId": 456,
  "ownerName": "张三",
  "ownerUsername": "landlord001",
  "ownerPhone": "13800138000",
  "ownerJoinDate": "2023-01-15T10:30:00Z",
  "ownerHomestayCount": 5
}
```

### 2. 改进现有 API

**优化 `/api/admin/homestays/{id}` 接口:**

- 确保房东信息的完整性
- 统一数据结构
- 处理房东信息缺失的情况

**数据映射策略:**

```java
// 后端DTO示例
public class HomestayDetailDTO {
    // 房源基本信息
    private Long id;
    private String title;
    // ...

    // 房东信息 (嵌套)
    private OwnerDTO owner;

    // 房东信息 (扁平化)
    private Long ownerId;
    private String ownerName;
    private String ownerUsername;
    private String ownerPhone;
    private String ownerJoinDate;
    private Integer ownerHomestayCount;

    // 数据完整性检查
    private Boolean ownerInfoComplete;
    private String ownerInfoStatus; // "COMPLETE", "PARTIAL", "MISSING"
}

public class OwnerDTO {
    private Long id;
    private String username;
    private String realName;
    private String nickname;
    private String phone;
    private String email;
    private String createdAt;
    private String status;
    private String verificationStatus;
    private Integer homestayCount;
    private Integer totalReviews;
    private Double averageRating;
}
```

### 3. 数据完整性处理

**房东信息缺失的处理策略:**

1. **账户已删除**

   ```json
   {
     "ownerId": null,
     "ownerName": null,
     "ownerInfoStatus": "DELETED",
     "ownerInfoMessage": "房东账户已被删除"
   }
   ```

2. **数据同步异常**

   ```json
   {
     "ownerId": 456,
     "ownerName": null,
     "ownerInfoStatus": "SYNC_ERROR",
     "ownerInfoMessage": "房东信息同步异常，请联系管理员"
   }
   ```

3. **关联关系错误**
   ```json
   {
     "ownerId": 999,
     "ownerName": null,
     "ownerInfoStatus": "RELATION_ERROR",
     "ownerInfoMessage": "房东关联关系错误"
   }
   ```

## 前端适配策略

### 1. API 调用优化

```typescript
// 前端API调用逻辑
export async function getHomestayDetailWithOwner(
  id: number
): Promise<Homestay> {
  try {
    // 优先使用专用API
    const response = await request(`/api/admin/homestays/${id}/with-owner`);
    return adaptHomestayItem(response.data);
  } catch (error) {
    // 降级使用标准API
    console.warn("专用API不可用，使用标准API");
    return getHomestayDetail(id);
  }
}
```

### 2. 数据适配器增强

```typescript
export function adaptHomestayItem(item: any): Homestay {
  // 多源房东信息合并
  const ownerInfo = {
    ownerId: item.ownerId || item.owner?.id || item.user?.id || item.hostId,
    ownerName:
      item.ownerName ||
      item.owner?.realName ||
      item.owner?.nickname ||
      item.user?.realName ||
      item.user?.nickname ||
      item.hostName,
    ownerUsername:
      item.ownerUsername || item.owner?.username || item.user?.username,
    ownerPhone: item.ownerPhone || item.owner?.phone || item.user?.phone,
    ownerJoinDate:
      item.ownerJoinDate || item.owner?.createdAt || item.user?.createdAt,
    ownerHomestayCount:
      item.ownerHomestayCount ||
      item.owner?.homestayCount ||
      item.user?.homestayCount,
  };

  return {
    // ... 其他字段
    ...ownerInfo,
  };
}
```

## 后端实现建议

### 1. Service 层增强

```java
@Service
public class HomestayService {

    @Autowired
    private UserService userService;

    public HomestayDetailDTO getHomestayWithOwner(Long homestayId) {
        Homestay homestay = homestayRepository.findById(homestayId)
            .orElseThrow(() -> new EntityNotFoundException("房源不存在"));

        HomestayDetailDTO dto = convertToDTO(homestay);

        // 获取房东信息
        if (homestay.getOwnerId() != null) {
            try {
                User owner = userService.findById(homestay.getOwnerId());
                if (owner != null) {
                    dto.setOwner(convertToOwnerDTO(owner));
                    setFlatOwnerInfo(dto, owner);
                    dto.setOwnerInfoStatus("COMPLETE");
                } else {
                    dto.setOwnerInfoStatus("DELETED");
                    dto.setOwnerInfoMessage("房东账户已被删除");
                }
            } catch (Exception e) {
                dto.setOwnerInfoStatus("SYNC_ERROR");
                dto.setOwnerInfoMessage("房东信息获取失败: " + e.getMessage());
            }
        } else {
            dto.setOwnerInfoStatus("MISSING");
            dto.setOwnerInfoMessage("房东ID缺失");
        }

        return dto;
    }

    private void setFlatOwnerInfo(HomestayDetailDTO dto, User owner) {
        dto.setOwnerId(owner.getId());
        dto.setOwnerName(owner.getRealName() != null ? owner.getRealName() : owner.getNickname());
        dto.setOwnerUsername(owner.getUsername());
        dto.setOwnerPhone(owner.getPhone());
        dto.setOwnerJoinDate(owner.getCreatedAt());
        dto.setOwnerHomestayCount(homestayRepository.countByOwnerId(owner.getId()));
    }
}
```

### 2. Controller 增强

```java
@RestController
@RequestMapping("/api/admin/homestays")
public class AdminHomestayController {

    @GetMapping("/{id}/with-owner")
    public Result<HomestayDetailDTO> getHomestayWithOwner(@PathVariable Long id) {
        HomestayDetailDTO homestay = homestayService.getHomestayWithOwner(id);
        return Result.success(homestay);
    }
}
```

## 数据库优化建议

### 1. 添加外键约束

```sql
-- 确保房源和房东的关联关系
ALTER TABLE homestays
ADD CONSTRAINT fk_homestay_owner
FOREIGN KEY (owner_id) REFERENCES users(id)
ON DELETE SET NULL ON UPDATE CASCADE;
```

### 2. 添加索引

```sql
-- 优化房东信息查询
CREATE INDEX idx_homestay_owner_id ON homestays(owner_id);
CREATE INDEX idx_user_homestay_count ON users(id)
INCLUDE (username, real_name, phone, created_at);
```

### 3. 数据完整性检查

```sql
-- 检查孤儿房源
SELECT h.id, h.title, h.owner_id
FROM homestays h
LEFT JOIN users u ON h.owner_id = u.id
WHERE h.owner_id IS NOT NULL AND u.id IS NULL;

-- 检查房东房源数量
SELECT u.id, u.username, COUNT(h.id) as homestay_count
FROM users u
LEFT JOIN homestays h ON u.id = h.owner_id
GROUP BY u.id, u.username;
```

## 测试建议

### 1. API 测试用例

- 正常房源（有完整房东信息）
- 房东已删除的房源
- 房东 ID 为空的房源
- 房东信息不完整的房源

### 2. 前端测试场景

- 房东信息完整显示
- 房东信息缺失警告
- 重新获取房东信息功能
- 原始数据调试面板

## 总结

通过以上改进，可以显著提升房东信息的完整性和可靠性：

1. **数据完整性** - 确保房东信息的完整获取
2. **错误处理** - 妥善处理各种异常情况
3. **用户体验** - 提供清晰的错误提示和调试信息
4. **系统健壮性** - 增强数据关联和错误恢复能力

这样的设计既解决了当前房东信息缺失的问题，又为未来的扩展提供了良好的基础。
