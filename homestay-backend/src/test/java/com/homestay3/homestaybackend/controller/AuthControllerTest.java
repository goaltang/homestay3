package com.homestay3.homestaybackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay3.homestaybackend.dto.AuthRequest;
import com.homestay3.homestaybackend.dto.RegisterRequest;
import com.homestay3.homestaybackend.security.JwtTokenProvider;
import com.homestay3.homestaybackend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 测试
 * 测试认证相关接口
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void loginEndpoint_Exists() throws Exception {
        // 准备请求
        AuthRequest request = AuthRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        // 执行和验证 - 端点已存在，mock service 返回 null 时返回 200
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void registerEndpoint_Exists() throws Exception {
        // 准备请求
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("password123")
                .build();

        // 执行和验证 - 端点已存在，mock service 返回 null 时返回 200
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void getUserInfo_WithoutAuth_Returns401() throws Exception {
        // 执行和验证 - 未认证访问 /api/auth/current 返回 401
        mockMvc.perform(get("/api/auth/current"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void checkUsernameEndpoint() throws Exception {
        mockMvc.perform(get("/api/auth/check-username")
                .param("username", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
    }

    @Test
    void checkEmailEndpoint() throws Exception {
        mockMvc.perform(get("/api/auth/check-email")
                .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
    }
}
