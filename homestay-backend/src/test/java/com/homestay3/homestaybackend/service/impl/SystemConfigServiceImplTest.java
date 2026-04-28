package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.entity.SystemConfig;
import com.homestay3.homestaybackend.repository.SystemConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SystemConfigService 单元测试
 * 测试系统配置核心业务逻辑
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SystemConfigServiceImplTest {

    @Mock
    private SystemConfigRepository systemConfigRepository;

    @InjectMocks
    private SystemConfigServiceImpl systemConfigService;

    private SystemConfig platformConfig;
    private SystemConfig policyConfig;

    @BeforeEach
    void setUp() {
        platformConfig = SystemConfig.builder()
                .id(1L)
                .configKey("platform.name")
                .configValue("乡村民宿平台")
                .configName("平台名称")
                .category("platform")
                .updatedBy("admin")
                .build();

        policyConfig = SystemConfig.builder()
                .id(2L)
                .configKey("policy.max_advance_days")
                .configValue("90")
                .configName("最大提前预订天数")
                .category("policy")
                .updatedBy("admin")
                .build();
    }

    @Test
    void testGetAllConfigs() {
        when(systemConfigRepository.findAll()).thenReturn(Arrays.asList(platformConfig, policyConfig));

        List<SystemConfig> configs = systemConfigService.getAllConfigs();

        assertEquals(2, configs.size());
        assertEquals("platform.name", configs.get(0).getConfigKey());
        assertEquals("policy.max_advance_days", configs.get(1).getConfigKey());
        verify(systemConfigRepository, times(1)).findAll();
    }

    @Test
    void testGetConfigByKey_Found() {
        when(systemConfigRepository.findByConfigKey("platform.name")).thenReturn(Optional.of(platformConfig));

        Optional<SystemConfig> result = systemConfigService.getConfigByKey("platform.name");

        assertTrue(result.isPresent());
        assertEquals("platform.name", result.get().getConfigKey());
        assertEquals("乡村民宿平台", result.get().getConfigValue());
    }

    @Test
    void testGetConfigByKey_NotFound() {
        when(systemConfigRepository.findByConfigKey("not.exist")).thenReturn(Optional.empty());

        Optional<SystemConfig> result = systemConfigService.getConfigByKey("not.exist");

        assertFalse(result.isPresent());
    }

    @Test
    void testGetConfigValue_Found() {
        when(systemConfigRepository.findByConfigKey("platform.name")).thenReturn(Optional.of(platformConfig));

        String value = systemConfigService.getConfigValue("platform.name");

        assertEquals("乡村民宿平台", value);
    }

    @Test
    void testGetConfigValue_NotFound() {
        when(systemConfigRepository.findByConfigKey("not.exist")).thenReturn(Optional.empty());

        String value = systemConfigService.getConfigValue("not.exist");

        assertNull(value);
    }

    @Test
    void testGetConfigValue_WithDefault() {
        when(systemConfigRepository.findByConfigKey("not.exist")).thenReturn(Optional.empty());

        String value = systemConfigService.getConfigValue("not.exist", "default_value");

        assertEquals("default_value", value);
    }

    @Test
    void testGetConfigsByCategory() {
        when(systemConfigRepository.findByCategory("platform")).thenReturn(Arrays.asList(platformConfig));

        List<SystemConfig> configs = systemConfigService.getConfigsByCategory("platform");

        assertEquals(1, configs.size());
        assertEquals("platform", configs.get(0).getCategory());
    }

    @Test
    void testSaveConfig() {
        SystemConfig newConfig = SystemConfig.builder()
                .configKey("test.key")
                .configValue("test_value")
                .configName("测试配置")
                .category("other")
                .build();

        when(systemConfigRepository.save(any(SystemConfig.class))).thenReturn(newConfig);

        SystemConfig saved = systemConfigService.saveConfig(newConfig);

        assertNotNull(saved);
        assertEquals("test.key", saved.getConfigKey());
        verify(systemConfigRepository, times(1)).save(newConfig);
    }

    @Test
    void testUpdateConfig_Success() {
        when(systemConfigRepository.findByConfigKey("platform.name")).thenReturn(Optional.of(platformConfig));
        when(systemConfigRepository.save(any(SystemConfig.class))).thenReturn(platformConfig);

        SystemConfig updateData = SystemConfig.builder()
                .configValue("新平台名称")
                .configName("新平台名称")
                .category("platform")
                .description("更新描述")
                .updatedBy("admin")
                .build();

        SystemConfig updated = systemConfigService.updateConfig("platform.name", updateData);

        assertNotNull(updated);
        verify(systemConfigRepository, times(1)).findByConfigKey("platform.name");
        verify(systemConfigRepository, times(1)).save(any(SystemConfig.class));
    }

    @Test
    void testUpdateConfig_NotFound() {
        when(systemConfigRepository.findByConfigKey("not.exist")).thenReturn(Optional.empty());

        SystemConfig updateData = SystemConfig.builder()
                .configValue("value")
                .updatedBy("admin")
                .build();

        assertThrows(RuntimeException.class, () -> systemConfigService.updateConfig("not.exist", updateData));
    }

    @Test
    void testDeleteConfig() {
        when(systemConfigRepository.findById(1L)).thenReturn(Optional.of(platformConfig));
        doNothing().when(systemConfigRepository).deleteById(1L);

        systemConfigService.deleteConfig(1L);

        verify(systemConfigRepository, times(1)).deleteById(1L);
    }

    @Test
    void testGetConfigsByKeys() {
        when(systemConfigRepository.findByConfigKey("platform.name")).thenReturn(Optional.of(platformConfig));
        when(systemConfigRepository.findByConfigKey("policy.max_advance_days")).thenReturn(Optional.of(policyConfig));

        Map<String, String> result = systemConfigService.getConfigsByKeys(Arrays.asList("platform.name", "policy.max_advance_days"));

        assertEquals(2, result.size());
        assertEquals("乡村民宿平台", result.get("platform.name"));
        assertEquals("90", result.get("policy.max_advance_days"));
    }

    @Test
    void testGetConfigsByKeys_PartialNotFound() {
        when(systemConfigRepository.findByConfigKey("platform.name")).thenReturn(Optional.of(platformConfig));
        when(systemConfigRepository.findByConfigKey("not.exist")).thenReturn(Optional.empty());

        Map<String, String> result = systemConfigService.getConfigsByKeys(Arrays.asList("platform.name", "not.exist"));

        assertEquals(1, result.size());
        assertEquals("乡村民宿平台", result.get("platform.name"));
        assertNull(result.get("not.exist"));
    }

    @Test
    void testInitDefaultConfigs() {
        when(systemConfigRepository.existsByConfigKey("platform.name")).thenReturn(false);
        when(systemConfigRepository.existsByConfigKey("platform.hotline")).thenReturn(false);
        when(systemConfigRepository.save(any(SystemConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        systemConfigService.initDefaultConfigs();

        verify(systemConfigRepository, atLeastOnce()).existsByConfigKey(any());
        verify(systemConfigRepository, atLeastOnce()).save(any(SystemConfig.class));
    }

    @Test
    void testInitDefaultConfigs_SkipExisting() {
        // 所有配置都已存在，跳过保存
        when(systemConfigRepository.existsByConfigKey(any())).thenReturn(true);
        when(systemConfigRepository.findByConfigKey(any())).thenReturn(Optional.of(platformConfig));

        systemConfigService.initDefaultConfigs();

        // 所有配置都已存在，所以不会调用 save
        verify(systemConfigRepository, atLeastOnce()).existsByConfigKey(any());
        verify(systemConfigRepository, never()).save(any(SystemConfig.class));
    }
}
