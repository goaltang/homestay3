import { cancelOrder } from "@/api/order";
import { OrderStatus } from "@/types/order";

/**
 * 订单超时处理工具
 * 用于处理订单在各个状态的超时情况
 */

// 超时配置（单位：毫秒）
export const timeoutConfig: Record<string, number> = {
  // 待确认状态超时时间（24小时）
  [OrderStatus.PENDING]: 24 * 60 * 60 * 1000,

  // 已确认未支付状态超时时间（2小时）
  [OrderStatus.CONFIRMED]: 2 * 60 * 60 * 1000,

  // 已支付未入住状态超时时间（无限制，或根据入住日期自动计算）
  [OrderStatus.PAID]: Infinity,

  // 其他状态无超时限制
  [OrderStatus.CHECKED_IN]: Infinity,
  [OrderStatus.COMPLETED]: Infinity,
  [OrderStatus.CANCELLED_BY_USER]: Infinity,
  [OrderStatus.CANCELLED_BY_HOST]: Infinity,
  [OrderStatus.CANCELLED]: Infinity,
  [OrderStatus.CANCELLED_SYSTEM]: Infinity,
  [OrderStatus.PAYMENT_PENDING]: Infinity,
  [OrderStatus.PAYMENT_FAILED]: Infinity,
  [OrderStatus.REFUND_PENDING]: Infinity,
  [OrderStatus.REFUNDED]: Infinity,
  [OrderStatus.REFUND_FAILED]: Infinity,
  [OrderStatus.READY_FOR_CHECKIN]: Infinity,
};

/**
 * 计算订单超时剩余时间
 * @param orderStatus 订单状态
 * @param createTime 订单创建时间
 * @param confirmTime 订单确认时间
 */
export function calculateRemainingTime(
  orderStatus: OrderStatus,
  createTime: string,
  confirmTime?: string
): number {
  // 获取状态对应的超时时间
  const timeout = timeoutConfig[orderStatus] || Infinity;

  // 如果无超时限制，返回Infinity
  if (timeout === Infinity) {
    return Infinity;
  }

  // 根据不同的状态，计算开始计时的时间点
  let startTime: number;

  switch (orderStatus) {
    case OrderStatus.PENDING:
      // 待确认状态从创建时间开始计时
      startTime = new Date(createTime).getTime();
      break;

    case OrderStatus.CONFIRMED:
      // 已确认状态从确认时间开始计时
      startTime = confirmTime
        ? new Date(confirmTime).getTime()
        : new Date(createTime).getTime();
      break;

    default:
      // 其他状态不考虑超时
      return Infinity;
  }

  // 计算截止时间
  const endTime = startTime + timeout;

  // 计算剩余时间
  const now = new Date().getTime();
  const remaining = endTime - now;

  // 返回剩余时间（如果小于0，表示已超时）
  return Math.max(0, remaining);
}

/**
 * 检查订单是否已超时
 * @param orderStatus 订单状态
 * @param createTime 订单创建时间
 * @param confirmTime 订单确认时间
 */
export function isOrderTimedOut(
  orderStatus: OrderStatus,
  createTime: string,
  confirmTime?: string
): boolean {
  return calculateRemainingTime(orderStatus, createTime, confirmTime) === 0;
}

/**
 * 获取超时倒计时文本
 * @param remainingTime 剩余时间（毫秒）
 */
export function getTimeoutCountdownText(remainingTime: number): string {
  if (remainingTime === Infinity) {
    return "无时间限制";
  }

  if (remainingTime <= 0) {
    return "已超时";
  }

  // 转换为小时、分钟和秒
  const hours = Math.floor(remainingTime / (60 * 60 * 1000));
  const minutes = Math.floor((remainingTime % (60 * 60 * 1000)) / (60 * 1000));
  const seconds = Math.floor((remainingTime % (60 * 1000)) / 1000);

  // 根据剩余时间长度返回不同格式
  if (hours > 0) {
    return `${hours}小时${minutes}分钟后超时`;
  } else if (minutes > 0) {
    return `${minutes}分钟${seconds}秒后超时`;
  } else {
    return `${seconds}秒后超时`;
  }
}

/**
 * 处理订单超时
 * @param orderId 订单ID
 * @param orderStatus 订单状态
 * @param createTime 订单创建时间
 * @param confirmTime 订单确认时间
 */
export async function handleOrderTimeout(
  orderId: number,
  orderStatus: OrderStatus,
  createTime: string,
  confirmTime?: string
): Promise<boolean> {
  // 检查订单是否已超时
  if (!isOrderTimedOut(orderStatus, createTime, confirmTime)) {
    return false;
  }

  // 根据不同的状态执行不同的超时处理
  try {
    switch (orderStatus) {
      case OrderStatus.PENDING:
      case OrderStatus.CONFIRMED:
        // 对于待确认和已确认未支付的订单，超时后自动取消
        await cancelOrder(orderId, "TIMEOUT");
        return true;

      default:
        // 其他状态不做超时处理
        return false;
    }
  } catch (error) {
    console.error("处理订单超时失败:", error);
    return false;
  }
}

/**
 * 设置订单超时自动处理（通常在页面加载时调用）
 * @param orderId 订单ID
 * @param orderStatus 订单状态
 * @param createTime 订单创建时间
 * @param confirmTime 订单确认时间
 * @param onTimeout 超时回调函数
 */
export function setupTimeoutHandler(
  orderId: number,
  orderStatus: OrderStatus,
  createTime: string,
  confirmTime?: string,
  onTimeout?: () => void
): ReturnType<typeof setTimeout> | null {
  // 计算剩余时间
  const remainingTime = calculateRemainingTime(
    orderStatus,
    createTime,
    confirmTime
  );

  // 如果已超时或无超时限制，不设置定时器
  if (remainingTime <= 0 || remainingTime === Infinity) {
    return null;
  }

  // 设置定时器
  const timeoutId = setTimeout(async () => {
    // 处理订单超时
    const handled = await handleOrderTimeout(
      orderId,
      orderStatus,
      createTime,
      confirmTime
    );

    // 如果成功处理了超时并且提供了回调，则调用回调
    if (handled && onTimeout) {
      onTimeout();
    }
  }, remainingTime);

  // 返回定时器ID，便于清理
  return timeoutId;
}
