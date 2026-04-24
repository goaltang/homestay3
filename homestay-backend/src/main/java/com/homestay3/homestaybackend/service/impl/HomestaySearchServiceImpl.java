package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.dto.HomestaySearchRequest;
import com.homestay3.homestaybackend.dto.HomestaySearchResultDTO;
import com.homestay3.homestaybackend.dto.MapClusterDTO;
import com.homestay3.homestaybackend.entity.Amenity;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.entity.HomestayType;
import com.homestay3.homestaybackend.entity.Order;
import com.homestay3.homestaybackend.model.HomestayStatus;
import com.homestay3.homestaybackend.model.OrderStatus;
import com.homestay3.homestaybackend.repository.AmenityRepository;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.HomestayTypeRepository;
import com.homestay3.homestaybackend.service.HomestaySearchService;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomestaySearchServiceImpl implements HomestaySearchService {

    private final HomestayRepository homestayRepository;
    private final HomestayTypeRepository homestayTypeRepository;
    private final AmenityRepository amenityRepository;
    private final HomestayDtoAssembler homestayDtoAssembler;
    private final HomestaySpecificationSupport homestaySpecificationSupport;

    @Override
    public List<HomestayDTO> searchHomestays(HomestaySearchRequest request) {
        final String typeCodeToSearch;
        if (StringUtils.hasText(request.getPropertyType())) {
            Optional<HomestayType> typeOpt = homestayTypeRepository.findByNameIgnoreCase(request.getPropertyType());
            typeCodeToSearch = typeOpt.map(HomestayType::getCode).orElse(null);
        } else {
            typeCodeToSearch = null;
        }

        Specification<Homestay> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("status"), HomestayStatus.ACTIVE));

            if (StringUtils.hasText(request.getKeyword())) {
                String keyword = "%" + request.getKeyword() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("title"), keyword),
                        criteriaBuilder.like(root.get("description"), keyword),
                        criteriaBuilder.like(root.get("addressDetail"), keyword)));
            }

            if (StringUtils.hasText(request.getLocation())) {
                String locationLike = "%" + request.getLocation() + "%";
                String locationExact = request.getLocation();
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("provinceText"), locationLike),
                        criteriaBuilder.like(root.get("cityText"), locationLike),
                        criteriaBuilder.like(root.get("districtText"), locationLike),
                        criteriaBuilder.like(root.get("addressDetail"), locationLike),
                        criteriaBuilder.equal(root.get("provinceCode"), locationExact),
                        criteriaBuilder.equal(root.get("cityCode"), locationExact),
                        criteriaBuilder.equal(root.get("districtCode"), locationExact)));
            }

            if (StringUtils.hasText(request.getProvinceCode())) {
                predicates.add(root.get("provinceCode")
                        .in(homestaySpecificationSupport.getProvinceCodeCandidates(request.getProvinceCode())));
            }
            if (StringUtils.hasText(request.getCityCode())) {
                predicates.add(root.get("cityCode")
                        .in(homestaySpecificationSupport.getCityCodeCandidates(request.getCityCode())));
            }
            if (StringUtils.hasText(request.getDistrictCode())) {
                predicates.add(criteriaBuilder.equal(root.get("districtCode"), request.getDistrictCode().trim()));
            }

            if (request.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), request.getMinPrice()));
            }
            if (request.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), request.getMaxPrice()));
            }

            if (Stream.of(
                    request.getNorthEastLat(),
                    request.getNorthEastLng(),
                    request.getSouthWestLat(),
                    request.getSouthWestLng()).allMatch(Objects::nonNull)) {
                BigDecimal minLatitude = request.getNorthEastLat().min(request.getSouthWestLat());
                BigDecimal maxLatitude = request.getNorthEastLat().max(request.getSouthWestLat());
                BigDecimal minLongitude = request.getNorthEastLng().min(request.getSouthWestLng());
                BigDecimal maxLongitude = request.getNorthEastLng().max(request.getSouthWestLng());

                predicates.add(criteriaBuilder.isNotNull(root.get("latitude")));
                predicates.add(criteriaBuilder.isNotNull(root.get("longitude")));
                predicates.add(criteriaBuilder.between(root.get("latitude"), minLatitude, maxLatitude));
                predicates.add(criteriaBuilder.between(root.get("longitude"), minLongitude, maxLongitude));
            }

            if (request.getMinGuests() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("maxGuests"), request.getMinGuests()));
            }

            if (typeCodeToSearch != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), typeCodeToSearch));
            }

            List<String> requiredAmenityValues = request.getRequiredAmenities();
            if (requiredAmenityValues != null && !requiredAmenityValues.isEmpty()) {
                List<Amenity> requiredAmenities = amenityRepository.findByValueInIgnoreCase(requiredAmenityValues);
                if (!requiredAmenities.isEmpty()) {
                    for (Amenity amenity : requiredAmenities) {
                        predicates.add(criteriaBuilder.isMember(amenity, root.get("amenities")));
                    }
                } else {
                    predicates.add(criteriaBuilder.disjunction());
                }
            }

            LocalDate checkInDate = request.getCheckInDate();
            LocalDate checkOutDate = request.getCheckOutDate();
            if (checkInDate != null && checkOutDate != null) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<Order> orderRoot = subquery.from(Order.class);
                subquery.select(orderRoot.get("id"));

                Predicate homestayMatch = criteriaBuilder.equal(orderRoot.get("homestay"), root);
                Predicate statusMatch = orderRoot.get("status").in(
                        OrderStatus.CONFIRMED.name(),
                        OrderStatus.PAID.name());
                Predicate dateOverlap = criteriaBuilder.and(
                        criteriaBuilder.greaterThan(orderRoot.get("checkOutDate"), checkInDate),
                        criteriaBuilder.lessThan(orderRoot.get("checkInDate"), checkOutDate));

                subquery.where(criteriaBuilder.and(homestayMatch, statusMatch, dateOverlap));
                predicates.add(criteriaBuilder.not(criteriaBuilder.exists(subquery)));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        List<Homestay> homestays = homestayRepository.findAll(
                homestaySpecificationSupport.withDetailFetch(specification));

        List<String> criteriaFromSearch = new ArrayList<>();
        if (request.getRequiredAmenities() != null && !request.getRequiredAmenities().isEmpty()) {
            request.getRequiredAmenities().forEach(amenityCode ->
                    criteriaFromSearch.add("AMENITY_" + amenityCode.toUpperCase().replace(" ", "_")));
        }
        if (typeCodeToSearch != null) {
            criteriaFromSearch.add("PROPERTY_TYPE_" + typeCodeToSearch.toUpperCase());
        }

        List<String> finalCriteria = criteriaFromSearch.isEmpty()
                ? null
                : Collections.unmodifiableList(criteriaFromSearch);

        List<HomestayDTO> results = homestayDtoAssembler.toDTOs(homestays, finalCriteria);
        return applySearchSorting(results, request);
    }

    @Override
    public List<HomestaySearchResultDTO> searchHomestayResults(HomestaySearchRequest request) {
        return searchHomestays(request).stream()
                .map(homestayDtoAssembler::toSearchResultDTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<MapClusterDTO> getMapClusters(HomestaySearchRequest request) {
        int zoom = request.getZoom() != null ? request.getZoom() : 12;
        double gridSize = resolveClusterGridSize(zoom);

        Map<String, MapClusterAccumulator> clusters = new LinkedHashMap<>();
        searchHomestays(request).stream()
                .filter(Objects::nonNull)
                .filter(homestay -> homestay.getLatitude() != null && homestay.getLongitude() != null)
                .forEach(homestay -> {
                    double latitude = homestay.getLatitude();
                    double longitude = homestay.getLongitude();
                    long latitudeBucket = (long) Math.floor(latitude / gridSize);
                    long longitudeBucket = (long) Math.floor(longitude / gridSize);
                    String key = latitudeBucket + ":" + longitudeBucket;

                    clusters.computeIfAbsent(key, ignored -> new MapClusterAccumulator())
                            .add(latitude, longitude);
                });

        return clusters.values().stream()
                .map(MapClusterAccumulator::toDTO)
                .sorted(Comparator.comparing(MapClusterDTO::getCount).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<HomestayDTO> getNearbyHomestays(HomestaySearchRequest request) {
        int limit = request.getLimit() != null ? request.getLimit() : 20;
        return searchHomestaysWithinRadius(request, limit);
    }

    @Override
    public List<HomestaySearchResultDTO> getNearbyHomestayResults(HomestaySearchRequest request) {
        return getNearbyHomestays(request).stream()
                .map(homestayDtoAssembler::toSearchResultDTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<HomestayDTO> searchHomestaysNearLandmark(HomestaySearchRequest request) {
        return searchHomestaysWithinRadius(request, request.getLimit());
    }

    @Override
    public List<HomestaySearchResultDTO> searchHomestayResultsNearLandmark(HomestaySearchRequest request) {
        return searchHomestaysNearLandmark(request).stream()
                .map(homestayDtoAssembler::toSearchResultDTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<HomestayDTO> searchHomestays(
            String keyword,
            String provinceCode,
            String cityCode,
            Integer minPrice,
            Integer maxPrice,
            Integer guests,
            String type) {
        BigDecimal minPriceDecimal = minPrice != null ? BigDecimal.valueOf(minPrice) : null;
        BigDecimal maxPriceDecimal = maxPrice != null ? BigDecimal.valueOf(maxPrice) : null;

        List<Homestay> homestays = homestayRepository.searchHomestays(
                keyword,
                minPriceDecimal,
                maxPriceDecimal,
                guests,
                type);
        return homestayDtoAssembler.toDTOs(homestays, null);
    }

    private double resolveClusterGridSize(int zoom) {
        int normalizedZoom = Math.max(1, Math.min(20, zoom));
        return Math.max(0.0005d, 180.0d / Math.pow(2.0d, normalizedZoom));
    }

    private List<HomestayDTO> applySearchSorting(List<HomestayDTO> results, HomestaySearchRequest request) {
        if (results == null || results.isEmpty() || request == null || !StringUtils.hasText(request.getSortBy())) {
            return results;
        }

        if (!isDistanceSort(request.getSortBy())) {
            return results;
        }

        if (!hasDistanceSortOrigin(request)) {
            log.warn("Distance sort requested without origin coordinates");
            return results;
        }

        double latitude = request.getLatitude().doubleValue();
        double longitude = request.getLongitude().doubleValue();
        results.forEach(homestay -> attachDistanceKm(homestay, latitude, longitude));

        boolean descending = isDescending(request.getSortDirection());
        Comparator<HomestayDTO> comparator = (left, right) -> compareDistanceKm(left, right, descending);
        return results.stream()
                .sorted(comparator.thenComparing(HomestayDTO::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
    }

    private boolean isDistanceSort(String sortBy) {
        return "DISTANCE".equalsIgnoreCase(sortBy)
                || "distance".equalsIgnoreCase(sortBy)
                || "distanceKm".equalsIgnoreCase(sortBy);
    }

    private boolean hasDistanceSortOrigin(HomestaySearchRequest request) {
        return request.getLatitude() != null && request.getLongitude() != null;
    }

    private boolean isDescending(String sortDirection) {
        return "desc".equalsIgnoreCase(sortDirection)
                || "descending".equalsIgnoreCase(sortDirection);
    }

    private int compareDistanceKm(HomestayDTO left, HomestayDTO right, boolean descending) {
        Double leftDistance = left.getDistanceKm();
        Double rightDistance = right.getDistanceKm();

        if (leftDistance == null && rightDistance == null) {
            return 0;
        }
        if (leftDistance == null) {
            return 1;
        }
        if (rightDistance == null) {
            return -1;
        }

        int result = Double.compare(leftDistance, rightDistance);
        return descending ? -result : result;
    }

    private List<HomestayDTO> searchHomestaysWithinRadius(HomestaySearchRequest request, Integer limit) {
        double latitude = request.getLatitude().doubleValue();
        double longitude = request.getLongitude().doubleValue();
        double radiusKm = request.getRadiusKm().doubleValue();

        applyNearbyBoundingBox(request, latitude, longitude, radiusKm);

        Stream<HomestayDTO> results = searchHomestays(request).stream()
                .filter(Objects::nonNull)
                .filter(homestay -> homestay.getLatitude() != null && homestay.getLongitude() != null)
                .filter(homestay -> calculateDistanceKm(
                        latitude,
                        longitude,
                        homestay.getLatitude(),
                        homestay.getLongitude()) <= radiusKm)
                .map(homestay -> {
                    double distanceKm = calculateDistanceKm(
                            latitude,
                            longitude,
                            homestay.getLatitude(),
                            homestay.getLongitude());
                    homestay.setDistanceKm(roundDistance(distanceKm));
                    return homestay;
                })
                .sorted(Comparator.comparing(HomestayDTO::getDistanceKm));

        if (limit != null) {
            results = results.limit(limit);
        }

        return results.collect(Collectors.toList());
    }

    private void applyNearbyBoundingBox(
            HomestaySearchRequest request,
            double latitude,
            double longitude,
            double radiusKm) {
        if (Stream.of(
                request.getNorthEastLat(),
                request.getNorthEastLng(),
                request.getSouthWestLat(),
                request.getSouthWestLng()).allMatch(Objects::nonNull)) {
            return;
        }

        double latitudeDelta = radiusKm / 111.32d;
        double longitudeScale = Math.max(Math.cos(Math.toRadians(latitude)), 0.000001d);
        double longitudeDelta = radiusKm / (111.32d * longitudeScale);
        double minLongitude = longitude - longitudeDelta;
        double maxLongitude = longitude + longitudeDelta;

        if (minLongitude < -180d || maxLongitude > 180d) {
            return;
        }

        request.setNorthEastLat(BigDecimal.valueOf(Math.min(90d, latitude + latitudeDelta)));
        request.setSouthWestLat(BigDecimal.valueOf(Math.max(-90d, latitude - latitudeDelta)));
        request.setNorthEastLng(BigDecimal.valueOf(maxLongitude));
        request.setSouthWestLng(BigDecimal.valueOf(minLongitude));
    }

    private double calculateDistanceKm(double fromLatitude, double fromLongitude, double toLatitude, double toLongitude) {
        double earthRadiusKm = 6371.0088d;
        double latitudeDelta = Math.toRadians(toLatitude - fromLatitude);
        double longitudeDelta = Math.toRadians(toLongitude - fromLongitude);
        double fromLatitudeRadians = Math.toRadians(fromLatitude);
        double toLatitudeRadians = Math.toRadians(toLatitude);

        double a = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(fromLatitudeRadians) * Math.cos(toLatitudeRadians)
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private void attachDistanceKm(HomestayDTO homestay, double latitude, double longitude) {
        if (homestay == null || homestay.getLatitude() == null || homestay.getLongitude() == null) {
            return;
        }

        double distanceKm = calculateDistanceKm(
                latitude,
                longitude,
                homestay.getLatitude(),
                homestay.getLongitude());
        homestay.setDistanceKm(roundDistance(distanceKm));
    }

    private double roundDistance(double value) {
        return Math.round(value * 100d) / 100d;
    }

    private static class MapClusterAccumulator {
        private double latitudeSum;
        private double longitudeSum;
        private int count;

        void add(double latitude, double longitude) {
            latitudeSum += latitude;
            longitudeSum += longitude;
            count++;
        }

        MapClusterDTO toDTO() {
            return MapClusterDTO.builder()
                    .latitude(roundCoordinate(latitudeSum / count))
                    .longitude(roundCoordinate(longitudeSum / count))
                    .count(count)
                    .build();
        }

        private static double roundCoordinate(double value) {
            return Math.round(value * 1_000_000d) / 1_000_000d;
        }
    }
}
