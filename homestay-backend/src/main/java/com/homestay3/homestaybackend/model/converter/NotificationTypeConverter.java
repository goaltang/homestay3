package com.homestay3.homestaybackend.model.converter;

import com.homestay3.homestaybackend.model.enums.NotificationType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class NotificationTypeConverter implements AttributeConverter<NotificationType, String> {

    @Override
    public String convertToDatabaseColumn(NotificationType attribute) {
        return attribute == null ? NotificationType.UNKNOWN.name() : attribute.name();
    }

    @Override
    public NotificationType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return NotificationType.UNKNOWN;
        }

        try {
            return NotificationType.valueOf(dbData.trim());
        } catch (IllegalArgumentException ignored) {
            return NotificationType.UNKNOWN;
        }
    }
}
