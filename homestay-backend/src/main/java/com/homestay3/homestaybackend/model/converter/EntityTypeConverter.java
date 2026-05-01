package com.homestay3.homestaybackend.model.converter;

import com.homestay3.homestaybackend.model.enums.EntityType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EntityTypeConverter implements AttributeConverter<EntityType, String> {

    @Override
    public String convertToDatabaseColumn(EntityType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public EntityType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }

        try {
            return EntityType.valueOf(dbData.trim());
        } catch (IllegalArgumentException ignored) {
            return EntityType.UNKNOWN;
        }
    }
}
