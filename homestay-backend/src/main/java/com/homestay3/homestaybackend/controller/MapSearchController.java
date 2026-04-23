package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.PoiSuggestionDTO;
import com.homestay3.homestaybackend.service.GeocodingService;
import com.homestay3.homestaybackend.service.PoiSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapSearchController {

    private final PoiSearchService poiSearchService;
    private final GeocodingService geocodingService;

    @GetMapping("/poi-suggestions")
    public ResponseEntity<List<PoiSuggestionDTO>> getPoiSuggestions(
            @RequestParam String keyword,
            @RequestParam(required = false) String city,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        return ResponseEntity.ok(poiSearchService.searchPoiSuggestions(keyword, city, limit));
    }

    @GetMapping("/geocode")
    public ResponseEntity<?> geocode(
            @RequestParam String address,
            @RequestParam(required = false) String provinceCode,
            @RequestParam(required = false) String cityCode,
            @RequestParam(required = false) String districtCode) {
        Optional<GeocodingService.Coordinates> result = geocodingService.geocode(provinceCode, cityCode, districtCode, address);
        return result.map(c -> ResponseEntity.ok(Map.of(
                "latitude", c.latitude(),
                "longitude", c.longitude()
        ))).orElse(ResponseEntity.noContent().build());
    }
}
