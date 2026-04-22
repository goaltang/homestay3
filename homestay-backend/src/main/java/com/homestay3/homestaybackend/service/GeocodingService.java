package com.homestay3.homestaybackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay3.homestaybackend.dto.AdministrativeDivisionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeocodingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AdministrativeDivisionService administrativeDivisionService;
    private final Map<String, Optional<Coordinates>> cache = new ConcurrentHashMap<>();

    @Value("${app.map.geocoding.enabled:true}")
    private boolean enabled;

    @Value("${app.map.amap.api-key:}")
    private String amapApiKey;

    @Value("${app.map.amap.geocode-url:https://restapi.amap.com/v3/geocode/geo}")
    private String geocodeUrl;

    public Optional<Coordinates> geocode(
            String provinceCode,
            String cityCode,
            String districtCode,
            String addressDetail) {
        String address = buildAddress(provinceCode, cityCode, districtCode, addressDetail);
        if (!StringUtils.hasText(address)) {
            return Optional.empty();
        }

        return geocode(address);
    }

    private Optional<Coordinates> geocode(String address) {
        if (!enabled) {
            return Optional.empty();
        }

        if (!StringUtils.hasText(amapApiKey)) {
            log.debug("Amap geocoding skipped because app.map.amap.api-key is empty");
            return Optional.empty();
        }

        return cache.computeIfAbsent(address, this::fetchCoordinates);
    }

    private Optional<Coordinates> fetchCoordinates(String address) {
        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(geocodeUrl)
                    .queryParam("address", address)
                    .queryParam("key", amapApiKey)
                    .queryParam("output", "json")
                    .build()
                    .encode()
                    .toUri();

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Amap geocoding failed for address={}, status={}", address, response.getStatusCode());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            if (!"1".equals(root.path("status").asText())) {
                log.warn("Amap geocoding returned non-success for address={}, info={}", address, root.path("info").asText());
                return Optional.empty();
            }

            JsonNode geocodes = root.path("geocodes");
            if (!geocodes.isArray() || geocodes.size() == 0) {
                log.warn("Amap geocoding returned no candidates for address={}", address);
                return Optional.empty();
            }

            String location = geocodes.get(0).path("location").asText();
            String[] parts = location.split(",");
            if (parts.length != 2) {
                log.warn("Amap geocoding returned invalid location={} for address={}", location, address);
                return Optional.empty();
            }

            BigDecimal longitude = new BigDecimal(parts[0].trim());
            BigDecimal latitude = new BigDecimal(parts[1].trim());
            if (!isValidCoordinate(latitude, longitude)) {
                log.warn("Amap geocoding returned out-of-range coordinate={},{} for address={}", latitude, longitude, address);
                return Optional.empty();
            }

            log.info("Amap geocoding succeeded for address={}", address);
            return Optional.of(new Coordinates(latitude, longitude));
        } catch (RestClientException | IOException | NumberFormatException exception) {
            log.warn("Amap geocoding failed for address={}: {}", address, exception.getMessage());
            return Optional.empty();
        }
    }

    private String buildAddress(
            String provinceCode,
            String cityCode,
            String districtCode,
            String addressDetail) {
        StringBuilder builder = new StringBuilder();

        String provinceName = findName(administrativeDivisionService.getProvinceOptions(), provinceCode);
        appendPart(builder, provinceName);

        String cityName = findName(administrativeDivisionService.getCityOptions(provinceCode), cityCode);
        if (!sameText(provinceName, cityName)) {
            appendPart(builder, cityName);
        }

        appendPart(builder, findName(administrativeDivisionService.getDistrictOptions(cityCode), districtCode));
        appendPart(builder, addressDetail);

        return builder.toString();
    }

    private String findName(List<AdministrativeDivisionDTO> options, String code) {
        if (!StringUtils.hasText(code) || options == null || options.isEmpty()) {
            return null;
        }

        String normalizedCode = code.trim();
        return options.stream()
                .filter(option -> normalizedCode.equals(option.getCode()))
                .map(AdministrativeDivisionDTO::getName)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private void appendPart(StringBuilder builder, String value) {
        if (StringUtils.hasText(value)) {
            builder.append(value.trim());
        }
    }

    private boolean sameText(String left, String right) {
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return false;
        }
        return left.trim().equals(right.trim());
    }

    private boolean isValidCoordinate(BigDecimal latitude, BigDecimal longitude) {
        return isBetween(latitude, BigDecimal.valueOf(-90), BigDecimal.valueOf(90))
                && isBetween(longitude, BigDecimal.valueOf(-180), BigDecimal.valueOf(180));
    }

    private boolean isBetween(BigDecimal value, BigDecimal min, BigDecimal max) {
        return value != null && value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }

    public record Coordinates(BigDecimal latitude, BigDecimal longitude) {
    }
}
