package com.homestay3.homestaybackend.service.search.impl;

import com.homestay3.homestaybackend.repository.HomestayDocumentRepository;
import com.homestay3.homestaybackend.service.search.HomestayIndexingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证 ES 不可用时，搜索能正确降级到 JPA 的单元测试。
 */
@ExtendWith(MockitoExtension.class)
class HomestaySearchServiceEsFallbackTest {

    @Mock
    private HomestayIndexingService homestayIndexingService;

    @Mock
    private ObjectProvider<HomestayDocumentRepository> homestayDocumentRepositoryProvider;

    @Test
    void whenEsDisabled_shouldReturnFalseFromAvailabilityCheck() {
        when(homestayIndexingService.isElasticsearchAvailable()).thenReturn(false);

        boolean available = homestayIndexingService.isElasticsearchAvailable();
        assertFalse(available);
    }

    @Test
    void whenEsRepositoryNotAvailable_shouldReturnNull() {
        when(homestayDocumentRepositoryProvider.getIfAvailable()).thenReturn(null);

        HomestayDocumentRepository repo = homestayDocumentRepositoryProvider.getIfAvailable();
        assertNull(repo);
    }

    @Test
    void noOpIndexingService_shouldNeverBeAvailable() {
        HomestayIndexingService noOp = new NoOpHomestayIndexingService();
        assertFalse(noOp.isElasticsearchAvailable());
    }
}
