package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.dto.HomestaySearchRequest;
import com.homestay3.homestaybackend.dto.HomestaySearchResultDTO;
import com.homestay3.homestaybackend.dto.MapClusterDTO;

import java.util.List;

public interface HomestaySearchService {

    List<HomestayDTO> searchHomestays(HomestaySearchRequest request);

    List<HomestaySearchResultDTO> searchHomestayResults(HomestaySearchRequest request);

    List<MapClusterDTO> getMapClusters(HomestaySearchRequest request);

    List<HomestayDTO> getNearbyHomestays(HomestaySearchRequest request);

    List<HomestaySearchResultDTO> getNearbyHomestayResults(HomestaySearchRequest request);

    List<HomestayDTO> searchHomestaysNearLandmark(HomestaySearchRequest request);

    List<HomestaySearchResultDTO> searchHomestayResultsNearLandmark(HomestaySearchRequest request);

    List<HomestayDTO> searchHomestays(
            String keyword,
            String provinceCode,
            String cityCode,
            Integer minPrice,
            Integer maxPrice,
            Integer guests,
            String type);
}
