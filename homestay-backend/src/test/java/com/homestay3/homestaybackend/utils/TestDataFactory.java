package com.homestay3.homestaybackend.utils;

import com.homestay3.homestaybackend.dto.OrderDTO;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.model.HomestayStatus;
import com.homestay3.homestaybackend.entity.Order;
import com.homestay3.homestaybackend.model.OrderStatus;
import com.homestay3.homestaybackend.model.PaymentStatus;
import com.homestay3.homestaybackend.model.UserRole;
import com.homestay3.homestaybackend.model.VerificationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 测试数据工厂
 * 提供创建测试数据的便捷方法
 */
public class TestDataFactory {

    private static long userIdCounter = 1L;
    private static long orderIdCounter = 1L;
    private static long homestayIdCounter = 1L;
    private static long couponTemplateIdCounter = 1L;
    private static long userCouponIdCounter = 1L;
    private static int orderNumberCounter = 1;

    /**
     * 创建测试用户
     */
    public static User createUser(String username, String role) {
        User user = new User();
        user.setId(userIdCounter++);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("encodedPassword");
        user.setPhone("138" + String.format("%08d", userIdCounter));
        user.setRole(role != null ? role : UserRole.ROLE_USER.name());
        user.setEnabled(true);
        user.setVerificationStatus(VerificationStatus.UNVERIFIED);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    /**
     * 创建测试房客用户
     */
    public static User createGuestUser() {
        return createUser("guest" + userIdCounter, UserRole.ROLE_USER.name());
    }

    /**
     * 创建测试房东用户
     */
    public static User createHostUser() {
        return createUser("host" + userIdCounter, UserRole.ROLE_HOST.name());
    }

    /**
     * 创建测试管理员用户
     */
    public static User createAdminUser() {
        return createUser("admin" + userIdCounter, UserRole.ROLE_ADMIN.name());
    }

    /**
     * 创建测试房源
     */
    public static Homestay createHomestay(User owner, BigDecimal price) {
        Homestay homestay = new Homestay();
        homestay.setId(homestayIdCounter++);
        homestay.setTitle("测试民宿 " + homestay.getId());
        homestay.setDescription("这是一个测试房源");
        homestay.setPrice(price != null ? price : new BigDecimal("200"));
        homestay.setStatus(HomestayStatus.ACTIVE);
        homestay.setMinNights(1);
        homestay.setOwner(owner);
        homestay.setAutoConfirm(false);
        return homestay;
    }

    /**
     * 创建测试订单
     */
    public static Order createOrder(User guest, Homestay homestay, OrderStatus status, PaymentStatus paymentStatus) {
        Order order = new Order();
        order.setId(orderIdCounter++);
        order.setOrderNumber(generateOrderNumber());
        order.setHomestay(homestay);
        order.setGuest(guest);
        order.setGuestPhone("13800138000");
        order.setCheckInDate(LocalDate.now().plusDays(1));
        order.setCheckOutDate(LocalDate.now().plusDays(3));
        order.setNights(2);
        order.setGuestCount(2);
        order.setPrice(homestay.getPrice());
        order.setTotalAmount(homestay.getPrice().multiply(BigDecimal.valueOf(2)));
        order.setStatus(status != null ? status.name() : OrderStatus.PENDING.name());
        order.setPaymentStatus(paymentStatus != null ? paymentStatus : PaymentStatus.UNPAID);
        return order;
    }

    /**
     * 创建待支付订单
     */
    public static Order createPendingOrder(User guest, Homestay homestay) {
        return createOrder(guest, homestay, OrderStatus.CONFIRMED, PaymentStatus.UNPAID);
    }

    /**
     * 创建已支付订单
     */
    public static Order createPaidOrder(User guest, Homestay homestay) {
        return createOrder(guest, homestay, OrderStatus.PAID, PaymentStatus.PAID);
    }

    /**
     * 创建订单DTO
     */
    public static OrderDTO createOrderDTO(Long homestayId, LocalDate checkIn, LocalDate checkOut) {
        return OrderDTO.builder()
                .homestayId(homestayId)
                .checkInDate(checkIn != null ? checkIn : LocalDate.now().plusDays(1))
                .checkOutDate(checkOut != null ? checkOut : LocalDate.now().plusDays(3))
                .guestPhone("13800138000")
                .guestCount(2)
                .build();
    }

    /**
     * 创建优惠券模板
     */
    public static com.homestay3.homestaybackend.entity.CouponTemplate createCouponTemplate(
            String type, Integer totalStock, Integer perUserLimit) {
        com.homestay3.homestaybackend.entity.CouponTemplate template =
                com.homestay3.homestaybackend.entity.CouponTemplate.builder()
                        .id(couponTemplateIdCounter++)
                        .name("测试优惠券" + couponTemplateIdCounter)
                        .couponType(type != null ? type : "CASH")
                        .faceValue(new BigDecimal("50"))
                        .discountRate(new BigDecimal("0.80"))
                        .thresholdAmount(new BigDecimal("100"))
                        .maxDiscount(new BigDecimal("100"))
                        .totalStock(totalStock != null ? totalStock : 100)
                        .issuedCount(0)
                        .perUserLimit(perUserLimit != null ? perUserLimit : 1)
                        .validType("AFTER_CLAIM_DAYS")
                        .validDays(7)
                        .validStartAt(LocalDateTime.now().minusDays(1))
                        .validEndAt(LocalDateTime.now().plusDays(30))
                        .scopeType("ALL")
                        .subsidyBearer("PLATFORM")
                        .status("ACTIVE")
                        .build();
        return template;
    }

    /**
     * 创建用户优惠券
     */
    public static com.homestay3.homestaybackend.entity.UserCoupon createUserCoupon(
            Long userId,
            com.homestay3.homestaybackend.entity.CouponTemplate template,
            String status) {
        com.homestay3.homestaybackend.entity.UserCoupon userCoupon =
                com.homestay3.homestaybackend.entity.UserCoupon.builder()
                        .id(userCouponIdCounter++)
                        .userId(userId != null ? userId : 1L)
                        .template(template)
                        .couponCode("CP" + userCouponIdCounter)
                        .status(status != null ? status : "AVAILABLE")
                        .expireAt(LocalDateTime.now().plusDays(7))
                        .build();
        return userCoupon;
    }

    /**
     * 生成订单号
     */
    private static String generateOrderNumber() {
        return String.format("ORD%s%04d", 
                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")),
                orderNumberCounter++);
    }
}
