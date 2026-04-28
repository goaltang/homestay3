package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.AuthRequest;
import com.homestay3.homestaybackend.dto.AuthResponse;
import com.homestay3.homestaybackend.dto.RegisterRequest;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.model.UserRole;
import com.homestay3.homestaybackend.model.VerificationStatus;
import com.homestay3.homestaybackend.dto.UserDTO;
import com.homestay3.homestaybackend.mapper.UserMapper;
import com.homestay3.homestaybackend.repository.AdminRepository;
import com.homestay3.homestaybackend.repository.CouponTemplateRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.security.JwtTokenProvider;
import com.homestay3.homestaybackend.service.CouponService;
import com.homestay3.homestaybackend.service.EmailService;
import com.homestay3.homestaybackend.service.NotificationService;
import com.homestay3.homestaybackend.service.ReferralService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuthService 单元测试
 * 测试认证核心业务逻辑：注册、登录、密码重置等
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @Mock
    private AuthenticationManager authenticationManager;
    
    @Mock
    private EmailService emailService;
    
    @Mock
    private NotificationService notificationService;
    
    @Mock
    private AdminRepository adminRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CouponTemplateRepository couponTemplateRepository;

    @Mock
    private CouponService couponService;

    @Mock
    private ReferralService referralService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private AuthRequest authRequest;

    @BeforeEach
    void setUp() {
        // mock userMapper
        when(userMapper.toDTO(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            UserDTO dto = new UserDTO();
            dto.setId(u.getId());
            dto.setUsername(u.getUsername());
            dto.setEmail(u.getEmail());
            dto.setPhone(u.getPhone());
            dto.setRole(u.getRole());
            return dto;
        });

        // 设置测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setPhone("13800138000");
        testUser.setRole(UserRole.ROLE_USER.name());
        testUser.setEnabled(true);
        testUser.setVerificationStatus(VerificationStatus.UNVERIFIED);
        
        // 设置注册请求
        registerRequest = RegisterRequest.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("password123")
                .phone("13900139000")
                .role("USER")
                .build();
        
        // 设置登录请求
        authRequest = AuthRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        // 通用 mock
        when(jwtTokenProvider.generateToken(any(String.class), any(Long.class), any(String.class))).thenReturn("test-jwt-token");
    }

    @Test
    void register_Success() {
        // 准备
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtTokenProvider.generateToken(any(Authentication.class))).thenReturn("test-jwt-token");
        
        // 执行
        AuthResponse response = authService.register(registerRequest);
        
        // 验证
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("test-jwt-token", response.getToken());
        assertNotNull(response.getUser());
        assertEquals("newuser", response.getUser().getUsername());
        
        verify(userRepository, times(1)).saveAndFlush(any(User.class));
        verify(notificationService, times(1)).createNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void register_UsernameAlreadyExists() {
        // 准备
        when(userRepository.existsByUsername("newuser")).thenReturn(true);
        
        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(registerRequest);
        });
        
        assertEquals("用户名已存在", exception.getMessage());
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void register_EmailAlreadyExists() {
        // 准备
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(true);
        
        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(registerRequest);
        });
        
        assertEquals("邮箱已存在", exception.getMessage());
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void login_Success() {
        // 准备
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        // 执行
        AuthResponse response = authService.login(authRequest);
        
        // 验证
        assertNotNull(response);
        assertEquals("test-jwt-token", response.getToken());
        assertNotNull(response.getUser());
        assertEquals("testuser", response.getUser().getUsername());
        
        verify(userRepository, times(1)).save(any(User.class)); // 更新最后登录时间
    }

    @Test
    void login_UserNotFound() {
        // 准备
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(authRequest);
        });

        assertTrue(exception.getMessage().contains("用户不存在"));
    }

    @Test
    void login_BadCredentials() {
        // 准备
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));
        
        // 执行和验证
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(authRequest);
        });
        
        assertEquals("密码错误，请重新输入", exception.getMessage());
    }

    @Test
    void getUserInfo_Success() {
        // 准备
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        // 执行
        AuthResponse response = authService.getUserInfo("testuser");
        
        // 验证
        assertNotNull(response);
        assertNotNull(response.getUser());
        assertEquals("testuser", response.getUser().getUsername());
        assertEquals("test@example.com", response.getUser().getEmail());
    }

    @Test
    void getUserInfo_UserNotFound() {
        // 准备
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());
        
        // 执行和验证
        assertThrows(RuntimeException.class, () -> {
            authService.getUserInfo("nonexistent");
        });
    }

    @Test
    void isUsernameExists_True() {
        // 准备
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);
        
        // 执行
        boolean exists = authService.isUsernameExists("existinguser");
        
        // 验证
        assertTrue(exists);
    }

    @Test
    void isUsernameExists_False() {
        // 准备
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        
        // 执行
        boolean exists = authService.isUsernameExists("newuser");
        
        // 验证
        assertFalse(exists);
    }

    @Test
    void isEmailExists_True() {
        // 准备
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);
        
        // 执行
        boolean exists = authService.isEmailExists("existing@example.com");
        
        // 验证
        assertTrue(exists);
    }

    @Test
    void isEmailExists_False() {
        // 准备
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        
        // 执行
        boolean exists = authService.isEmailExists("new@example.com");
        
        // 验证
        assertFalse(exists);
    }

    @Test
    void isUsernameExists_EmptyUsername() {
        // 执行
        boolean exists = authService.isUsernameExists("");
        
        // 验证
        assertFalse(exists);
        verify(userRepository, never()).existsByUsername(any());
    }

    @Test
    void isUsernameExists_NullUsername() {
        // 执行
        boolean exists = authService.isUsernameExists(null);
        
        // 验证
        assertFalse(exists);
        verify(userRepository, never()).existsByUsername(any());
    }

    @Test
    void updateUserAvatar_Success() {
        // 准备
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // 执行
        assertDoesNotThrow(() -> {
            authService.updateUserAvatar("testuser", "http://example.com/avatar.jpg");
        });
        
        // 验证
        verify(userRepository, times(1)).save(any(User.class));
    }
}
