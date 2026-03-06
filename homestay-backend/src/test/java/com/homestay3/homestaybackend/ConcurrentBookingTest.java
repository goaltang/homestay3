package com.homestay3.homestaybackend;

import com.homestay3.homestaybackend.dto.OrderDTO;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.model.HomestayStatus;
import com.homestay3.homestaybackend.repository.*;
import com.homestay3.homestaybackend.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 并发预订测试 - 验证 Redis 细粒度并行锁
 */
@SpringBootTest
public class ConcurrentBookingTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private HomestayRepository homestayRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EarningRepository earningRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @Autowired
    private RefundRecordRepository refundRecordRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserFavoriteRepository userFavoriteRepository;

    @Autowired
    private HomestayImageRepository homestayImageRepository;

    @Autowired
    private HomestayAuditLogRepository homestayAuditLogRepository;

    private Homestay testHomestay;
    private User testUser;
    private User hostUser;

    @BeforeEach
    public void setUp() {
        // 彻底级联清理数据库 (子级 -> 父级)
        // notificationRepository.deleteAll();
        // earningRepository.deleteAll();
        // reviewRepository.deleteAll();
        // paymentRecordRepository.deleteAll();
        // refundRecordRepository.deleteAll();
        // userFavoriteRepository.deleteAll();
        // homestayImageRepository.deleteAll();
        // homestayAuditLogRepository.deleteAll();
        //
        // orderRepository.deleteAll();
        // homestayRepository.deleteAll();
        // userRepository.deleteAll();

        // 创建测试用户 (预订者)
        testUser = new User();
        testUser.setUsername("testuser_concurrent");
        testUser.setPassword("password");
        testUser.setEmail("concurrent@example.com");
        testUser.setRole("ROLE_USER");
        testUser = userRepository.save(testUser);

        // 创建房东用户
        hostUser = new User();
        hostUser.setUsername("hostuser_concurrent");
        hostUser.setPassword("password");
        hostUser.setEmail("host@example.com");
        hostUser.setRole("ROLE_USER");
        hostUser = userRepository.save(hostUser);

        // 创建测试房源 (包含所有必填字段)
        testHomestay = new Homestay();
        testHomestay.setTitle("并发测试房源");
        testHomestay.setType("整套公寓");
        testHomestay.setPrice(new BigDecimal("100.00"));
        testHomestay.setMaxGuests(4);
        testHomestay.setMinNights(1);
        testHomestay.setProvinceCode("110000");
        testHomestay.setCityCode("110100");
        testHomestay.setAddressDetail("测试街道 123 号");
        testHomestay.setDescription("测试 Redis 细粒度锁");
        testHomestay.setStatus(HomestayStatus.ACTIVE);
        testHomestay.setOwner(hostUser);
        testHomestay.setAutoConfirm(true);
        testHomestay = homestayRepository.save(testHomestay);

        // 设置全局 SecurityContext
        setMockAuthentication();
    }

    private SecurityContext setMockAuthentication() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                testUser.getUsername(), null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        return context;
    }

    @Test
    public void testConcurrentBookingSameDate() throws InterruptedException {
        int numberOfThreads = 8;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        LocalDate checkIn = LocalDate.now().plusWeeks(3);
        LocalDate checkOut = checkIn.plusDays(2);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    setMockAuthentication();
                    startLatch.await();

                    OrderDTO orderDTO = OrderDTO.builder()
                            .homestayId(testHomestay.getId())
                            .guestPhone("13800000000")
                            .checkInDate(checkIn)
                            .checkOutDate(checkOut)
                            .guestCount(1)
                            .build();

                    orderService.createOrder(orderDTO);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("相同日期预订失败: " + e.getMessage());
                    e.printStackTrace();
                    failureCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await(40, TimeUnit.SECONDS);
        executorService.shutdown();

        assertEquals(1, successCount.get(), "相同日期的并发预订只能成功一个");
        assertEquals(numberOfThreads - 1, failureCount.get(), "其他预订应该报错失败");
    }

    @Test
    public void testConcurrentBookingDifferentDates() throws InterruptedException {
        int numberOfThreads = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<LocalDate[]> dateRanges = new ArrayList<>();
        for (int i = 0; i < numberOfThreads; i++) {
            LocalDate start = LocalDate.now().plusMonths(3).plusDays(i * 5);
            dateRanges.add(new LocalDate[] { start, start.plusDays(2) });
        }

        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    setMockAuthentication();
                    startLatch.await();

                    LocalDate[] range = dateRanges.get(index);
                    OrderDTO orderDTO = OrderDTO.builder()
                            .homestayId(testHomestay.getId())
                            .guestPhone("1390000000" + index)
                            .checkInDate(range[0])
                            .checkOutDate(range[1])
                            .guestCount(1)
                            .build();

                    orderService.createOrder(orderDTO);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("不同日期预订意外失败: " + e.getMessage());
                    e.printStackTrace();
                    failureCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await(40, TimeUnit.SECONDS);
        executorService.shutdown();

        assertEquals(numberOfThreads, successCount.get(), "不同日期的预订应该并行成功");
        assertEquals(0, failureCount.get(), "不应该有锁竞争产生的失败");
    }
}