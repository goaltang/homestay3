package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.entity.SystemConfig;
import com.homestay3.homestaybackend.repository.SystemConfigRepository;
import com.homestay3.homestaybackend.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final Logger log = LoggerFactory.getLogger(SystemConfigServiceImpl.class);

    private final SystemConfigRepository systemConfigRepository;
    private final CacheManager cacheManager;

    @Override
    public List<SystemConfig> getAllConfigs() {
        return systemConfigRepository.findAll();
    }

    @Override
    public Optional<SystemConfig> getConfigByKey(String key) {
        return systemConfigRepository.findByConfigKey(key);
    }

    @Override
    public String getConfigValue(String key) {
        return systemConfigRepository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .orElse(null);
    }

    @Override
    public String getConfigValue(String key, String defaultValue) {
        return systemConfigRepository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }

    @Override
    public List<SystemConfig> getConfigsByCategory(String category) {
        return systemConfigRepository.findByCategory(category);
    }

    @Override
    @Transactional
    public SystemConfig saveConfig(SystemConfig config) {
        SystemConfig saved = systemConfigRepository.save(config);
        clearConfigCache(saved.getConfigKey());
        return saved;
    }

    @Override
    @Transactional
    public SystemConfig updateConfig(String key, SystemConfig config) {
        SystemConfig existing = systemConfigRepository.findByConfigKey(key)
                .orElseThrow(() -> new RuntimeException("配置不存在: " + key));

        existing.setConfigValue(config.getConfigValue());
        existing.setConfigName(config.getConfigName());
        existing.setDescription(config.getDescription());
        existing.setCategory(config.getCategory());
        existing.setUpdatedBy(config.getUpdatedBy());

        SystemConfig saved = systemConfigRepository.save(existing);

        // 清除缓存，确保下次读取获取最新值
        clearConfigCache(key);

        return saved;
    }

    /**
     * 清除配置相关的缓存
     */
    private void clearConfigCache(String key) {
        try {
            // 清除所有缓存（如果有多个缓存管理器会有问题，这里简单处理）
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.evict(key);
                    cache.clear();
                }
            });
        } catch (Exception e) {
            // 缓存清除失败不影响业务
        }
    }

    @Override
    @Transactional
    public void deleteConfig(Long id) {
        // 先获取configKey，用于清除缓存
        systemConfigRepository.findById(id).ifPresent(config -> {
            String key = config.getConfigKey();
            systemConfigRepository.deleteById(id);
            clearConfigCache(key);
        });
    }

    @Override
    public Map<String, String> getConfigsByKeys(List<String> keys) {
        Map<String, String> result = new HashMap<>();
        for (String key : keys) {
            getConfigByKey(key).ifPresent(config -> result.put(key, config.getConfigValue()));
        }
        return result;
    }

    @Override
    @Transactional
    public void initDefaultConfigs() {
        // 平台配置
        initConfigIfNotExists("platform.name", "乡村民宿平台", "平台名称", "platform");
        initConfigIfNotExists("platform.logo", "", "平台Logo URL", "platform");
        initConfigIfNotExists("platform.hotline", "400-888-8888", "平台客服热线", "platform");

        // 预订政策配置
        initConfigIfNotExists("policy.max_advance_days", "90", "最大提前预订天数", "policy");
        initConfigIfNotExists("policy.min_advance_hours", "2", "最少提前预订小时数", "policy");
        initConfigIfNotExists("policy.cancel_free_hours", "24", "免费取消时限(小时)", "policy");
        initConfigIfNotExists("policy.max_stay_days", "30", "单次最大入住天数", "policy");

        // 费用配置
        initConfigIfNotExists("fee.platform_rate", "0.10", "平台服务费比例(0.1=10%)", "fee");
        initConfigIfNotExists("fee.min_withdraw_amount", "100", "最小提现金额(元)", "fee");

        // 房源配置
        initConfigIfNotExists("homestay.min_rating_visible", "3.0", "房源可见最低评分", "homestay");
        initConfigIfNotExists("homestay.max_images", "20", "房源最大图片数量", "homestay");
        initConfigIfNotExists("homestay.auto_confirm_hours", "24", "自动确认订单时限(小时)", "homestay");

        // 价格计算配置（统一使用下划线格式，便于管理员端修改）
        // 迁移旧配置（连字符）到新配置（下划线）
        migrateConfigIfNeeded("pricing.cleaning-fee-rate", "pricing.cleaning_fee", "清洁费固定比例");
        migrateConfigIfNeeded("pricing.service-fee-rate", "pricing.service_fee", "服务费费率");
        migrateConfigIfNeeded("pricing.weekend-rate", "pricing.weekend_rate", "周末价格系数");
        migrateConfigIfNeeded("pricing.holiday-rate", "pricing.holiday_rate", "节假日价格系数");
    }

    /**
     * 迁移旧配置到新配置（兼容处理）
     * 如果旧配置存在，将其值迁移到新配置
     */
    private void migrateConfigIfNeeded(String oldKey, String newKey, String newName) {
        Optional<SystemConfig> oldConfig = systemConfigRepository.findByConfigKey(oldKey);
        Optional<SystemConfig> newConfig = systemConfigRepository.findByConfigKey(newKey);

        if (oldConfig.isPresent()) {
            // 旧配置存在
            if (newConfig.isPresent()) {
                // 新配置也存在，检查是否需要从旧配置迁移值
                if ("0.1".equals(newConfig.get().getConfigValue()) && !newConfig.get().getConfigValue().equals(oldConfig.get().getConfigValue())) {
                    // 新配置是默认值，且旧配置不是默认值，迁移旧配置的值
                    newConfig.get().setConfigValue(oldConfig.get().getConfigValue());
                    systemConfigRepository.save(newConfig.get());
                    log.info("配置迁移完成: {} -> {}，值: {} -> {}", oldKey, newKey, "0.1", oldConfig.get().getConfigValue());
                }
            } else {
                // 新配置不存在，创建并使用旧配置的值
                SystemConfig config = SystemConfig.builder()
                        .configKey(newKey)
                        .configValue(oldConfig.get().getConfigValue())
                        .configName(newName)
                        .category(oldConfig.get().getCategory())
                        .updatedBy("system")
                        .build();
                systemConfigRepository.save(config);
                log.info("配置迁移完成: {} -> {}，值: {}", oldKey, newKey, oldConfig.get().getConfigValue());
            }
        } else if (!newConfig.isPresent()) {
            // 旧配置不存在，新配置也不存在，初始化默认配置
            initConfigIfNotExists(newKey, "0.1", newName, "fee");
        }
    }

    private void initConfigIfNotExists(String key, String value, String name, String category) {
        if (!systemConfigRepository.existsByConfigKey(key)) {
            SystemConfig config = SystemConfig.builder()
                    .configKey(key)
                    .configValue(value)
                    .configName(name)
                    .category(category)
                    .updatedBy("system")
                    .build();
            systemConfigRepository.save(config);
        }
    }
}
