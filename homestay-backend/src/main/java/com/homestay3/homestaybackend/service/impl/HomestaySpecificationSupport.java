package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.entity.Homestay;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j
@Component
public class HomestaySpecificationSupport {

    public Specification<Homestay> withDetailFetch(Specification<Homestay> delegate) {
        return (root, query, criteriaBuilder) -> {
            if (query != null && !isCountQuery(query)) {
                root.fetch("owner", JoinType.LEFT);
                root.fetch("amenities", JoinType.LEFT);
                root.fetch("images", JoinType.LEFT);
                query.distinct(true);
            }

            return delegate == null
                    ? criteriaBuilder.conjunction()
                    : delegate.toPredicate(root, query, criteriaBuilder);
        };
    }

    public Set<String> getProvinceCodeCandidates(String code) {
        Set<String> candidates = new LinkedHashSet<>();
        String normalized = code.trim();
        candidates.add(normalized);

        if (normalized.length() == 2) {
            candidates.add(normalized + "0000");
        } else if (normalized.length() == 6 && normalized.endsWith("0000")) {
            candidates.add(normalized.substring(0, 2));
        }

        return candidates;
    }

    public Set<String> getCityCodeCandidates(String code) {
        Set<String> candidates = new LinkedHashSet<>();
        String normalized = code.trim();
        candidates.add(normalized);

        if (normalized.length() == 4) {
            candidates.add(normalized + "00");
        } else if (normalized.length() == 6 && normalized.endsWith("00")) {
            candidates.add(normalized.substring(0, 4));
        }

        return candidates;
    }

    public Specification<Homestay> withOwnerAndGroupFetch(Specification<Homestay> delegate) {
        return (root, query, criteriaBuilder) -> {
            if (query != null && !isCountQuery(query)) {
                root.fetch("owner", JoinType.LEFT);
                root.fetch("group", JoinType.LEFT);
                query.distinct(true);
            }

            return delegate == null
                    ? criteriaBuilder.conjunction()
                    : delegate.toPredicate(root, query, criteriaBuilder);
        };
    }

    private boolean isCountQuery(CriteriaQuery<?> query) {
        Class<?> resultType = query.getResultType();
        return Long.class.equals(resultType) || long.class.equals(resultType);
    }
}
