package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.model.search.HomestayDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HomestayDocumentRepository extends ElasticsearchRepository<HomestayDocument, Long> {

    /**
     * 根据状态查询
     */
    List<HomestayDocument> findByStatus(String status);

    /**
     * 根据城市查询
     */
    List<HomestayDocument> findByCityCodeAndStatus(String cityCode, String status);

    /**
     * 根据类型查询
     */
    List<HomestayDocument> findByTypeAndStatus(String type, String status);

    /**
     * 全文本搜索（标题、描述、地址）
     * 使用 Function Score 进行多维度排序
     */
    @Query("""
        {
          "function_score": {
            "query": {
              "bool": {
                "must": [
                  { "match": { "status": "ACTIVE" } },
                  {
                    "multi_match": {
                      "query": "?0",
                      "fields": ["title^3", "description^2", "addressDetail^2", "amenities"],
                      "type": "best_fields",
                      "fuzziness": "AUTO"
                    }
                  }
                ],
                "filter": [
                  { "range": { "price": { "gte": ?1, "lte": ?2 } } },
                  { "range": { "maxGuests": { "gte": ?3 } } }
                ]
              }
            },
            "functions": [
              { "field_value_factor": { "field": "rating", "factor": 0.20, "modifier": "log1p", "missing": 0 } },
              { "field_value_factor": { "field": "bookingCount", "factor": 0.075, "modifier": "log1p", "missing": 0 } },
              { "field_value_factor": { "field": "favoriteCount", "factor": 0.075, "modifier": "log1p", "missing": 0 } },
              { "gauss": { "createdAt": { "origin": "now", "scale": "30d", "decay": 0.5 } } }
            ],
            "score_mode": "sum",
            "boost_mode": "multiply"
          }
        }
        """)
    SearchHits<HomestayDocument> searchByKeywordWithScoring(
            String keyword,
            Double minPrice,
            Double maxPrice,
            Integer minGuests);

    /**
     * 根据 amenities 过滤搜索
     */
    @Query("""
        {
          "bool": {
            "must": [
              { "match": { "status": "ACTIVE" } },
              { "terms": { "amenities": ?0 } }
            ]
          }
        }
        """)
    List<HomestayDocument> findByAmenities(List<String> amenities);

    /**
     * 距离排序搜索
     */
    @Query("""
        {
          "geo_distance": {
            "distance": "?2km",
            "location": {
              "lat": ?0,
              "lon": ?1
            }
          }
        }
        """)
    List<HomestayDocument> findNearby(double lat, double lon, double distanceKm);
}
