# 支付按钮显示问题修复

## 问题描述

用户在点击支付后，订单状态变为`PAYMENT_PENDING`，但在"我的订单"页面中，该状态下的订单不显示"立即支付"按钮，导致用户无法继续支付。

## 问题根本原因

前端的 MyOrders.vue 页面中，支付按钮的显示条件只检查：

```javascript
order.status === "CONFIRMED" && order.paymentStatus === "UNPAID";
```

但是后端在创建支付时，会将订单状态从`CONFIRMED`更新为`PAYMENT_PENDING`，导致前端不再显示支付按钮。

## 修复方案

### 1. 更新支付按钮显示条件

在`MyOrders.vue`第 117 行附近，修改模板条件：

```javascript
// 修改前
<template v-if="order.status === 'CONFIRMED' && order.paymentStatus === 'UNPAID'">

// 修改后
<template v-if="(order.status === 'CONFIRMED' && order.paymentStatus === 'UNPAID') || order.status === 'PAYMENT_PENDING'">
```

### 2. 更新状态统计逻辑

在统计函数中添加对`PAYMENT_PENDING`状态的支持：

```javascript
// 待支付 (已确认但未支付 或 支付中状态)
else if ((status === 'CONFIRMED' && (paymentStatus === 'UNPAID' || !paymentStatus)) || status === 'PAYMENT_PENDING') {
    counts.NEED_PAYMENT++
}
```

### 3. 更新过滤逻辑

在`getFilteredOrders`函数中更新过滤条件：

```javascript
case 'NEED_PAYMENT':
    return (status === 'CONFIRMED' && (paymentStatus === 'UNPAID' || !paymentStatus)) || status === 'PAYMENT_PENDING'
```

### 4. 更新倒计时显示

支持`PAYMENT_PENDING`状态也显示支付倒计时：

```javascript
<div v-if="order.status === 'CONFIRMED' || order.status === 'PAYMENT_PENDING'" class="payment-reminder danger small-text">
```

## 修复效果

修复后，当用户点击支付，订单状态变为`PAYMENT_PENDING`时：

- ✅ 仍然显示"立即支付"按钮
- ✅ 订单归类到"待支付"标签页
- ✅ 显示支付倒计时提醒
- ✅ 可以取消订单或查看详情

这样用户就可以继续完成支付流程，解决了支付按钮消失的问题。

## 状态流转说明

正确的订单状态流转应该是：

1. `PENDING` (待确认)
2. `CONFIRMED` (已确认)
3. `PAYMENT_PENDING` (支付中) ← **此时应该仍可支付**
4. `PAID` (已支付)
5. `READY_FOR_CHECKIN` (待入住)
6. `CHECKED_IN` (已入住)
7. `COMPLETED` (已完成)

修复确保了第 3 步`PAYMENT_PENDING`状态下用户仍然可以继续支付。
