package com.donornexa.admin.repository;

import com.donornexa.admin.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SettingRepository extends JpaRepository<Setting, Long> {

    Optional<Setting> findBySettingKey(String settingKey);

    default String getSettingValue(String key, String defaultValue) {
        return findBySettingKey(key)
                .map(Setting::getSettingValue)
                .orElse(defaultValue);
    }

    default int getIntSetting(String key, int defaultValue) {
        try {
            return Integer.parseInt(getSettingValue(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
