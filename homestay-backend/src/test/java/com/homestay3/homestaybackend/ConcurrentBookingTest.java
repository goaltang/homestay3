package com.homestay3.homestaybackend;

import com.homestay3.homestaybackend.dto.OrderDTO;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.model.HomestayStatus;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 并发预订测试
 * 验证系统是否能够正确处理多个用户同时预订同一房源相同日期的情况
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
public class ConcurrentBookingTest {

    @Autowired
    private OrderService orderService;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private HomestayRepository homestayRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    private Homestay testHomestay;
    private List<User> testUsers;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    
    @BeforeEach
    public void setUp() {
        // 创建测试房源
        testHomestay = new Homestay();
        testHomestay.setTitle("测试民宿");
        testHomestay.setDescription("用于并发测试的民宿");
        testHomestay.setPrice(new BigDecimal("200.00"));
        testHomestay.setMinNights(1);
        testHomestay.setStatus(HomestayStatus.ACTIVE);
        testHomestay = homestayRepository.save(testHomestay);
        
        // 创建测试用户
        testUsers = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            User user = new User();
            user.setUsername("testuser" + i);
            user.setPassword("password" + i);
            user.setEmail("test" + i + "@example.com");
            testUsers.add(userRepository.save(user));
        }
        
        // 设置测试日期
        checkInDate = LocalDate.now().plusDays(1);
        checkOutDate = LocalDate.now().plusDays(3);
    }
    
    @Test
    public void testConcurrentBookingPrevention() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Exception> exceptions = new CopyOnWriteArrayList<>();
        
        // 创建并发预订任务
        for (int i = 0; i < numberOfThreads; i++) {
            final int userIndex = i;
            executorService.submit(() -> {
                try {
                    // 等待所有线程准备就绪
                    startLatch.await();
                    
                    // 创建订单DTO
                    OrderDTO orderDTO = OrderDTO.builder()
                            .homestayId(testHomestay.getId())
                            .guestPhone("13800138000")
                            .checkInDate(checkInDate)
                            .checkOutDate(checkOutDate)
                            .guestCount(2)
                            .remark("并发测试订单 " + userIndex)
                            .build();
                    
                    // 模拟当前用户（这里需要在实际环境中设置SecurityContext）
                    // 尝试创建订单
                    OrderDTO result = orderService.createOrder(orderDTO);
                    
                    if (result != null) {
                        successCount.incrementAndGet();
                        System.out.println("用户 " + userIndex + " 预订成功: " + result.getOrderNumber());
                    }
                    
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    exceptions.add(e);
                    System.out.println("用户 " + userIndex + " 预订失败: " + e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });
        }
        
        // 启动所有线程
        startLatch.countDown();
        
        // 等待所有线程完成
        boolean finished = finishLatch.await(30, TimeUnit.SECONDS);
        assertTrue(finished, "测试在30秒内应该完成");
        
        executorService.shutdown();
        
        // 验证结果
        System.out.println("成功预订数量: " + successCount.get());
        System.out.println("失败预订数量: " + failureCount.get());
        System.out.println("异常列表: ");
        exceptions.forEach(e -> System.out.println("  - " + e.getMessage()));
        
        // 断言：只能有一个成功的预订
        assertEquals(1, successCount.get(), "只能有一个用户成功预订");
        assertEquals(numberOfThreads - 1, failureCount.get(), "其他用户应该预订失败");
        
        // 验证数据库中只有一条有效订单
        long validOrderCount = orderRepository.countByHomestayIdAndCreatedAtAfter(
                testHomestay.getId(), 
                java.time.LocalDateTime.now().minusMinutes(5)
        );
        assertEquals(1, validOrderCount, "数据库中只能有一条有效订单");
    }
    
    @Test
    public void testSequentialBookingForDifferentDates() {
        // 测试不同日期的预订应该都能成功
        LocalDate date1Start = LocalDate.now().plusDays(1);
        LocalDate date1End = LocalDate.now().plusDays(3);
        
        LocalDate date2Start = LocalDate.now().plusDays(5);
        LocalDate date2End = LocalDate.now().plusDays(7);
        
        try {
            OrderDTO order1 = OrderDTO.builder()
                    .homestayId(testHomestay.getId())
                    .guestPhone("13800138001")
                    .checkInDate(date1Start)
                    .checkOutDate(date1End)
                    .guestCount(2)
                    .remark("第一个订单")
                    .build();
            
            OrderDTO order2 = OrderDTO.builder()
                    .homestayId(testHomestay.getId())
                    .guestPhone("13800138002")
                    .checkInDate(date2Start)
                    .checkOutDate(date2End)
                    .guestCount(2)
                    .remark("第二个订单")
                    .build();
            
            OrderDTO result1 = orderService.createOrder(order1);
            OrderDTO result2 = orderService.createOrder(order2);
            
            assertNotNull(result1, "第一个订单应该创建成功");
            assertNotNull(result2, "第二个订单应该创建成功");
            assertNotEquals(result1.getOrderNumber(), result2.getOrderNumber(), "订单号应该不同");
            
        } catch (Exception e) {
            fail("不同日期的预订应该都能成功: " + e.getMessage());
        }
    }
    
    @Test
    public void testOverlappingDatesPrevention() {
        // 测试重叠日期的预订应该被阻止
        LocalDate baseStart = LocalDate.now().plusDays(1);
        LocalDate baseEnd = LocalDate.now().plusDays(5);
        
        try {
            // 第一个订单
            OrderDTO order1 = OrderDTO.builder()
                    .homestayId(testHomestay.getId())
                    .guestPhone("13800138001")
                    .checkInDate(baseStart)
                    .checkOutDate(baseEnd)
                    .guestCount(2)
                    .remark("基础订单")
                    .build();
            
            OrderDTO result1 = orderService.createOrder(order1);
            assertNotNull(result1, "第一个订单应该创建成功");
            
            // 尝试创建重叠的订单
            OrderDTO overlappingOrder = OrderDTO.builder()
                    .homestayId(testHomestay.getId())
                    .guestPhone("13800138002")
                    .checkInDate(baseStart.plusDays(1)) // 重叠日期
                    .checkOutDate(baseEnd.plusDays(1))
                    .guestCount(2)
                    .remark("重叠订单")
                    .build();
            
            // 这里应该抛出异常
            assertThrows(IllegalArgumentException.class, () -> {
                orderService.createOrder(overlappingOrder);
            }, "重叠日期的订单应该被拒绝");
            
        } catch (Exception e) {
            if (!(e instanceof IllegalArgumentException)) {
                fail("应该抛出IllegalArgumentException: " + e.getMessage());
            }
        }
    }
} 