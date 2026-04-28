package com.homestay3.homestaybackend.service.search;

import com.homestay3.homestaybackend.dto.HomestayDTO;

import java.util.List;

/**
 * 搜索个性化服务：根据用户画像对搜索结果进行排序提升
 */
public interface SearchPersonalizationService {

    /**
     * 对搜索结果进行个性化提升排序。
     * 仅在用户有画像数据且未指定显式排序时生效。
     *
     * @param results 原始搜索结果
     * @param userId  当前用户 ID，未登录传 null
     * @return 排序后的结果
     */
    List<HomestayDTO> boost(List<HomestayDTO> results, Long userId);
}
