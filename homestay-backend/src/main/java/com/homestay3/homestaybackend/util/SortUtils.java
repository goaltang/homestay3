package com.homestay3.homestaybackend.util;

import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * JPA 多字段排序工具类。
 *
 * <p>支持从字符串解析多字段排序规则，格式如下：</p>
 * <ul>
 *   <li>单字段：{@code createdAt,desc}</li>
 *   <li>多字段：{@code createdAt,desc;id,asc;price,asc}</li>
 *   <li>简写方向：{@code createdAt,desc} 或 {@code createdAt,-}</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>
 * Sort sort = SortUtils.parseSort(
 *     sortParam,
 *     Map.of("hostName", "homestay.owner.username", "createTime", "createdAt"),
 *     Set.of("id", "createdAt", "updatedAt", "totalAmount", "status")
 * );
 * </pre>
 */
public final class SortUtils {

    private SortUtils() {
        // 工具类禁止实例化
    }

    /**
     * 默认排序字段（当传入参数为空或解析结果为空时回退）。
     */
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "id");

    /**
     * 解析排序字符串。
     *
     * @param sortParam      排序参数字符串，例如 {@code "createdAt,desc;id,asc"}
     * @param fieldMapping   字段映射（DTO/前端字段名 → 实体字段名），可为 null
     * @param allowedFields  允许的字段白名单，为空则不做限制（不建议生产环境使用）
     * @return Spring Data JPA {@link Sort} 对象
     */
    public static Sort parseSort(String sortParam,
                                  Map<String, String> fieldMapping,
                                  Set<String> allowedFields) {
        if (!StringUtils.hasText(sortParam)) {
            return DEFAULT_SORT;
        }

        List<Sort.Order> orders = new ArrayList<>();
        String[] segments = sortParam.split(";");

        for (String segment : segments) {
            String trimmed = segment.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String[] parts = trimmed.split(",", 2);
            String field = parts[0].trim();
            String directionStr = parts.length > 1 ? parts[1].trim() : "asc";

            if (field.isEmpty()) {
                continue;
            }

            // 字段映射
            if (fieldMapping != null && fieldMapping.containsKey(field)) {
                field = fieldMapping.get(field);
            }

            // 白名单校验
            if (allowedFields != null && !allowedFields.isEmpty()) {
                String checkField = field;
                // 支持嵌套字段，只校验第一层
                if (checkField.contains(".")) {
                    checkField = checkField.substring(0, checkField.indexOf('.'));
                }
                if (!allowedFields.contains(checkField)) {
                    continue; // 跳过不在白名单中的字段
                }
            }

            Sort.Direction direction = parseDirection(directionStr);
            orders.add(new Sort.Order(direction, field));
        }

        return orders.isEmpty() ? DEFAULT_SORT : Sort.by(orders);
    }

    /**
     * 解析排序字符串（无字段映射、无白名单限制）。
     *
     * @param sortParam 排序参数字符串
     * @return Spring Data JPA {@link Sort} 对象
     */
    public static Sort parseSort(String sortParam) {
        return parseSort(sortParam, null, null);
    }

    /**
     * 解析排序字符串（带字段白名单）。
     *
     * @param sortParam     排序参数字符串
     * @param allowedFields 允许的字段白名单
     * @return Spring Data JPA {@link Sort} 对象
     */
    public static Sort parseSort(String sortParam, Set<String> allowedFields) {
        return parseSort(sortParam, null, allowedFields);
    }

    /**
     * 解析排序字符串（带字段映射）。
     *
     * @param sortParam    排序参数字符串
     * @param fieldMapping 字段映射
     * @return Spring Data JPA {@link Sort} 对象
     */
    public static Sort parseSort(String sortParam, Map<String, String> fieldMapping) {
        return parseSort(sortParam, fieldMapping, null);
    }

    /**
     * 构建 {@link org.springframework.data.domain.Pageable} 的便捷方法。
     *
     * @param page           页码（从 0 开始）
     * @param size           每页大小
     * @param sortParam      排序参数字符串
     * @param fieldMapping   字段映射
     * @param allowedFields  字段白名单
     * @return Pageable 对象
     */
    public static org.springframework.data.domain.Pageable buildPageable(
            int page, int size,
            String sortParam,
            Map<String, String> fieldMapping,
            Set<String> allowedFields) {
        Sort sort = parseSort(sortParam, fieldMapping, allowedFields);
        return org.springframework.data.domain.PageRequest.of(page, size, sort);
    }

    private static Sort.Direction parseDirection(String directionStr) {
        if ("-".equals(directionStr) || "desc".equalsIgnoreCase(directionStr) || "descending".equalsIgnoreCase(directionStr)) {
            return Sort.Direction.DESC;
        }
        return Sort.Direction.ASC;
    }
}
