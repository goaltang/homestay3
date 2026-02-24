package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.OrderDTO;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.entity.Order;
import com.homestay3.homestaybackend.model.OrderStatus;
import com.homestay3.homestaybackend.model.PaymentStatus;
import com.homestay3.homestaybackend.entity.PaymentRecord;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.repository.PaymentRecordRepository;
import com.homestay3.homestaybackend.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * PaymentService 单元测试
 * 测试支付核心业务逻辑
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private PaymentRecordRepository paymentRecordRepository;
    
    @Mock
    private OrderService orderService;
    
    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User currentUser;
    private User hostUser;
    private Order order;
    private Homestay homestay;
    private PaymentRecord paymentRecord;

    @BeforeEach
    void setUp() {
        // 设置当前用户（房客）
        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("guestuser");
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
        homestay.setOwner(hostUser);
        
        // 设置订单
        order = new Order();
        order.setId(1L);
        order.setOrderNumber("ORD202401010001");
        order.setHomestay(homestay);
        order.setGuest(currentUser);
        order.setGuestPhone("13800138000");
        order.setTotalAmount(new BigDecimal("400"));
        order.setStatus(OrderStatus.CONFIRMED.name());
        order.setPaymentStatus(PaymentStatus.UNPAID);
        
        // 设置支付记录
        paymentRecord = new PaymentRecord();
        paymentRecord.setId(1L);
        paymentRecord.setOrderId(1L);
        paymentRecord.setOutTradeNo("HST202401011200001001");
        paymentRecord.setPaymentMethod("ALIPAY");
        paymentRecord.setAmount(new BigDecimal("400"));
        paymentRecord.setStatus("PENDING");
    }

    @Test
    void checkPaymentStatus_PaidViaRecord() {
        // 准备 - 支付记录已经是成功状态
        paymentRecord.setStatus("SUCCESS");
        when(paymentRecordRepository.findTopByOrderIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(paymentRecord));
        
        // 执行
        boolean isPaid = paymentService.checkPaymentStatus(1L);
        
        // 验证
        assertTrue(isPaid);
    }

    @Test
    void checkPaymentStatus_Unpaid() {
        // 准备 - 支付记录存在但未支付
        when(paymentRecordRepository.findTopByOrderIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(paymentRecord));
        
        // 执行
        boolean isPaid = paymentService.checkPaymentStatus(1L);
        
        // 验证
        assertFalse(isPaid);
    }

    @Test
    void checkPaymentStatus_NoRecord() {
        // 准备 - 没有支付记录
        when(paymentRecordRepository.findTopByOrderIdOrderByCreatedAtDesc(999L))
                .thenReturn(Optional.empty());
        
        // 执行
        boolean isPaid = paymentService.checkPaymentStatus(999L);
        
        // 验证
        assertFalse(isPaid);
    }

    @Test
    void checkPaymentStatus_OrderNotFound() {
        // 准备 - 订单不存在，但 paymentRecordRepository 返回空
        when(paymentRecordRepository.findTopByOrderIdOrderByCreatedAtDesc(999L))
                .thenReturn(Optional.empty());
        
        // 执行
        boolean isPaid = paymentService.checkPaymentStatus(999L);
        
        // 验证
        assertFalse(isPaid);
    }

    @Test
    void mockSuccessPayment_Success() {
        // 准备
        order.setGuest(currentUser);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderService.updateOrderStatus(anyLong(), anyString())).thenReturn(new OrderDTO());
        
        // 执行
        OrderDTO result = paymentService.mockSuccessPayment(1L);
        
        // 验证
        assertNotNull(result);
        verify(orderService, times(1)).updateOrderStatus(1L, OrderStatus.PAID.name());
    }

    @Test
    void mockSuccessPayment_AlreadyPaid() {
        // 准备 - 订单已支付
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setStatus(OrderStatus.PAID.name());
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        
        // 执行和验证 - 应该抛出异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            paymentService.mockSuccessPayment(1L);
        });
        
        assertTrue(exception.getMessage().contains("已支付"));
    }

    @Test
    void mockSuccessPayment_OrderNotFound() {
        // 准备
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());
        
        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            paymentService.mockSuccessPayment(999L);
        });
        
        assertTrue(exception.getMessage().contains("不存在"));
    }

    @Test
    void mockSuccessPayment_InvalidStatus() {
        // 准备 - 订单已完成，不能支付
        order.setStatus(OrderStatus.COMPLETED.name());
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        
        // 执行和验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            paymentService.mockSuccessPayment(1L);
        });
        
        assertTrue(exception.getMessage().contains("状态不正确"));
    }

    @Test
    void mockSuccessPayment_CancelledOrder() {
        // 准备 - 订单已取消
        order.setStatus(OrderStatus.CANCELLED.name());
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        
        // 执行和验证
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            paymentService.mockSuccessPayment(1L);
        });
        
        assertTrue(exception.getMessage().contains("状态不正确"));
    }
}
