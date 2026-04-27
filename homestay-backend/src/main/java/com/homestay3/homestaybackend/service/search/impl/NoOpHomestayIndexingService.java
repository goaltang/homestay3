package com.homestay3.homestaybackend.service.search.impl;

import com.homestay3.homestaybackend.model.search.HomestayDocument;
import com.homestay3.homestaybackend.service.search.HomestayIndexingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ES 禁用时的空实现，保证应用不依赖 Docker 也能正常启动和运行。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpHomestayIndexingService implements HomestayIndexingService {

    @Override
    public void rebuildIndex() {
        log.info("ES is disabled, skip index rebuild");
    }

    @Override
    public void syncHomestay(Long homestayId) {
        log.debug("ES is disabled, skip syncing homestay {}", homestayId);
    }

    @Override
    public void syncHomestays(List<Long> homestayIds) {
        log.debug("ES is disabled, skip batch syncing {} homestays", homestayIds != null ? homestayIds.size() : 0);
    }

    @Override
    public void deleteHomestay(Long homestayId) {
        log.debug("ES is disabled, skip deleting homestay {}", homestayId);
    }

    @Override
    public boolean isElasticsearchAvailable() {
        return false;
    }

    @Override
    public HomestayDocument findById(Long homestayId) {
        return null;
    }
}
