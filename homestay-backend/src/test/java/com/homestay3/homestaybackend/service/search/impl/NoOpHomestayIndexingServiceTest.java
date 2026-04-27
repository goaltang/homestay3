package com.homestay3.homestaybackend.service.search.impl;

import com.homestay3.homestaybackend.model.search.HomestayDocument;
import com.homestay3.homestaybackend.service.search.HomestayIndexingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NoOpHomestayIndexingServiceTest {

    private HomestayIndexingService indexingService;

    @BeforeEach
    void setUp() {
        indexingService = new NoOpHomestayIndexingService();
    }

    @Test
    void isElasticsearchAvailable_shouldReturnFalse() {
        assertFalse(indexingService.isElasticsearchAvailable());
    }

    @Test
    void rebuildIndex_shouldNotThrow() {
        assertDoesNotThrow(() -> indexingService.rebuildIndex());
    }

    @Test
    void syncHomestay_shouldNotThrow() {
        assertDoesNotThrow(() -> indexingService.syncHomestay(1L));
    }

    @Test
    void syncHomestays_shouldNotThrow() {
        assertDoesNotThrow(() -> indexingService.syncHomestays(List.of(1L, 2L, 3L)));
    }

    @Test
    void deleteHomestay_shouldNotThrow() {
        assertDoesNotThrow(() -> indexingService.deleteHomestay(1L));
    }

    @Test
    void findById_shouldReturnNull() {
        assertNull(indexingService.findById(1L));
    }
}
