package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.OrderDTO;
import com.homestay3.homestaybackend.dto.refund.RefundRequest;
import com.homestay3.homestaybackend.dto.refund.RefundResponse;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.exception.AccessDeniedException;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.model.HomestayStatus;
import com.homestay3.homestaybackend.entity.Order;
import com.homestay3.homestaybackend.model.OrderStatus;
import com.homestay3.homestaybackend.model.PaymentStatus;
import com.homestay3.homestaybackend.model.RefundType;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.repository.ReviewRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.BookingConflictService;
import com.homestay3.homestaybackend.service.EarningService;
import com.homestay3.homestaybackend.service.PaymentService;
import com.homestay3.homestaybackend.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OrderService 单元测试
 * 测试订单核心业务逻辑
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HomestayRepository homestayRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EarningService earningService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookingConflictService bookingConflictService;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User currentUser;
    private User hostUser;
    private Homestay homestay;
    private Order order;
    private OrderDTO orderDTO;

    @BeforeEach
    void setUp() {
        // 设置当前用户
        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("testuser");
        currentUser.setRole("ROLE_USER");

        // 设置房东用户
        hostUser = new User();
        hostUser.setId(2L);
        hostUser.setUsername("hostuser");
        hostUser.setRole("ROLE_HOST");

        // 设置房源
        homestay = new Homestay();
        homestay.setId(1L);
        homestay.setTitle("测试民宿");
        homestay.setPrice(new BigDecimal("200"));
        homestay.setStatus(HomestayStatus.ACTIVE);
        homestay.setMinNights(1);
        homestay.setOwner(hostUser);
        homestay.setAutoConfirm(false);

        // 设置订单
        order = new Order();
        order.setId(1L);
        order.setOrderNumber("ORD202401010001");
        order.setHomestay(homestay);
        order.setGuest(currentUser);
        order.setGuestPhone("13800138000");
        order.setCheckInDate(LocalDate.now().plusDays(1));
        order.setCheckOutDate(LocalDate.now().plusDays(3));
        order.setNights(2);
        order.setGuestCount(2);
        order.setPrice(new BigDecimal("200"));
        order.setTotalAmount(new BigDecimal("400"));
        order.setStatus(OrderStatus.PENDING.name());
        order.setPaymentStatus(PaymentStatus.UNPAID);

        // 设置订单DTO
        orderDTO = OrderDTO.builder()
                .homestayId(1L)
                .guestPhone("13800138000")
                .checkInDate(LocalDate.now().plusDays(1))
                .checkOutDate(LocalDate.now().plusDays(3))
                .guestCount(2)
                .build();
    }

    private void mockSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(currentUser.getUsername());

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername(currentUser.getUsername())).thenReturn(Optional.of(currentUser));
    }

    @Test
    void getOrderById_Success() {
        // 准备
        mockSecurityContext();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // 执行
        OrderDTO result = orderService.getOrderById(1L);

        // 验证
        assertNotNull(result);
        assertEquals(order.getId(), result.getId());
        assertEquals(order.getOrderNumber(), result.getOrderNumber());
    }

    @Test
    void getOrderById_OrderNotFound() {
        // 准备
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // 执行和验证
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.getOrderById(999L);
        });
    }

    @Test
    void getOrderByOrderNumber_Success() {
        // 准备
        mockSecurityContext();
        when(orderRepository.findByOrderNumber("ORD202401010001")).thenReturn(Optional.of(order));

        // 执行
        OrderDTO result = orderService.getOrderByOrderNumber("ORD202401010001");

        // 验证
        assertNotNull(result);
        assertEquals(order.getOrderNumber(), result.getOrderNumber());
    }

    @Test
    void updateOrderStatus_Success_Confirmed() {
        // 准备
        mockSecurityContext();
        when(userRepository.findByUsername(hostUser.getUsername())).thenReturn(Optional.of(hostUser));
        order.setStatus(OrderStatus.PENDING.name());
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // 模拟当前用户是房东
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(hostUser.getUsername());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // 执行
        OrderDTO result = orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED.name());

        // 验证
        assertNotNull(result);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void cancelOrder_Success() {
        // 准备
        mockSecurityContext();
        order.setGuest(currentUser);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // 执行
        OrderDTO result = orderService.cancelOrder(1L);

        // 验证
        assertNotNull(result);
        assertTrue(result.getStatus().contains("CANCELLED") || result.getStatus().contains("REFUND_PENDING"));
    }

    @Test
    void payOrder_Success() {
        // 准备
        mockSecurityContext();
        order.setStatus(OrderStatus.CONFIRMED.name());
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(earningService.generatePendingEarningForOrder(anyLong())).thenReturn(null);

        // 执行
        OrderDTO result = orderService.payOrder(1L);

        // 验证
        assertNotNull(result);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void createOrderPreview_Success() {
        // 准备
        mockSecurityContext();
        when(homestayRepository.findById(1L)).thenReturn(Optional.of(homestay));
        when(orderRepository.existsOverlappingBooking(any(), any(), any())).thenReturn(false);

        // 执行
        OrderDTO result = orderService.createOrderPreview(orderDTO);

        // 验证
        assertNotNull(result);
        assertEquals(new BigDecimal("400"), result.getTotalAmount()); // 200 * 2晚
        assertEquals(2, result.getNights());
    }

    @Test
    void createOrderPreview_InvalidCheckInDate() {
        // 准备
        mockSecurityContext();
        orderDTO.setCheckInDate(LocalDate.now().minusDays(1)); // 过去的日期

        // 执行和验证
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrderPreview(orderDTO);
        });
    }

    @Test
    void createOrderPreview_InvalidDateRange() {
        // 准备
        mockSecurityContext();
        orderDTO.setCheckInDate(LocalDate.now().plusDays(3));
        orderDTO.setCheckOutDate(LocalDate.now().plusDays(1)); // 退房早于入住

        // 执行和验证
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrderPreview(orderDTO);
        });
    }

    @Test
    void systemCancelOrder_Success() {
        // 准备
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // 执行（系统取消不需要用户权限检查）
        OrderDTO result = orderService.systemCancelOrder(1L, OrderStatus.CANCELLED_SYSTEM.name(), "系统超时自动取消");

        // 验证
        assertNotNull(result);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    // ============================================================
    // 退款功能测试
    // ============================================================

    // --- 用户申请退款 requestUserRefund ---

    @Test
    void requestUserRefund_Success() {
        // 准备 - 已支付订单
        mockSecurityContext();
        order.setStatus(OrderStatus.PAID.name());
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setTotalAmount(new BigDecimal("400"));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行
        OrderDTO result = orderService.requestUserRefund(1L, "行程变更");

        // 验证
        assertNotNull(result);
        assertEquals(OrderStatus.REFUND_PENDING.name(), result.getStatus());
        assertEquals(PaymentStatus.REFUND_PENDING.name(), result.getPaymentStatus());
        assertEquals(RefundType.USER_REQUESTED.name(), result.getRefundType());
        assertEquals("行程变更", result.getRefundReason());
        assertNotNull(result.getRefundAmount());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void requestUserRefund_WithReason() {
        // 准备
        mockSecurityContext();
        order.setStatus(OrderStatus.PAID.name());
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setTotalAmount(new BigDecimal("400"));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行 - 不提供原因
        OrderDTO result = orderService.requestUserRefund(1L, null);

        // 验证 - 应该使用默认原因
        assertNotNull(result);
        assertEquals("用户申请退款", result.getRefundReason());
    }

    @Test
    void requestUserRefund_NotPaid() {
        // 准备 - 未支付订单
        mockSecurityContext();
        order.setStatus(OrderStatus.PENDING.name());
        order.setPaymentStatus(PaymentStatus.UNPAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // 执行和验证 - 应抛出异常
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            orderService.requestUserRefund(1L, "想退款");
        });
        assertTrue(exception.getMessage().contains("已支付"));
    }

    @Test
    void requestUserRefund_AlreadyRefunding() {
        // 准备 - 订单已在退款流程中
        // 注意：paymentStatus 是 PAID（通过第一个检查），但 status 是 REFUND_PENDING（触发第二个检查）
        order.setStatus(OrderStatus.REFUND_PENDING.name());
        order.setPaymentStatus(PaymentStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // 执行和验证 - 应抛出异常
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            orderService.requestUserRefund(1L, "再次申请");
        });
        assertTrue(exception.getMessage().contains("退款流程"));
    }

    @Test
    void requestUserRefund_OrderNotFound() {
        // 准备
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // 执行和验证
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.requestUserRefund(999L, "退款");
        });
    }

    // --- 管理员发起退款 initiateRefund ---

    @Test
    void initiateRefund_Success() {
        // 准备 - 已支付订单
        order.setStatus(OrderStatus.PAID.name());
        order.setPaymentStatus(PaymentStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行
        OrderDTO result = orderService.initiateRefund(1L);

        // 验证
        assertNotNull(result);
        assertEquals(PaymentStatus.REFUND_PENDING.name(), result.getPaymentStatus());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void initiateRefund_NotPaid() {
        // 准备 - 未支付订单
        order.setPaymentStatus(PaymentStatus.UNPAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // 执行和验证
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            orderService.initiateRefund(1L);
        });
        assertTrue(exception.getMessage().contains("已支付"));
    }

    // --- 管理员批准退款 approveRefund ---

    @Test
    void approveRefund_Success() {
        // 准备 - 退款中的订单
        mockSecurityContext();
        order.setStatus(OrderStatus.REFUND_PENDING.name());
        order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        order.setTotalAmount(new BigDecimal("400"));
        order.setRefundAmount(new BigDecimal("400"));
        order.setRefundReason("用户申请退款");
        order.setRefundType(RefundType.USER_REQUESTED);
        order.setPaymentMethod("alipay");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // mock 支付网关退款成功
        RefundResponse successResponse = RefundResponse.builder()
                .success(true)
                .refundTradeNo("REFUND_TXN_001")
                .refundAmount(new BigDecimal("400"))
                .message("退款成功")
                .build();
        when(paymentService.processRefund(any(RefundRequest.class))).thenReturn(successResponse);

        // 执行
        OrderDTO result = orderService.approveRefund(1L, "同意退款");

        // 验证
        assertNotNull(result);
        assertEquals(PaymentStatus.REFUNDED.name(), result.getPaymentStatus());
        assertEquals("REFUND_TXN_001", result.getRefundTransactionId());
        verify(paymentService, times(1)).processRefund(any(RefundRequest.class));
    }

    @Test
    void approveRefund_GatewayFailed() {
        // 准备 - 退款中的订单
        order.setStatus(OrderStatus.REFUND_PENDING.name());
        order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        order.setTotalAmount(new BigDecimal("400"));
        order.setRefundAmount(new BigDecimal("400"));
        order.setPaymentMethod("alipay");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // mock 支付网关退款失败
        RefundResponse failResponse = RefundResponse.builder()
                .success(false)
                .message("余额不足")
                .errorCode("INSUFFICIENT_BALANCE")
                .build();
        when(paymentService.processRefund(any(RefundRequest.class))).thenReturn(failResponse);

        // 执行和验证 - 应抛出异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.approveRefund(1L, "同意退款");
        });
        assertTrue(exception.getMessage().contains("退款失败") || exception.getMessage().contains("批准退款失败"));
    }

    @Test
    void approveRefund_NotRefundPending() {
        // 准备 - 非退款中状态
        order.setPaymentStatus(PaymentStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // 执行和验证
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            orderService.approveRefund(1L, "批准");
        });
        assertTrue(exception.getMessage().contains("退款中"));
    }

    // --- 管理员拒绝退款 rejectRefund ---

    @Test
    void rejectRefund_Success() {
        // 准备 - 退款中的订单
        mockSecurityContext();
        order.setStatus(OrderStatus.REFUND_PENDING.name());
        order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行
        OrderDTO result = orderService.rejectRefund(1L, "不符合退款条件");

        // 验证 - 恢复已支付状态
        assertNotNull(result);
        assertEquals(PaymentStatus.PAID.name(), result.getPaymentStatus());
        assertEquals(OrderStatus.PAID.name(), result.getStatus());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void rejectRefund_NotRefundPending() {
        // 准备 - 非退款中状态
        order.setPaymentStatus(PaymentStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // 执行和验证
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            orderService.rejectRefund(1L, "拒绝");
        });
        assertTrue(exception.getMessage().contains("退款中"));
    }

    // --- 管理员完成退款 completeRefund ---

    @Test
    void completeRefund_Success() {
        // 准备 - 退款中的订单
        mockSecurityContext();
        order.setStatus(OrderStatus.REFUND_PENDING.name());
        order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行
        OrderDTO result = orderService.completeRefund(1L, "TXN_REFUND_123");

        // 验证
        assertNotNull(result);
        assertEquals(PaymentStatus.REFUNDED.name(), result.getPaymentStatus());
        assertEquals(OrderStatus.REFUNDED.name(), result.getStatus());
        assertEquals("TXN_REFUND_123", result.getRefundTransactionId());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void completeRefund_NotRefundPending() {
        // 准备 - 非退款中状态
        order.setPaymentStatus(PaymentStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // 执行和验证
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            orderService.completeRefund(1L, "TXN_123");
        });
        assertTrue(exception.getMessage().contains("退款中"));
    }

    // --- 完整退款流程测试 ---

    @Test
    void refundFlow_UserRequest_AdminApprove() {
        // 准备
        mockSecurityContext();
        order.setStatus(OrderStatus.PAID.name());
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setTotalAmount(new BigDecimal("400"));
        order.setPaymentMethod("alipay");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // 步骤1：用户申请退款
        OrderDTO step1Result = orderService.requestUserRefund(1L, "行程有变");
        assertEquals(OrderStatus.REFUND_PENDING.name(), step1Result.getStatus());
        assertEquals(PaymentStatus.REFUND_PENDING.name(), step1Result.getPaymentStatus());

        // mock 支付网关退款成功
        RefundResponse successResponse = RefundResponse.builder()
                .success(true)
                .refundTradeNo("REFUND_TXN_FLOW")
                .refundAmount(new BigDecimal("400"))
                .message("退款成功")
                .build();
        when(paymentService.processRefund(any(RefundRequest.class))).thenReturn(successResponse);

        // 步骤2：管理员批准退款
        OrderDTO step2Result = orderService.approveRefund(1L, "确认退款");
        assertEquals(PaymentStatus.REFUNDED.name(), step2Result.getPaymentStatus());
        assertEquals(OrderStatus.REFUNDED.name(), step2Result.getStatus());
    }

    @Test
    void refundFlow_UserRequest_AdminReject() {
        // 准备
        mockSecurityContext();
        order.setStatus(OrderStatus.PAID.name());
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setTotalAmount(new BigDecimal("400"));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // 步骤1：用户申请退款
        OrderDTO step1Result = orderService.requestUserRefund(1L, "不想住了");
        assertEquals(OrderStatus.REFUND_PENDING.name(), step1Result.getStatus());

        // 步骤2：管理员拒绝退款
        OrderDTO step2Result = orderService.rejectRefund(1L, "超过退款期限");
        assertEquals(PaymentStatus.PAID.name(), step2Result.getPaymentStatus());
        assertEquals(OrderStatus.PAID.name(), step2Result.getStatus());
    }
}
