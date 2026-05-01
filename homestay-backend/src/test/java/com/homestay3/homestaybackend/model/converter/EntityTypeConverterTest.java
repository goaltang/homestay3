package com.homestay3.homestaybackend.model.converter;

import com.homestay3.homestaybackend.model.enums.EntityType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EntityTypeConverterTest {

    private final EntityTypeConverter converter = new EntityTypeConverter();

    @Test
    void convertsKnownEntityType() {
        assertEquals(EntityType.ORDER, converter.convertToEntityAttribute("ORDER"));
    }

    @Test
    void convertsUnknownEntityTypeToUnknown() {
        assertEquals(EntityType.UNKNOWN, converter.convertToEntityAttribute("LEGACY_MESSAGE"));
    }

    @Test
    void keepsBlankEntityTypeAsNull() {
        assertNull(converter.convertToEntityAttribute(" "));
    }
}
