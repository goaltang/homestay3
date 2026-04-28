package com.homestay3.homestaybackend.service.search;

import com.homestay3.homestaybackend.model.search.HomestayDocument;

import java.util.List;

public interface HomestayIndexingService {

    /**
     * 全量重建 ES 索引
     * @return 实际写入 ES 的文档数量
     */
    int rebuildIndex();

    /**
     * 增量同步单个房源到 ES
     */
    void syncHomestay(Long homestayId);

    /**
     * 批量同步房源到 ES
     */
    void syncHomestays(List<Long> homestayIds);

    /**
     * 从 ES 删除房源
     */
    void deleteHomestay(Long homestayId);

    /**
     * 检查 ES 是否可用
     */
    boolean isElasticsearchAvailable();

    /**
     * 根据 ID 查询 ES 文档
     */
    HomestayDocument findById(Long homestayId);
}
