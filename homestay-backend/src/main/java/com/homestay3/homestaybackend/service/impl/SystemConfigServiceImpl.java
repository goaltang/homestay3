package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.entity.SystemConfig;
import com.homestay3.homestaybackend.repository.SystemConfigRepository;
import com.homestay3.homestaybackend.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

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
        return systemConfigRepository.save(config);
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

        return systemConfigRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteConfig(Long id) {
        systemConfigRepository.deleteById(id);
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
