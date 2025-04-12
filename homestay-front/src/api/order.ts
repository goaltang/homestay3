import request from "@/utils/request";

/**
 * 创建订单
 * @param data 订单数据
 */
export function createOrder(data: {
  homestayId: number;
  checkInDate: string;
  checkOutDate: string;
  guestCount: number;
  totalPrice: number;
  guestName: string;
  guestPhone: string;
  message?: string;
}) {
  console.log("创建订单，参数:", data);
  return request({
    url: "/api/orders",
    method: "post",
    data,
  })
    .then((response) => {
      console.log("订单创建成功:", response.data);
      return response;
    })
    .catch((error) => {
      console.error("订单创建失败:", error);
      throw error;
    });
}

/**
 * 获取用户的订单列表
 * @param params 查询参数
 */
export function getUserOrders(params?: {
  page?: number;
  size?: number;
  status?: string;
}) {
  console.log("获取用户订单列表，参数:", params);
  return request({
    url: "/api/orders",
    method: "get",
    params,
  })
    .then((response) => {
      console.log("获取用户订单成功:", response.data);
      return response;
    })
    .catch((error) => {
      console.error("获取用户订单失败:", error);

      // 开发模式下提供模拟数据
      if (process.env.NODE_ENV === "development") {
        console.warn("开发模式：使用模拟订单数据");
        const mockOrders = [
          {
            id: 1,
            orderNumber: "ORD" + Date.now().toString().substring(5),
            homestayId: 1,
            homestayTitle: "湖景度假房",
            imageUrl: "https://picsum.photos/400/300?random=1",
            guestCount: 2,
            checkInDate: new Date(Date.now() + 86400000 * 7)
              .toISOString()
              .split("T")[0],
            checkOutDate: new Date(Date.now() + 86400000 * 10)
              .toISOString()
              .split("T")[0],
            nights: 3,
            totalAmount: 1200,
            status: "PENDING",
            createTime: new Date().toISOString(),
          },
          {
            id: 2,
            orderNumber: "ORD" + (Date.now() - 1000000).toString().substring(5),
            homestayId: 2,
            homestayTitle: "山景小木屋",
            imageUrl: "https://picsum.photos/400/300?random=2",
            guestCount: 4,
            checkInDate: new Date(Date.now() + 86400000 * 14)
              .toISOString()
              .split("T")[0],
            checkOutDate: new Date(Date.now() + 86400000 * 17)
              .toISOString()
              .split("T")[0],
            nights: 3,
            totalAmount: 1800,
            status: "CONFIRMED",
            createTime: new Date(Date.now() - 86400000 * 2).toISOString(),
          },
        ];

        // 根据传入的状态参数过滤订单
        let filteredOrders = mockOrders;
        if (params && params.status && params.status !== "all") {
          filteredOrders = mockOrders.filter(
            (order) => order.status === params.status
          );
        }

        return {
          data: {
            content: filteredOrders,
            totalElements: filteredOrders.length,
            totalPages: 1,
            size: filteredOrders.length,
            number: 0,
          },
        };
      }

      throw error;
    });
}

/**
 * 获取订单详情
 * @param id 订单ID
 */
export function getOrderById(id: number) {
  return request({
    url: `/api/orders/${id}`,
    method: "get",
  });
}

/**
 * 取消订单
 * @param id 订单ID
 */
export function cancelOrder(id: number) {
  console.log("取消订单，ID:", id);
  return request({
    url: `/api/orders/${id}/cancel`,
    method: "put",
  })
    .then((response) => {
      console.log("订单取消成功:", response.data);
      return response;
    })
    .catch((error) => {
      console.error("订单取消失败:", error);
      throw error;
    });
}

// 获取订单列表
export function getOrders(params?: any) {
  return request({
    url: "/api/orders",
    method: "get",
    params,
  });
}

/**
 * 支付订单
 * @param id 订单ID
 * @param paymentMethod 支付方式
 */
export function payOrder(id: number, paymentMethod: string) {
  return request({
    url: `/api/orders/${id}/pay`,
    method: "post",
    data: { paymentMethod },
  });
}

/**
 * 评价订单
 * @param id 订单ID
 * @param data 评价数据
 */
export function reviewOrder(id: number, data: any) {
  return request({
    url: `/api/orders/${id}/review`,
    method: "post",
    data,
  });
}

// 获取待处理订单数量
export function getPendingOrderCount() {
  return request({
    url: "/api/orders/pending/count",
    method: "get",
  });
}

/**
 * 创建订单预览
 * @param data 订单预览数据
 */
export function createOrderPreview(data: {
  homestayId: number;
  checkInDate: string;
  checkOutDate: string;
  guestCount: number;
  guestPhone?: string;
  message?: string;
}) {
  console.log("创建订单预览，参数:", data);
  return request({
    url: "/api/orders/preview",
    method: "post",
    data,
  })
    .then((response) => {
      console.log("订单预览创建成功:", response.data);
      return response;
    })
    .catch((error) => {
      console.error("订单预览创建失败:", error);
      throw error;
    });
}

