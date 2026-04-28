package com.homestay3.homestaybackend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay3.homestaybackend.dto.CouponDiscountResult;
import com.homestay3.homestaybackend.entity.CouponTemplate;
import com.homestay3.homestaybackend.entity.UserCoupon;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.repository.CouponTemplateRepository;
import com.homestay3.homestaybackend.repository.UserCouponRepository;
import com.homestay3.homestaybackend.utils.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CouponService 单元测试
 * 覆盖：越权防护、并发锁券、预算耗尽、支付后核销
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CouponServiceImplTest {

    @Mock
    private CouponTemplateRepository templateRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CouponServiceImpl couponService;

    private static final Long USER_A = 1L;
    private static final Long USER_B = 2L;
    private static final Long ORDER_1 = 100L;
    private static final Long HOMESTAY_1 = 10L;

    private CouponTemplate cashTemplate;
    private CouponTemplate discountTemplate;
    private CouponTemplate fullReductionTemplate;

    @BeforeEach
    void setUp() {
        cashTemplate = TestDataFactory.createCouponTemplate("CASH", 100, 1);
        discountTemplate = TestDataFactory.createCouponTemplate("DISCOUNT", 100, 1);
        fullReductionTemplate = TestDataFactory.createCouponTemplate("FULL_REDUCTION", 100, 1);
    }

    // ============================================================
    // 1. 优惠券越权防护测试
    // ============================================================

    @Nested
    @DisplayName("越权防护 - 用户只能使用自己的可用券")
    class AuthorizationTests {

        @Test
        @DisplayName("计算折扣时，LOCKED 状态的券（被他人锁定）返回零折扣")
        void calculateDiscount_LockedCoupon_ReturnsZero() {
            // 准备：用户B的券已被锁定
            UserCoupon userBCoupon = TestDataFactory.createUserCoupon(USER_B, cashTemplate, "LOCKED");
            userBCoupon.setLockedOrderId(ORDER_1);
            when(userCouponRepository.findById(userBCoupon.getId())).thenReturn(Optional.of(userBCoupon));

            // 执行：用户A尝试计算该券折扣
            CouponDiscountResult result = couponService.calculateCouponDiscount(
                    userBCoupon.getId(), HOMESTAY_1, new BigDecimal("200"));

            // 验证：非 AVAILABLE 状态应返回 0 折扣
            assertNotNull(result);
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getDiscountAmount()));
            assertEquals("PLATFORM", result.getSubsidyBearer());
        }

        @Test
        @DisplayName("计算折扣时，USED 状态的券返回零折扣")
        void calculateDiscount_UsedCoupon_ReturnsZero() {
            UserCoupon usedCoupon = TestDataFactory.createUserCoupon(USER_A, cashTemplate, "USED");
            when(userCouponRepository.findById(usedCoupon.getId())).thenReturn(Optional.of(usedCoupon));

            CouponDiscountResult result = couponService.calculateCouponDiscount(
                    usedCoupon.getId(), HOMESTAY_1, new BigDecimal("200"));

            assertEquals(0, BigDecimal.ZERO.compareTo(result.getDiscountAmount()));
        }

        @Test
        @DisplayName("获取可用优惠券只返回指定用户的 AVAILABLE 券")
        void getAvailableCoupons_OnlyReturnsAvailableForUser() {
            UserCoupon availableCoupon = TestDataFactory.createUserCoupon(USER_A, cashTemplate, "AVAILABLE");
            when(userCouponRepository.expireOutdatedCoupons(any(LocalDateTime.class))).thenReturn(0);
            when(userCouponRepository.findByUserIdAndStatusAndExpireAtAfterOrderByExpireAtAsc(
                    eq(USER_A), eq("AVAILABLE"), any(LocalDateTime.class)))
                    .thenReturn(Collections.singletonList(availableCoupon));

            List<UserCoupon> result = couponService.getAvailableCoupons(USER_A);

            assertEquals(1, result.size());
            assertEquals(USER_A, result.get(0).getUserId());
            assertEquals("AVAILABLE", result.get(0).getStatus());
            verify(userCouponRepository, never()).findByUserIdAndStatusAndExpireAtAfterOrderByExpireAtAsc(
                    eq(USER_B), any(), any());
        }

        @Test
        @DisplayName("批量计算折扣时，跨用户传入他人券 ID 返回零折扣")
        void calculateDiscount_Batch_CrossUser_ReturnsZero() {
            UserCoupon userBCoupon = TestDataFactory.createUserCoupon(USER_B, cashTemplate, "AVAILABLE");
            when(userCouponRepository.findByIdAndUserId(userBCoupon.getId(), USER_A))
                    .thenReturn(Optional.empty());

            CouponDiscountResult result = couponService.calculateCouponDiscount(
                    Collections.singletonList(userBCoupon.getId()), HOMESTAY_1, new BigDecimal("200"), USER_A);

            assertEquals(0, BigDecimal.ZERO.compareTo(result.getDiscountAmount()));
        }

        @Test
        @DisplayName("lockCoupons 原子锁定非 AVAILABLE 状态的券时抛异常")
        void lockCoupons_AlreadyLocked_ThrowsException() {
            when(userCouponRepository.lockCoupon(anyLong(), eq(USER_A), eq(ORDER_1), any(LocalDateTime.class)))
                    .thenReturn(0);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    couponService.lockCoupons(Collections.singletonList(1L), ORDER_1, USER_A));
            assertTrue(ex.getMessage().contains("锁定失败"));
        }

        @Test
        @DisplayName("lockCoupons 跨用户锁券时抛异常")
        void lockCoupons_CrossUser_ThrowsException() {
            when(userCouponRepository.lockCoupon(anyLong(), eq(USER_A), eq(ORDER_1), any(LocalDateTime.class)))
                    .thenReturn(0);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    couponService.lockCoupons(Collections.singletonList(1L), ORDER_1, USER_A));
            assertTrue(ex.getMessage().contains("锁定失败"));
        }
    }

    // ============================================================
    // 2. 并发锁券测试
    // ============================================================

    @Nested
    @DisplayName("并发锁券 - 竞争条件下的券状态安全")
    class ConcurrentLockTests {

        @Test
        @DisplayName("领取时 incrementIssuedCount 返回 0，抛出库存不足异常")
        void claimCoupon_IncrementReturnsZero_ThrowsStockExhausted() {
            // 准备：库存有限，已发 99/100
            CouponTemplate template = TestDataFactory.createCouponTemplate("CASH", 100, 1);
            template.setIssuedCount(99);
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userCouponRepository.countByUserIdAndTemplateId(USER_A, template.getId())).thenReturn(0L);
            when(templateRepository.incrementIssuedCount(template.getId())).thenReturn(0);

            // 执行与验证
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    couponService.claimCoupon(USER_A, template.getId()));
            assertTrue(ex.getMessage().contains("库存不足"));
        }

        @Test
        @DisplayName("领取时库存恰好耗尽，应拒绝")
        void claimCoupon_StockJustExhausted_ThrowsExhausted() {
            CouponTemplate template = TestDataFactory.createCouponTemplate("CASH", 100, 1);
            template.setIssuedCount(100);
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userCouponRepository.countByUserIdAndTemplateId(USER_A, template.getId())).thenReturn(0L);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    couponService.claimCoupon(USER_A, template.getId()));
            assertTrue(ex.getMessage().contains("已领完"));
        }

        @Test
        @DisplayName("锁券原子操作成功时返回 1")
        void lockCoupons_AtomicLockSuccess() {
            when(userCouponRepository.lockCoupon(eq(1L), eq(USER_A), eq(ORDER_1), any(LocalDateTime.class)))
                    .thenReturn(1);

            assertDoesNotThrow(() -> couponService.lockCoupons(Collections.singletonList(1L), ORDER_1, USER_A));
            verify(userCouponRepository).lockCoupon(eq(1L), eq(USER_A), eq(ORDER_1), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("无限库存券领取不受 issuedCount 限制")
        void claimCoupon_UnlimitedStock_NoStockCheck() {
            CouponTemplate template = TestDataFactory.createCouponTemplate("CASH", 0, 1);
            template.setIssuedCount(999);
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userCouponRepository.countByUserIdAndTemplateId(USER_A, template.getId())).thenReturn(0L);
            when(userCouponRepository.save(any(UserCoupon.class))).thenAnswer(inv -> inv.getArgument(0));

            UserCoupon result = couponService.claimCoupon(USER_A, template.getId());

            assertNotNull(result);
            verify(templateRepository, never()).incrementIssuedCount(anyLong());
        }
    }

    // ============================================================
    // 3. 预算耗尽测试
    // ============================================================

    @Nested
    @DisplayName("预算耗尽 - 库存与限领门槛")
    class BudgetExhaustionTests {

        @Test
        @DisplayName("有限库存恰好耗尽，领取应失败")
        void claimCoupon_LimitedStockExhausted_ThrowsExhausted() {
            CouponTemplate template = TestDataFactory.createCouponTemplate("CASH", 1, 1);
            template.setIssuedCount(1);
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userCouponRepository.countByUserIdAndTemplateId(USER_A, template.getId())).thenReturn(0L);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    couponService.claimCoupon(USER_A, template.getId()));
            assertTrue(ex.getMessage().contains("已领完"));
        }

        @Test
        @DisplayName("单用户限领 1 张，已领 1 张后再次领取应失败")
        void claimCoupon_PerUserLimitReached_ThrowsLimit() {
            CouponTemplate template = TestDataFactory.createCouponTemplate("CASH", 100, 1);
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userCouponRepository.countByUserIdAndTemplateId(USER_A, template.getId())).thenReturn(1L);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    couponService.claimCoupon(USER_A, template.getId()));
            assertTrue(ex.getMessage().contains("领取上限"));
        }

        @Test
        @DisplayName("单用户限领 2 张，已领 1 张后仍可领取")
        void claimCoupon_PerUserLimitNotReached_Success() {
            CouponTemplate template = TestDataFactory.createCouponTemplate("CASH", 100, 2);
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userCouponRepository.countByUserIdAndTemplateId(USER_A, template.getId())).thenReturn(1L);
            when(templateRepository.incrementIssuedCount(template.getId())).thenReturn(1);
            when(userCouponRepository.save(any(UserCoupon.class))).thenAnswer(inv -> inv.getArgument(0));

            UserCoupon result = couponService.claimCoupon(USER_A, template.getId());

            assertNotNull(result);
            assertEquals(USER_A, result.getUserId());
            verify(templateRepository).incrementIssuedCount(template.getId());
        }

        @Test
        @DisplayName("库存剩余 1，并发领取后 incrementIssuedCount 保护不超发")
        void claimCoupon_LastStock_IncrementProtects() {
            CouponTemplate template = TestDataFactory.createCouponTemplate("CASH", 100, 1);
            template.setIssuedCount(99);
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userCouponRepository.countByUserIdAndTemplateId(USER_A, template.getId())).thenReturn(0L);
            when(templateRepository.incrementIssuedCount(template.getId())).thenReturn(1);
            when(userCouponRepository.save(any(UserCoupon.class))).thenAnswer(inv -> inv.getArgument(0));

            UserCoupon result = couponService.claimCoupon(USER_A, template.getId());

            assertNotNull(result);
            verify(templateRepository).incrementIssuedCount(template.getId());
        }

        @Test
        @DisplayName("模板未激活时领取应失败")
        void claimCoupon_InactiveTemplate_ThrowsInactive() {
            CouponTemplate template = TestDataFactory.createCouponTemplate("CASH", 100, 1);
            template.setStatus("DRAFT");
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    couponService.claimCoupon(USER_A, template.getId()));
            assertTrue(ex.getMessage().contains("未激活"));
        }
    }

    // ============================================================
    // 4. 支付后核销测试
    // ============================================================

    @Nested
    @DisplayName("支付后核销 - useCoupons / releaseCoupons 状态转换")
    class PaymentWriteOffTests {

        @Test
        @DisplayName("支付成功后，批量核销原子操作被调用")
        void useCoupons_LockedCoupons_BecomeUsed() {
            when(userCouponRepository.useCouponsByOrderId(eq(ORDER_1), any(LocalDateTime.class)))
                    .thenReturn(2);

            couponService.useCoupons(ORDER_1);

            verify(userCouponRepository).useCouponsByOrderId(eq(ORDER_1), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("支付核销时无锁定券，不影响")
        void useCoupons_NoLockedCoupons_NoOp() {
            when(userCouponRepository.useCouponsByOrderId(eq(ORDER_1), any(LocalDateTime.class)))
                    .thenReturn(0);

            couponService.useCoupons(ORDER_1);

            verify(userCouponRepository).useCouponsByOrderId(eq(ORDER_1), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("取消订单后，批量释放原子操作被调用")
        void releaseCoupons_LockedCoupons_BecomeAvailable() {
            when(userCouponRepository.releaseCouponsByOrderId(ORDER_1)).thenReturn(1);

            couponService.releaseCoupons(ORDER_1);

            verify(userCouponRepository).releaseCouponsByOrderId(ORDER_1);
        }

        @Test
        @DisplayName("释放优惠券时无锁定券，不影响")
        void releaseCoupons_NoLockedCoupons_NoOp() {
            when(userCouponRepository.releaseCouponsByOrderId(ORDER_1)).thenReturn(0);

            couponService.releaseCoupons(ORDER_1);

            verify(userCouponRepository).releaseCouponsByOrderId(ORDER_1);
        }

        @Test
        @DisplayName("空列表锁券不执行任何操作")
        void lockCoupons_EmptyList_NoOp() {
            couponService.lockCoupons(Collections.emptyList(), ORDER_1, USER_A);
            verify(userCouponRepository, never()).lockCoupon(anyLong(), anyLong(), anyLong(), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("使用空订单 ID 核销不抛异常")
        void useCoupons_NullOrderId_DoesNotThrow() {
            when(userCouponRepository.useCouponsByOrderId(isNull(), any(LocalDateTime.class))).thenReturn(0);
            assertDoesNotThrow(() -> couponService.useCoupons(null));
        }
    }

    // ============================================================
    // 5. 折扣计算正确性测试（补充基础逻辑覆盖）
    // ============================================================

    @Nested
    @DisplayName("折扣计算 - 各类券型正确性")
    class DiscountCalculationTests {

        @Test
        @DisplayName("CASH 券计算正确")
        void calculateDiscount_CashCoupon_ReturnsFaceValue() {
            UserCoupon coupon = TestDataFactory.createUserCoupon(USER_A, cashTemplate, "AVAILABLE");
            when(userCouponRepository.findById(coupon.getId())).thenReturn(Optional.of(coupon));

            CouponDiscountResult result = couponService.calculateCouponDiscount(
                    coupon.getId(), HOMESTAY_1, new BigDecimal("200"));

            assertEquals(0, new BigDecimal("50").compareTo(result.getDiscountAmount()));
            assertEquals("PLATFORM", result.getSubsidyBearer());
        }

        @Test
        @DisplayName("DISCOUNT 券计算正确")
        void calculateDiscount_DiscountCoupon_ReturnsDiscountedAmount() {
            UserCoupon coupon = TestDataFactory.createUserCoupon(USER_A, discountTemplate, "AVAILABLE");
            when(userCouponRepository.findById(coupon.getId())).thenReturn(Optional.of(coupon));

            CouponDiscountResult result = couponService.calculateCouponDiscount(
                    coupon.getId(), HOMESTAY_1, new BigDecimal("200"));

            // 200 * (1 - 0.8) = 40
            assertEquals(0, new BigDecimal("40.00").compareTo(result.getDiscountAmount()));
        }

        @Test
        @DisplayName("FULL_REDUCTION 券满足门槛计算正确")
        void calculateDiscount_FullReductionCoupon_MeetsThreshold_ReturnsFaceValue() {
            UserCoupon coupon = TestDataFactory.createUserCoupon(USER_A, fullReductionTemplate, "AVAILABLE");
            when(userCouponRepository.findById(coupon.getId())).thenReturn(Optional.of(coupon));

            CouponDiscountResult result = couponService.calculateCouponDiscount(
                    coupon.getId(), HOMESTAY_1, new BigDecimal("200"));

            assertEquals(0, new BigDecimal("50").compareTo(result.getDiscountAmount()));
        }

        @Test
        @DisplayName("未满足门槛时返回零折扣")
        void calculateDiscount_BelowThreshold_ReturnsZero() {
            UserCoupon coupon = TestDataFactory.createUserCoupon(USER_A, fullReductionTemplate, "AVAILABLE");
            when(userCouponRepository.findById(coupon.getId())).thenReturn(Optional.of(coupon));

            CouponDiscountResult result = couponService.calculateCouponDiscount(
                    coupon.getId(), HOMESTAY_1, new BigDecimal("50"));

            assertEquals(0, BigDecimal.ZERO.compareTo(result.getDiscountAmount()));
        }

        @Test
        @DisplayName("折扣超过 maxDiscount 时被封顶")
        void calculateDiscount_ExceedsMaxDiscount_Capped() {
            CouponTemplate template = TestDataFactory.createCouponTemplate("DISCOUNT", 100, 1);
            template.setDiscountRate(new BigDecimal("0.50")); // 50% off
            template.setMaxDiscount(new BigDecimal("30"));    // cap at 30
            UserCoupon coupon = TestDataFactory.createUserCoupon(USER_A, template, "AVAILABLE");
            when(userCouponRepository.findById(coupon.getId())).thenReturn(Optional.of(coupon));

            CouponDiscountResult result = couponService.calculateCouponDiscount(
                    coupon.getId(), HOMESTAY_1, new BigDecimal("200"));

            assertEquals(0, new BigDecimal("30.00").compareTo(result.getDiscountAmount()));
        }

        @Test
        @DisplayName("折扣不超过原始金额")
        void calculateDiscount_NotExceedOriginalAmount() {
            CouponTemplate template = TestDataFactory.createCouponTemplate("CASH", 100, 1);
            template.setFaceValue(new BigDecimal("500"));
            template.setMaxDiscount(null); // 取消封顶，只受原始金额限制
            UserCoupon coupon = TestDataFactory.createUserCoupon(USER_A, template, "AVAILABLE");
            when(userCouponRepository.findById(coupon.getId())).thenReturn(Optional.of(coupon));

            CouponDiscountResult result = couponService.calculateCouponDiscount(
                    coupon.getId(), HOMESTAY_1, new BigDecimal("200"));

            assertEquals(0, new BigDecimal("200.00").compareTo(result.getDiscountAmount()));
        }
    }
}
