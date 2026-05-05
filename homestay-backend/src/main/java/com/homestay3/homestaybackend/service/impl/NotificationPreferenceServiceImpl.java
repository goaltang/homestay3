package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.entity.NotificationPreference;
import com.homestay3.homestaybackend.model.enums.NotificationDomain;
import com.homestay3.homestaybackend.repository.NotificationPreferenceRepository;
import com.homestay3.homestaybackend.service.NotificationPreferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Service
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    @Autowired
    public NotificationPreferenceServiceImpl(NotificationPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<NotificationDomain, Boolean> getPreferences(Long userId) {
        Map<NotificationDomain, Boolean> result = new EnumMap<>(NotificationDomain.class);
        for (NotificationDomain domain : NotificationDomain.values()) {
            result.put(domain, isEnabled(userId, domain));
        }
        return result;
    }

    @Override
    @Transactional
    public void updatePreference(Long userId, NotificationDomain domain, boolean enabled) {
        NotificationPreference preference = preferenceRepository
                .findByUserIdAndDomain(userId, domain.name())
                .orElseGet(() -> NotificationPreference.builder()
                        .userId(userId)
                        .domain(domain.name())
                        .enabled(enabled)
                        .build());
        preference.setEnabled(enabled);
        preferenceRepository.save(preference);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEnabled(Long userId, NotificationDomain domain) {
        return preferenceRepository
                .findByUserIdAndDomain(userId, domain.name())
                .map(NotificationPreference::isEnabled)
                .orElse(true);
    }
}
