package com.homestay3.homestaybackend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 测试基类
 * 提供所有测试类的公共设置和清理逻辑
 */
public abstract class BaseTest {

    private AutoCloseable mocks;

    @BeforeEach
    void baseSetUp() {
        // 初始化 Mockito 注解
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void baseTearDown() throws Exception {
        // 关闭 Mockito
        if (mocks != null) {
            mocks.close();
        }
        
        // 清理 SecurityContext
        SecurityContextHolder.clearContext();
    }

    /**
     * 创建测试用例的通用描述
     */
    protected String testDescription(String description) {
        return "[TEST] " + description;
    }
}
