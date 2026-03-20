package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.entity.HomestayAuditLog;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.model.HomestayStatus;
import com.homestay3.homestaybackend.repository.HomestayAuditLogRepository;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.AmenityService;
import com.homestay3.homestaybackend.service.HomestayFeatureAnalysisService;
import com.homestay3.homestaybackend.util.ImageUrlUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * HomestayService 单元测试
 * 测试房源核心业务逻辑
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HomestayServiceImplTest {

    @Mock
    private HomestayRepository homestayRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HomestayAuditLogRepository homestayAuditLogRepository;

    @Mock
    private AmenityService amenityService;

    @Mock
    private HomestayFeatureAnalysisService homestayFeatureAnalysisService;

    @Mock
    private ImageUrlUtil imageUrlUtil;

    @InjectMocks
    private HomestayServiceImpl homestayService;

    private User adminUser;
    private User hostUser;
    private Homestay activeHomestay;

    @BeforeEach
    void setUp() {
        // 设置管理员用户
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setRole("ROLE_ADMIN");

        // 设置房东用户
        hostUser = new User();
        hostUser.setId(2L);
        hostUser.setUsername("host");
        hostUser.setRole("ROLE_HOST");

        // 设置已上架的房源
        activeHomestay = new Homestay();
        activeHomestay.setId(1L);
        activeHomestay.setTitle("测试民宿");
        activeHomestay.setPrice(new BigDecimal("200"));
        activeHomestay.setStatus(HomestayStatus.ACTIVE);
        activeHomestay.setOwner(hostUser);
        activeHomestay.setCreatedAt(LocalDateTime.now());
        activeHomestay.setUpdatedAt(LocalDateTime.now());
    }

    private void mockSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(adminUser.getUsername());

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername(adminUser.getUsername())).thenReturn(Optional.of(adminUser));
    }

    // ============================================================
    // 强制下架功能测试
    // ============================================================

    @Test
    void forceDelistHomestay_Success() {
        // 准备
        mockSecurityContext();
        String reason = "发布虚假信息";
        String notes = "多次被用户举报";
        String violationType = "FALSE_INFO";

        when(homestayRepository.findById(1L)).thenReturn(Optional.of(activeHomestay));
        when(homestayRepository.save(any(Homestay.class))).thenAnswer(inv -> inv.getArgument(0));
        when(homestayAuditLogRepository.save(any(HomestayAuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行
        homestayService.forceDelistHomestay(1L, reason, notes, violationType);

        // 验证
        ArgumentCaptor<Homestay> homestayCaptor = ArgumentCaptor.forClass(Homestay.class);
        verify(homestayRepository).save(homestayCaptor.capture());

        Homestay savedHomestay = homestayCaptor.getValue();
        assertEquals(HomestayStatus.INACTIVE, savedHomestay.getStatus());
        assertNotNull(savedHomestay.getUpdatedAt());
    }

    @Test
    void forceDelistHomestay_Success_RecordsAuditLog() {
        // 准备
        mockSecurityContext();
        String reason = "存在安全隐患";
        String notes = "经核实确实存在问题";
        String violationType = "SAFETY_ISSUE";

        when(homestayRepository.findById(1L)).thenReturn(Optional.of(activeHomestay));
        when(homestayRepository.save(any(Homestay.class))).thenAnswer(inv -> inv.getArgument(0));
        when(homestayAuditLogRepository.save(any(HomestayAuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行
        homestayService.forceDelistHomestay(1L, reason, notes, violationType);

        // 验证审核日志
        ArgumentCaptor<HomestayAuditLog> auditLogCaptor = ArgumentCaptor.forClass(HomestayAuditLog.class);
        verify(homestayAuditLogRepository).save(auditLogCaptor.capture());

        HomestayAuditLog savedLog = auditLogCaptor.getValue();
        assertEquals(activeHomestay, savedLog.getHomestay());
        assertEquals(adminUser, savedLog.getReviewer());
        assertEquals(HomestayStatus.ACTIVE, savedLog.getOldStatus());
        assertEquals(HomestayStatus.INACTIVE, savedLog.getNewStatus());
        assertEquals(HomestayAuditLog.AuditActionType.DEACTIVATE, savedLog.getActionType());
        assertEquals(reason, savedLog.getReviewReason());
        // 当 notes 不为 null 时，reviewNotes 使用 notes 的值
        assertEquals(notes, savedLog.getReviewNotes());
    }

    @Test
    void forceDelistHomestay_HomestayNotFound() {
        // 准备
        when(homestayRepository.findById(999L)).thenReturn(Optional.empty());

        // 执行和验证
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            homestayService.forceDelistHomestay(999L, "违规", null, "OTHER");
        });
        assertTrue(exception.getMessage().contains("房源不存在"));
    }

    @Test
    void forceDelistHomestay_AdminNotFound_SkipsAuditLog() {
        // 准备 - 管理员用户不存在
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("nonexistent");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(homestayRepository.findById(1L)).thenReturn(Optional.of(activeHomestay));
        when(homestayRepository.save(any(Homestay.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // 执行 - 应该不抛出异常，只是跳过审核日志
        assertDoesNotThrow(() -> homestayService.forceDelistHomestay(1L, "违规", null, "OTHER"));

        // 验证房源状态已更新但没有审核日志
        ArgumentCaptor<Homestay> homestayCaptor = ArgumentCaptor.forClass(Homestay.class);
        verify(homestayRepository).save(homestayCaptor.capture());
        assertEquals(HomestayStatus.INACTIVE, homestayCaptor.getValue().getStatus());
        verify(homestayAuditLogRepository, never()).save(any());
    }

    @Test
    void forceDelistHomestay_AlreadyInactive() {
        // 准备 - 房源已经是下架状态
        mockSecurityContext();
        activeHomestay.setStatus(HomestayStatus.INACTIVE);

        when(homestayRepository.findById(1L)).thenReturn(Optional.of(activeHomestay));
        when(homestayRepository.save(any(Homestay.class))).thenAnswer(inv -> inv.getArgument(0));
        when(homestayAuditLogRepository.save(any(HomestayAuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行
        homestayService.forceDelistHomestay(1L, "再次下架", null, "OTHER");

        // 验证仍然保存（状态不变）
        ArgumentCaptor<Homestay> homestayCaptor = ArgumentCaptor.forClass(Homestay.class);
        verify(homestayRepository).save(homestayCaptor.capture());
        assertEquals(HomestayStatus.INACTIVE, homestayCaptor.getValue().getStatus());

        // 验证审核日志仍然被记录
        verify(homestayAuditLogRepository, times(1)).save(any());
    }

    @Test
    void forceDelistHomestay_WithNullNotes() {
        // 准备 - notes 为空
        mockSecurityContext();

        when(homestayRepository.findById(1L)).thenReturn(Optional.of(activeHomestay));
        when(homestayRepository.save(any(Homestay.class))).thenAnswer(inv -> inv.getArgument(0));
        when(homestayAuditLogRepository.save(any(HomestayAuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行
        homestayService.forceDelistHomestay(1L, "违规内容", null, "PROHIBITED_CONTENT");

        // 验证
        ArgumentCaptor<HomestayAuditLog> auditLogCaptor = ArgumentCaptor.forClass(HomestayAuditLog.class);
        verify(homestayAuditLogRepository).save(auditLogCaptor.capture());

        HomestayAuditLog savedLog = auditLogCaptor.getValue();
        assertTrue(savedLog.getReviewNotes().contains("PROHIBITED_CONTENT"));
    }
}