// 获取订单详情
export function getOrderDetail(orderId: string | number) {
  return request({
    url: `/api/orders/${orderId}`,
    method: "get",
  });
}

// 生成支付二维码
export function generatePaymentQRCode(data: {
  orderId: number;
  method: string;
}) {
  return request({
    url: `/api/orders/${data.orderId}/payment/qrcode`,
    method: "post",
    data: { method: data.method },
  });
}

// 检查支付状态
export function checkPayment(orderId: number) {
  return request({
    url: `/api/orders/${orderId}/payment/status`,
    method: "get",
  });
}

// 模拟支付成功（仅用于测试）
export function mockPayment(orderId: number) {
  return request({
    url: `/api/orders/${orderId}/payment/mock`,
    method: "post",
  });
}

// 获取订单列表
export function getOrderList(params?: {
  status?: string;
  page?: number;
  size?: number;
}) {
  return request({
    url: "/api/orders",
    method: "get",
    params,
  });
}

/**
 * 获取我的订单列表
 * @param params 查询参数
 */
export function getMyOrders(params?: {
  page?: number;
  size?: number;
  status?: string;
}) {
  console.log("获取我的订单列表，参数:", params);
  return request({
    url: "/api/orders",
    method: "get",
    params,
  })
    .then((response) => {
      console.log("获取我的订单成功:", response.data);

      // 处理不同格式的响应
      let result = response.data;

      // 处理嵌套响应
      if (response.data && response.data.data) {
        result = response.data;
      }
      // 处理直接返回数组的情况
      else if (Array.isArray(response.data)) {
        result = {
          data: {
            content: response.data,
            totalElements: response.data.length,
            totalPages: 1,
            size: response.data.length,
            number: 0,
          },
        };
      }
      // 处理简化的分页响应
      else if (response.data && response.data.content) {
        result = {
          data: response.data,
        };
      }

      return { data: result };
    })
    .catch((error) => {
      console.error("获取我的订单失败:", error);

      // 开发模式下提供模拟数据
      if (process.env.NODE_ENV === "development") {
        console.warn("开发模式：使用模拟订单数据");
        const mockOrders = [
          {
            id: 1,
            orderNumber: "ORD" + Date.now().toString().substring(5),
            homestayId: 1,
            homestayTitle: "湖景度假房",
            imageUrl: "https://picsum.photos/400/300?random=1",
            location: "杭州西湖区",
            guestCount: 2,
            checkInDate: new Date(Date.now() + 86400000 * 7)
              .toISOString()
              .split("T")[0],
            checkOutDate: new Date(Date.now() + 86400000 * 10)
              .toISOString()
              .split("T")[0],
            nights: 3,
            totalAmount: 1200,
            price: 350,
            cleaningFee: 100,
            serviceFee: 50,
            status: "PENDING",
            createTime: new Date().toISOString(),
          },
          {
            id: 2,
            orderNumber: "ORD" + (Date.now() - 1000000).toString().substring(5),
            homestayId: 2,
            homestayTitle: "山景小木屋",
            imageUrl: "https://picsum.photos/400/300?random=2",
            location: "莫干山度假区",
            guestCount: 4,
            checkInDate: new Date(Date.now() + 86400000 * 14)
              .toISOString()
              .split("T")[0],
            checkOutDate: new Date(Date.now() + 86400000 * 17)
              .toISOString()
              .split("T")[0],
            nights: 3,
            totalAmount: 1800,
            price: 500,
            cleaningFee: 150,
            serviceFee: 150,
            status: "CONFIRMED",
            createTime: new Date(Date.now() - 86400000 * 2).toISOString(),
          },
          {
            id: 3,
            orderNumber: "ORD" + (Date.now() - 2000000).toString().substring(5),
            homestayId: 3,
            homestayTitle: "海滨别墅",
            imageUrl: "https://picsum.photos/400/300?random=3",
            location: "三亚亚龙湾",
            guestCount: 6,
            checkInDate: new Date(Date.now() - 86400000 * 10)
              .toISOString()
              .split("T")[0],
            checkOutDate: new Date(Date.now() - 86400000 * 5)
              .toISOString()
              .split("T")[0],
            nights: 5,
            totalAmount: 3500,
            price: 600,
            cleaningFee: 200,
            serviceFee: 200,
            status: "COMPLETED",
            createTime: new Date(Date.now() - 86400000 * 15).toISOString(),
          },
        ];

        // 根据传入的状态参数过滤订单
        let filteredOrders = mockOrders;
        if (params && params.status && params.status !== "all") {
          filteredOrders = mockOrders.filter(
            (order) => order.status === params.status
          );
        }

        return {
          data: {
            data: {
              content: filteredOrders,
              totalElements: filteredOrders.length,
              totalPages: 1,
              size: filteredOrders.length,
              number: 0,
            },
          },
        };
      }

      throw error;
    });
}
