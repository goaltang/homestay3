package com.homestay3.homestaybackend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay3.homestaybackend.dto.AdministrativeDivisionDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdministrativeDivisionService {

    private static final String DATA_PATH = "administrative-divisions/china-pca-code.json";
    private static final Set<String> MUNICIPALITY_CODES = Set.of("11", "12", "31", "50");

    private final ObjectMapper objectMapper;
    private final Map<String, AdministrativeDivisionDTO> provincesByCode = new LinkedHashMap<>();
    private List<AdministrativeDivisionDTO> divisionTree = List.of();

    @PostConstruct
    public void loadDivisionData() {
        ClassPathResource resource = new ClassPathResource(DATA_PATH);
        try (InputStream inputStream = resource.getInputStream()) {
            divisionTree = objectMapper.readValue(
                    inputStream,
                    new TypeReference<List<AdministrativeDivisionDTO>>() {
                    });
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load administrative division data", exception);
        }

        provincesByCode.clear();
        divisionTree.stream()
                .filter(Objects::nonNull)
                .filter(province -> province.getCode() != null)
                .forEach(province -> provincesByCode.put(province.getCode(), province));
    }

    public List<AdministrativeDivisionDTO> getProvinceOptions() {
        return divisionTree.stream()
                .map(this::toProvinceOption)
                .toList();
    }

    public List<AdministrativeDivisionDTO> getCityOptions(String provinceCode) {
        AdministrativeDivisionDTO province = provincesByCode.get(toSourceProvinceCode(provinceCode));
        if (province == null || province.getChildren() == null) {
            return List.of();
        }

        return province.getChildren().stream()
                .map(city -> toCityOption(province, city))
                .toList();
    }

    public List<AdministrativeDivisionDTO> getDistrictOptions(String cityCode) {
        String sourceCityCode = toSourceCityCode(cityCode);

        return divisionTree.stream()
                .filter(province -> province.getChildren() != null)
                .flatMap(province -> province.getChildren().stream())
                .filter(city -> sourceCityCode.equals(city.getCode()))
                .findFirst()
                .map(city -> city.getChildren() == null
                        ? List.<AdministrativeDivisionDTO>of()
                        : city.getChildren().stream()
                                .map(this::toDistrictOption)
                                .toList())
                .orElseGet(List::of);
    }

    public List<AdministrativeDivisionDTO> getRegionTree() {
        return divisionTree.stream()
                .map(province -> AdministrativeDivisionDTO.builder()
                        .code(toBackendProvinceCode(province.getCode()))
                        .name(province.getName())
                        .children(toCityTree(province))
                        .build())
                .toList();
    }

    private List<AdministrativeDivisionDTO> toCityTree(AdministrativeDivisionDTO province) {
        if (province.getChildren() == null) {
            return List.of();
        }

        return province.getChildren().stream()
                .map(city -> AdministrativeDivisionDTO.builder()
                        .code(toBackendCityCode(city.getCode()))
                        .name(resolveCityName(province, city))
                        .children(toDistrictTree(city))
                        .build())
                .toList();
    }

    private List<AdministrativeDivisionDTO> toDistrictTree(AdministrativeDivisionDTO city) {
        if (city.getChildren() == null) {
            return List.of();
        }

        return city.getChildren().stream()
                .map(this::toDistrictOption)
                .toList();
    }

    private AdministrativeDivisionDTO toProvinceOption(AdministrativeDivisionDTO province) {
        return AdministrativeDivisionDTO.builder()
                .code(toBackendProvinceCode(province.getCode()))
                .name(province.getName())
                .build();
    }

    private AdministrativeDivisionDTO toCityOption(
            AdministrativeDivisionDTO province,
            AdministrativeDivisionDTO city) {
        return AdministrativeDivisionDTO.builder()
                .code(toBackendCityCode(city.getCode()))
                .name(resolveCityName(province, city))
                .build();
    }

    private AdministrativeDivisionDTO toDistrictOption(AdministrativeDivisionDTO district) {
        return AdministrativeDivisionDTO.builder()
                .code(district.getCode())
                .name(district.getName())
                .build();
    }

    private String resolveCityName(
            AdministrativeDivisionDTO province,
            AdministrativeDivisionDTO city) {
        if (MUNICIPALITY_CODES.contains(province.getCode()) && "市辖区".equals(city.getName())) {
            return province.getName();
        }

        return city.getName();
    }

    private String toSourceProvinceCode(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }

        String normalized = code.trim();
        if (normalized.length() == 6 && normalized.endsWith("0000")) {
            return normalized.substring(0, 2);
        }

        return normalized;
    }

    private String toSourceCityCode(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }

        String normalized = code.trim();
        if (normalized.length() == 6 && normalized.endsWith("00")) {
            return normalized.substring(0, 4);
        }

        return normalized;
    }

    private String toBackendProvinceCode(String code) {
        if (code == null) {
            return null;
        }

        return code.length() == 2 ? code + "0000" : code;
    }

    private String toBackendCityCode(String code) {
        if (code == null) {
            return null;
        }

        return code.length() == 4 ? code + "00" : code;
    }
}
