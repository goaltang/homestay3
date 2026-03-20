package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.entity.SystemConfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SystemConfigService {

    /**
     * 获取所有配置
     */
    List<SystemConfig> getAllConfigs();

    /**
     * 根据key获取配置
     */
    Optional<SystemConfig> getConfigByKey(String key);

    /**
     * 根据key获取配置值
     */
    String getConfigValue(String key);

    /**
     * 根据key获取配置值，如果不存在返回默认值
     */
    String getConfigValue(String key, String defaultValue);

    /**
     * 根据分类获取配置
     */
    List<SystemConfig> getConfigsByCategory(String category);

    /**
     * 创建或更新配置
     */
    SystemConfig saveConfig(SystemConfig config);

    /**
     * 更新配置
     */
    SystemConfig updateConfig(String key, SystemConfig config);

    /**
     * 删除配置
     */
    void deleteConfig(Long id);

    /**
     * 批量获取配置值
     */
    Map<String, String> getConfigsByKeys(List<String> keys);

    /**
     * 初始化默认配置
     */
    void initDefaultConfigs();
}
