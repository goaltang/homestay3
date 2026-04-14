package com.homestay3.homestaybackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay3.homestaybackend.dto.AdministrativeDivisionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdministrativeDivisionServiceTest {

    private AdministrativeDivisionService service;

    @BeforeEach
    void setUp() {
        service = new AdministrativeDivisionService(new ObjectMapper());
        service.loadDivisionData();
    }

    @Test
    void getProvinceOptionsReturnsRealProvinceDataWithBackendCodes() {
        List<AdministrativeDivisionDTO> provinces = service.getProvinceOptions();

        assertTrue(provinces.size() >= 31);
        assertEquals("110000", provinces.get(0).getCode());
        assertEquals("北京市", provinces.get(0).getName());
    }

    @Test
    void getCityAndDistrictOptionsAcceptBackendCodes() {
        List<AdministrativeDivisionDTO> cities = service.getCityOptions("440000");
        List<AdministrativeDivisionDTO> districts = service.getDistrictOptions("440100");

        assertTrue(cities.stream()
                .anyMatch(city -> "440100".equals(city.getCode()) && "广州市".equals(city.getName())));
        assertTrue(districts.stream()
                .anyMatch(district -> "440106".equals(district.getCode()) && "天河区".equals(district.getName())));
    }

    @Test
    void getCityAndDistrictOptionsAcceptSourceCodes() {
        List<AdministrativeDivisionDTO> cities = service.getCityOptions("44");
        List<AdministrativeDivisionDTO> districts = service.getDistrictOptions("4401");

        assertTrue(cities.stream()
                .anyMatch(city -> "440100".equals(city.getCode()) && "广州市".equals(city.getName())));
        assertTrue(districts.stream()
                .anyMatch(district -> "440106".equals(district.getCode()) && "天河区".equals(district.getName())));
    }

    @Test
    void getRegionTreeReturnsBackendCompatibleTree() {
        AdministrativeDivisionDTO province = service.getRegionTree().get(0);
        AdministrativeDivisionDTO city = province.getChildren().get(0);

        assertEquals("110000", province.getCode());
        assertEquals("110100", city.getCode());
        assertEquals("北京市", city.getName());
        assertNotNull(city.getChildren());
    }
}
