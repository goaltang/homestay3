package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.model.enums.NotificationDomain;

import java.util.Map;

public interface NotificationPreferenceService {

    /**
     * 获取用户全部通知偏好（按业务域）
     *
     * @param userId 用户ID
     * @return 业务域 -> 是否开启
     */
    Map<NotificationDomain, Boolean> getPreferences(Long userId);

    /**
     * 更新指定业务域的通知偏好
     *
     * @param userId  用户ID
     * @param domain  通知业务域
     * @param enabled 是否开启
     */
    void updatePreference(Long userId, NotificationDomain domain, boolean enabled);

    /**
     * 检查用户是否开启了指定业务域的通知
     *
     * @param userId 用户ID
     * @param domain 通知业务域
     * @return 是否开启（默认 true）
     */
    boolean isEnabled(Long userId, NotificationDomain domain);
}
