package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.AdministrativeDivisionDTO;
import com.homestay3.homestaybackend.service.AdministrativeDivisionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/locations", "/api/v1/locations"})
@RequiredArgsConstructor
@CrossOrigin(
    origins = {"http://localhost:5173", "http://127.0.0.1:5173"},
    allowedHeaders = "*",
    methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE, RequestMethod.OPTIONS, RequestMethod.PATCH
    },
    exposedHeaders = {
        "Content-Type", "Content-Length", "Authorization",
        "Access-Control-Allow-Origin", "Access-Control-Allow-Headers",
        "Access-Control-Allow-Methods"
    },
    allowCredentials = "true",
    maxAge = 3600
)
public class LocationController {

    private static final Logger logger = LoggerFactory.getLogger(LocationController.class);

    private final AdministrativeDivisionService administrativeDivisionService;

    @GetMapping("/provinces")
    public ResponseEntity<Map<String, Object>> getProvinces() {
        logger.info("Get province list from administrative division data source");
        return ok(administrativeDivisionService.getProvinceOptions());
    }

    @GetMapping("/cities")
    public ResponseEntity<Map<String, Object>> getCities(@RequestParam String provinceCode) {
        logger.info("Get city list, provinceCode: {}", provinceCode);
        return ok(administrativeDivisionService.getCityOptions(provinceCode));
    }

    @GetMapping("/districts")
    public ResponseEntity<Map<String, Object>> getDistricts(@RequestParam String cityCode) {
        logger.info("Get district list, cityCode: {}", cityCode);
        return ok(administrativeDivisionService.getDistrictOptions(cityCode));
    }

    @GetMapping("/tree")
    public ResponseEntity<Map<String, Object>> getRegionTree() {
        logger.info("Get full province-city-district tree");
        return ok(administrativeDivisionService.getRegionTree());
    }

    private ResponseEntity<Map<String, Object>> ok(List<AdministrativeDivisionDTO> data) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", data
        ));
    }
}
