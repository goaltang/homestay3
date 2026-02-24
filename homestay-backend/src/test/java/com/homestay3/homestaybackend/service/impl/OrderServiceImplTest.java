package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.OrderDTO;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.exception.AccessDeniedException;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.model.HomestayStatus;
import com.homestay3.homestaybackend.entity.Order;
import com.homestay3.homestaybackend.model.OrderStatus;
import com.homestay3.homestaybackend.model.PaymentStatus;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.repository.ReviewRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.BookingConflictService;
import com.homestay3.homestaybackend.service.EarningService;
import com.homestay3.homestaybackend.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
}
