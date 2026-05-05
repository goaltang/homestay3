package com.homestay3.homestaybackend.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityTypeTest {

    @Test
    void canonicalTypeMapsLegacyMessageAlias() {
        assertEquals(EntityType.MESSAGE_THREAD, EntityType.MESSAGE.canonicalType());
    }

    @Test
    void canonicalTypeKeepsCurrentEntityTypesUnchanged() {
        assertEquals(EntityType.ORDER, EntityType.ORDER.canonicalType());
        assertEquals(EntityType.MESSAGE_THREAD, EntityType.MESSAGE_THREAD.canonicalType());
        assertEquals(EntityType.UNKNOWN, EntityType.UNKNOWN.canonicalType());
    }

    @Test
    void isLegacyAliasOnlyForMappedLegacyTypes() {
        assertTrue(EntityType.MESSAGE.isLegacyAlias());
        assertFalse(EntityType.MESSAGE_THREAD.isLegacyAlias());
        assertFalse(EntityType.UNKNOWN.isLegacyAlias());
    }
}
