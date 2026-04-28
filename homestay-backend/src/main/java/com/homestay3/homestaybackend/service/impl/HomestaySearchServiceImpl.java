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
import com.homestay3.homestaybackend.model.search.HomestayDocument;
import com.homestay3.homestaybackend.repository.AmenityRepository;
import com.homestay3.homestaybackend.repository.HomestayDocumentRepository;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.HomestayTypeRepository;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.repository.ReviewRepository;
import com.homestay3.homestaybackend.service.HomestaySearchService;
import com.homestay3.homestaybackend.service.search.HomestayIndexingService;
import com.homestay3.homestaybackend.service.search.UserBehaviorTrackingService;
import com.homestay3.homestaybackend.util.SortUtils;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final ObjectProvider<HomestayIndexingService> homestayIndexingServiceProvider;
    private final ObjectProvider<HomestayDocumentRepository> homestayDocumentRepositoryProvider;
    private final ObjectProvider<ElasticsearchOperations> elasticsearchOperationsProvider;
    private final UserBehaviorTrackingService userBehaviorTrackingService;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;

    @Override
    public List<HomestayDTO> searchHomestays(HomestaySearchRequest request) {
        // 尝试 ES 全文搜索（仅限有 keyword 时）
        if (StringUtils.hasText(request.getKeyword())) {
            try {
                HomestayIndexingService indexingService = homestayIndexingServiceProvider.getIfAvailable();
                if (indexingService != null && indexingService.isElasticsearchAvailable()) {
                    List<HomestayDTO> esResults = searchByElasticsearch(request);
                    if (esResults != null && !esResults.isEmpty()) {
                        try {
                            userBehaviorTrackingService.trackSearch(
                                    null, null, request.getKeyword(),
                                    request.getCityCode(), request.getPropertyType());
                        } catch (Exception ignored) {}
                        return applySearchSorting(esResults, request);
                    }
                }
            } catch (Exception e) {
                log.warn("ES search failed, falling back to JPA: {}", e.getMessage());
            }
        }

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
    public Page<HomestaySearchResultDTO> searchHomestayPage(HomestaySearchRequest request) {
        int page = request.getPage() != null && request.getPage() >= 0 ? request.getPage() : 0;
        int size = request.getSize() != null && request.getSize() > 0 ? request.getSize() : 12;

        // ES 路径：暂全量查询后内存分页
        if (StringUtils.hasText(request.getKeyword())) {
            try {
                HomestayIndexingService indexingService = homestayIndexingServiceProvider.getIfAvailable();
                if (indexingService != null && indexingService.isElasticsearchAvailable()) {
                    List<HomestayDTO> esResults = searchByElasticsearch(request);
                    esResults = applySearchSorting(esResults, request);
                    return toSearchResultPage(esResults, page, size);
                }
            } catch (Exception e) {
                log.warn("ES search failed, falling back to JPA: {}", e.getMessage());
            }
        }

        final String typeCodeToSearch = resolveTypeCode(request.getPropertyType());

        // Distance / Rating 排序需要全量查询后内存计算
        if ((isDistanceSort(request.getSortBy()) && hasDistanceSortOrigin(request)) || isRatingSort(request.getSortBy())) {
            Specification<Homestay> specification = buildSearchSpecification(request, typeCodeToSearch);
            List<Homestay> homestays = homestayRepository.findAll(
                    homestaySpecificationSupport.withDetailFetch(specification));
            List<String> finalCriteria = buildFinalCriteria(request, typeCodeToSearch);
            List<HomestayDTO> dtos = homestayDtoAssembler.toDTOs(homestays, finalCriteria);
            dtos = applySearchSorting(dtos, request);
            return toSearchResultPage(dtos, page, size);
        }

        // JPA 路径：数据库层排序 + 分页
        Specification<Homestay> specification = buildSearchSpecification(request, typeCodeToSearch);
        Sort sort = buildJpaSort(request);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Homestay> homestayPage = homestayRepository.findAll(
                homestaySpecificationSupport.withDetailFetch(specification), pageable);

        List<String> finalCriteria = buildFinalCriteria(request, typeCodeToSearch);
        List<HomestaySearchResultDTO> content = homestayDtoAssembler.toDTOs(homestayPage.getContent(), finalCriteria)
                .stream()
                .map(homestayDtoAssembler::toSearchResultDTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, homestayPage.getTotalElements());
    }

    private String resolveTypeCode(String propertyType) {
        if (!StringUtils.hasText(propertyType)) {
            return null;
        }
        Optional<HomestayType> typeOpt = homestayTypeRepository.findByNameIgnoreCase(propertyType);
        return typeOpt.map(HomestayType::getCode).orElse(null);
    }

    private Specification<Homestay> buildSearchSpecification(HomestaySearchRequest request, String typeCodeToSearch) {
        return (root, query, criteriaBuilder) -> {
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
    }

    private List<String> buildFinalCriteria(HomestaySearchRequest request, String typeCodeToSearch) {
        List<String> criteriaFromSearch = new ArrayList<>();
        if (request.getRequiredAmenities() != null && !request.getRequiredAmenities().isEmpty()) {
            request.getRequiredAmenities().forEach(amenityCode ->
                    criteriaFromSearch.add("AMENITY_" + amenityCode.toUpperCase().replace(" ", "_")));
        }
        if (typeCodeToSearch != null) {
            criteriaFromSearch.add("PROPERTY_TYPE_" + typeCodeToSearch.toUpperCase());
        }
        return criteriaFromSearch.isEmpty() ? null : Collections.unmodifiableList(criteriaFromSearch);
    }

    private Sort buildJpaSort(HomestaySearchRequest request) {
        String sortParam = StringUtils.hasText(request.getSort()) ? request.getSort()
                : (StringUtils.hasText(request.getSortBy()) ? request.getSortBy() + "," + request.getSortDirection() : "");
        Map<String, String> fieldMapping = Map.of(
                "price", "price",
                "pricepernight", "price",
                "createdat", "createdAt",
                "created_at", "createdAt",
                "default", "id",
                "rating", "id"
        );
        Set<String> allowedFields = Set.of("id", "price", "createdAt", "updatedAt", "title");
        return SortUtils.parseSort(sortParam, fieldMapping, allowedFields);
    }

    private Page<HomestaySearchResultDTO> toSearchResultPage(List<HomestayDTO> dtos, int page, int size) {
        int total = dtos.size();
        int start = page * size;
        if (start >= total) {
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), total);
        }
        int end = Math.min(start + size, total);
        List<HomestaySearchResultDTO> content = dtos.subList(start, end).stream()
                .map(homestayDtoAssembler::toSearchResultDTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new PageImpl<>(content, PageRequest.of(page, size), total);
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

        if (isDistanceSort(request.getSortBy())) {
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

        if (isRatingSort(request.getSortBy())) {
            boolean descending = isDescending(request.getSortDirection());
            // 批量预加载评分，避免 N+1
            List<Long> ids = results.stream().map(HomestayDTO::getId).filter(Objects::nonNull).collect(Collectors.toList());
            if (!ids.isEmpty()) {
                Map<Long, Double> ratingMap = toMapDouble(reviewRepository.getAverageRatingByHomestayIds(ids));
                results.forEach(dto -> dto.setRating(ratingMap.getOrDefault(dto.getId(), 0.0)));
            }
            Comparator<HomestayDTO> comparator = (left, right) -> {
                Double leftRating = left.getRating();
                Double rightRating = right.getRating();
                if (leftRating == null && rightRating == null) return 0;
                if (leftRating == null) return 1;
                if (rightRating == null) return -1;
                int result = Double.compare(leftRating, rightRating);
                return descending ? -result : result;
            };
            return results.stream()
                    .sorted(comparator.thenComparing(HomestayDTO::getId, Comparator.nullsLast(Long::compareTo)))
                    .collect(Collectors.toList());
        }

        return results;
    }

    private boolean isDistanceSort(String sortBy) {
        return "DISTANCE".equalsIgnoreCase(sortBy)
                || "distance".equalsIgnoreCase(sortBy)
                || "distanceKm".equalsIgnoreCase(sortBy);
    }

    private boolean isRatingSort(String sortBy) {
        return "RATING".equalsIgnoreCase(sortBy)
                || "rating".equalsIgnoreCase(sortBy);
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

    @SuppressWarnings("unchecked")
    private static Map<Long, Double> toMapDouble(List<Object[]> rows) {
        if (rows == null) return Map.of();
        return rows.stream().collect(Collectors.toMap(
                r -> ((Number) r[0]).longValue(),
                r -> ((Number) r[1]).doubleValue(),
                (a, b) -> a));
    }

    private List<HomestayDTO> searchByElasticsearch(HomestaySearchRequest request) {
        String keyword = request.getKeyword();
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }

        ElasticsearchOperations elasticsearchOperations = elasticsearchOperationsProvider.getIfAvailable();
        if (elasticsearchOperations == null) {
            return Collections.emptyList();
        }

        try {
            String queryJson = buildElasticsearchQueryJson(request);
            StringQuery stringQuery = new StringQuery(queryJson, PageRequest.of(0, 10000));
            SearchHits<HomestayDocument> searchHits = elasticsearchOperations.search(
                    stringQuery, HomestayDocument.class);

            if (searchHits == null || searchHits.isEmpty()) {
                return Collections.emptyList();
            }

            List<Long> homestayIds = searchHits.getSearchHits().stream()
                    .map(hit -> hit.getContent().getId())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (homestayIds.isEmpty()) {
                return Collections.emptyList();
            }

            // 用 ID 列表去 JPA 查询完整实体，保留原有 DTO 组装逻辑
            List<Homestay> homestays = homestayRepository.findAllById(homestayIds);

            // 保持 ES 返回的顺序
            Map<Long, Homestay> homestayMap = homestays.stream()
                    .collect(Collectors.toMap(Homestay::getId, h -> h, (a, b) -> a));
            List<Homestay> orderedHomestays = homestayIds.stream()
                    .map(homestayMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // 补充：可订日期检查（ES 搜索绕过了 JPA 的日期冲突子查询）
            if (request.getCheckInDate() != null && request.getCheckOutDate() != null) {
                List<Long> conflictIds = orderRepository.findConflictingHomestayIds(
                        homestayIds, request.getCheckInDate(), request.getCheckOutDate());
                if (conflictIds != null && !conflictIds.isEmpty()) {
                    orderedHomestays = orderedHomestays.stream()
                            .filter(h -> !conflictIds.contains(h.getId()))
                            .collect(Collectors.toList());
                }
            }

            List<String> finalCriteria = buildFinalCriteria(request, resolveTypeCode(request.getPropertyType()));
            return homestayDtoAssembler.toDTOs(orderedHomestays, finalCriteria);
        } catch (Exception e) {
            log.warn("ES advanced search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String buildElasticsearchQueryJson(HomestaySearchRequest request) {
        String keyword = jsonEscape(request.getKeyword());
        double minPrice = request.getMinPrice() != null ? request.getMinPrice().doubleValue() : 0.0;
        double maxPrice = request.getMaxPrice() != null ? request.getMaxPrice().doubleValue() : Double.MAX_VALUE;
        int minGuests = request.getMinGuests() != null ? request.getMinGuests() : 1;

        StringBuilder filters = new StringBuilder();
        filters.append(String.format("{\"range\":{\"price\":{\"gte\":%.2f,\"lte\":%.2f}}},", minPrice, maxPrice));
        filters.append(String.format("{\"range\":{\"maxGuests\":{\"gte\":%d}}},", minGuests));

        if (StringUtils.hasText(request.getProvinceCode())) {
            Set<String> candidates = homestaySpecificationSupport.getProvinceCodeCandidates(request.getProvinceCode());
            filters.append("{\"terms\":{\"provinceCode\":[");
            boolean first = true;
            for (String c : candidates) {
                if (!first) filters.append(",");
                filters.append("\"").append(jsonEscape(c)).append("\"");
                first = false;
            }
            filters.append("]}},");
        }

        if (StringUtils.hasText(request.getCityCode())) {
            Set<String> candidates = homestaySpecificationSupport.getCityCodeCandidates(request.getCityCode());
            filters.append("{\"terms\":{\"cityCode\":[");
            boolean first = true;
            for (String c : candidates) {
                if (!first) filters.append(",");
                filters.append("\"").append(jsonEscape(c)).append("\"");
                first = false;
            }
            filters.append("]}},");
        }

        if (StringUtils.hasText(request.getDistrictCode())) {
            filters.append(String.format("{\"term\":{\"districtCode\":\"%s\"}},",
                    jsonEscape(request.getDistrictCode().trim())));
        }

        String typeCode = resolveTypeCode(request.getPropertyType());
        if (typeCode != null) {
            filters.append(String.format("{\"term\":{\"type\":\"%s\"}},", jsonEscape(typeCode)));
        }

        // 设施筛选：AND 语义（必须同时包含所有指定设施）
        if (request.getRequiredAmenities() != null && !request.getRequiredAmenities().isEmpty()) {
            for (String a : request.getRequiredAmenities()) {
                filters.append(String.format("{\"term\":{\"amenities\":\"%s\"}},", jsonEscape(a)));
            }
        }

        // location 文本筛选：匹配 addressDetail 或地区编码
        if (StringUtils.hasText(request.getLocation())) {
            String locationExact = jsonEscape(request.getLocation().trim());
            String locationFuzzy = jsonEscape(request.getLocation().trim());
            filters.append(String.format(
                    "{\"bool\":{\"should\":[" +
                    "{\"match\":{\"addressDetail\":\"%s\"}}," +
                    "{\"term\":{\"provinceCode\":\"%s\"}}," +
                    "{\"term\":{\"cityCode\":\"%s\"}}," +
                    "{\"term\":{\"districtCode\":\"%s\"}}" +
                    "],\"minimum_should_match\":1}},",
                    locationFuzzy, locationExact, locationExact, locationExact));
        }

        if (Stream.of(request.getNorthEastLat(), request.getNorthEastLng(),
                request.getSouthWestLat(), request.getSouthWestLng()).allMatch(Objects::nonNull)) {
            double topLat = request.getNorthEastLat().max(request.getSouthWestLat()).doubleValue();
            double bottomLat = request.getNorthEastLat().min(request.getSouthWestLat()).doubleValue();
            double leftLng = request.getNorthEastLng().min(request.getSouthWestLng()).doubleValue();
            double rightLng = request.getNorthEastLng().max(request.getSouthWestLng()).doubleValue();
            filters.append(String.format(
                    "{\"geo_bounding_box\":{\"location\":{\"top_left\":{\"lat\":%.6f,\"lon\":%.6f},\"bottom_right\":{\"lat\":%.6f,\"lon\":%.6f}}}},",
                    topLat, leftLng, bottomLat, rightLng));
        }

        String filterStr = filters.toString();
        if (filterStr.endsWith(",")) {
            filterStr = filterStr.substring(0, filterStr.length() - 1);
        }

        return String.format(
                "{\"function_score\":{\"query\":{\"bool\":{\"must\":[{\"match\":{\"status\":\"ACTIVE\"}},{\"multi_match\":{\"query\":\"%s\",\"fields\":[\"title^3\",\"description^2\",\"addressDetail^2\",\"amenities\"],\"type\":\"best_fields\",\"fuzziness\":\"AUTO\"}}],\"filter\":[%s]}},\"functions\":[{\"field_value_factor\":{\"field\":\"rating\",\"factor\":0.20,\"modifier\":\"log1p\",\"missing\":0}},{\"field_value_factor\":{\"field\":\"bookingCount\",\"factor\":0.075,\"modifier\":\"log1p\",\"missing\":0}},{\"field_value_factor\":{\"field\":\"favoriteCount\",\"factor\":0.075,\"modifier\":\"log1p\",\"missing\":0}},{\"gauss\":{\"createdAt\":{\"origin\":\"now\",\"scale\":\"30d\",\"decay\":0.5}}}],\"score_mode\":\"sum\",\"boost_mode\":\"multiply\"}}",
                keyword, filterStr);
    }

    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
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
