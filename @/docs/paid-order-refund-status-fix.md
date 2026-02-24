# 已支付订单取消状态显示修复

## 问题描述

用户反馈：当用户或房东在订单支付后发起退款时，用户端的"我的订单"和房东端的"订单管理"中状态显示都只是"已取消"，这不够准确，应该显示"退款中"等更具体的状态。

## 问题分析

### 根本原因

1. **后端逻辑问题**：在 `OrderServiceImpl.processCancelOrder()` 方法中，已支付订单被取消时：

   - 订单状态(`status`)仍然设置为 `CANCELLED_BY_USER` 或 `CANCELLED_BY_HOST`
   - 只有支付状态(`paymentStatus`)设置为 `REFUND_PENDING`
   - 前端优先显示订单状态，导致显示"已取消"而非"退款中"

2. **业务逻辑不合理**：已支付订单的取消应该进入退款流程，状态应该是 `REFUND_PENDING` 而不是各种取消状态

## 修复方案

### 1. 后端修复 - 优化取消逻辑

修改 `OrderServiceImpl.processCancelOrder()` 方法：

```java
// 特殊处理：如果是已支付订单被取消，应该进入退款流程
if (order.getPaymentStatus() == PaymentStatus.PAID) {
    log.info("已支付订单被取消，将进入退款流程，订单号: {}", order.getOrderNumber());

    // 已支付订单取消时，状态应该是REFUND_PENDING，而不是各种取消状态
    targetStatus = OrderStatus.REFUND_PENDING;

    // 更新订单状态和支付状态
    order.setStatus(targetStatus.name());
    order.setPaymentStatus(PaymentStatus.REFUND_PENDING);

    // 添加退款原因到备注
    String refundReason = String.format("退款申请 - 原因: %s",
        reason != null && !reason.isEmpty() ? reason : "用户申请");
    // ... 更新备注逻辑
}
```

**核心改进**：

- 已支付订单被取消时，直接将订单状态设置为 `REFUND_PENDING`
- 不再使用 `CANCELLED_BY_USER/HOST` 等取消状态
- 同时更新支付状态为 `REFUND_PENDING`

### 2. 通知系统优化

改进通知消息内容：

```java
if (cancelledOrder.getStatus().equals(OrderStatus.REFUND_PENDING.name())) {
    // 退款通知
    notificationContent = String.format(
        "您的订单 %s (房源: %s) 退款申请已提交，我们将在1-3个工作日内处理。原因: %s",
        cancelledOrder.getOrderNumber(),
        cancelledOrder.getHomestay().getTitle(),
        reason != null && !reason.isEmpty() ? reason : "未提供原因"
    );
} else {
    // 取消通知 (用于未支付订单)
    // ...
}
```

### 3. 前端状态映射完善

#### 用户订单页面 (MyOrders.vue)

`getStatusText()` 函数已正确处理：

```typescript
// 优先处理最终/关键状态
if (status === "REFUND_PENDING") return "退款中";
if (status === "REFUNDED" || paymentStatus === "REFUNDED") return "已退款";
if (status === "REFUND_FAILED") return "退款失败";
```

#### 房东订单管理页面 (OrderManage.vue)

添加退款状态支持：

```typescript
const statusMap: Record<string, string> = {
  // ... 其他状态
  REFUND_PENDING: "退款中",
  REFUNDED: "已退款",
  REFUND_FAILED: "退款失败",
};
```

## 修复效果

### 修复前

- 用户支付后申请退款 → 显示"已取消"
- 房东取消已支付订单 → 显示"已取消"
- 用户体验差，不知道退款进度

### 修复后

- 用户支付后申请退款 → 显示"退款中"
- 房东取消已支付订单 → 显示"退款中"
- 后续退款完成 → 显示"已退款"
- 退款失败 → 显示"退款失败"

## 业务流程

```mermaid
graph TD
    A[已支付订单] --> B{申请取消}
    B --> C[后端处理]
    C --> D{检查支付状态}
    D -->|已支付| E[设置状态为REFUND_PENDING]
    D -->|未支付| F[设置为相应取消状态]
    E --> G[前端显示"退款中"]
    F --> H[前端显示"已取消"]
    G --> I[管理员处理退款]
    I --> J{退款结果}
    J -->|成功| K[状态更新为REFUNDED]
    J -->|失败| L[状态更新为REFUND_FAILED]
    K --> M[前端显示"已退款"]
    L --> N[前端显示"退款失败"]
```

## 技术要点

1. **状态流转规则**：确保 `PAID` 和 `READY_FOR_CHECKIN` 可以转换为 `REFUND_PENDING`
2. **通知类型**：使用 `NotificationType.REFUND_REQUESTED` 区分退款通知
3. **前端容错**：完善状态映射，处理所有可能的退款状态
4. **日志记录**：增强日志输出，便于问题追踪

## 验证测试

### 测试用例

1. **用户申请退款**：

   - 用户支付订单后点击"申请退款"
   - 验证状态显示为"退款中"而非"已取消"

2. **房东取消已支付订单**：

   - 房东取消用户已支付的订单
   - 验证状态显示为"退款中"

3. **未支付订单取消**：
   - 取消未支付订单
   - 验证状态仍显示"已取消"（正常取消流程）

### 预期结果

- ✅ 已支付订单取消后正确显示"退款中"
- ✅ 用户和房东都能看到准确的退款状态
- ✅ 通知内容更加准确和用户友好
- ✅ 业务流程更加符合实际情况

## 总结

通过修改后端取消逻辑，将已支付订单的取消操作直接转换为退款流程，并完善前端状态显示，解决了状态显示不准确的问题。现在用户和房东都能清楚地了解退款进度，提升了整体用户体验。
