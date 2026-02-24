package com.homestay3.homestaybackend.mapper;

import com.homestay3.homestaybackend.dto.AmenityDTO;
import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.entity.Amenity;
import com.homestay3.homestaybackend.entity.Homestay;
import com.homestay3.homestaybackend.model.HomestayStatus;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 房源实体与 DTO 之间的映射器
 *
 * 使用 MapStruct 自动生成转换代码，替代手动编写的映射逻辑
 * 
 * @Mapper(componentModel = "spring") - 让 Spring 自动管理这个 Mapper
 */
@Mapper(componentModel = "spring")
public interface HomestayMapper {

    // 获取 Mapper 实例（用于测试或非 Spring 环境）
    HomestayMapper INSTANCE = Mappers.getMapper(HomestayMapper.class);

    // ===================== Entity -> DTO =====================

    /**
     * Homestay Entity -> HomestayDTO
     * 
     * 复杂字段映射说明：
     * - price: BigDecimal -> String (自定义方法 bigDecimalToString)
     * - status: HomestayStatus 枚举 -> String (枚举 name())
     * - owner.*: 嵌套 User 对象拆平为 ownerId/ownerUsername/ownerName/ownerAvatar
     * - amenities: Set<Amenity> -> List<AmenityDTO> (自定义方法 amenitySetToList)
     */
    @Mapping(target = "price", expression = "java(bigDecimalToString(homestay.getPrice()))")
    @Mapping(target = "status", expression = "java(statusToString(homestay.getStatus()))")
    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerUsername", source = "owner.username")
    @Mapping(target = "ownerName", expression = "java(resolveOwnerName(homestay))")
    @Mapping(target = "ownerAvatar", source = "owner.avatar")
    @Mapping(target = "amenities", expression = "java(amenitySetToList(homestay.getAmenities()))")
    // 以下 DTO 字段在 Entity 中不存在，忽略
    @Mapping(target = "propertyTypeName", ignore = true)
    @Mapping(target = "suggestedFeatures", ignore = true)
    @Mapping(target = "ownerRating", ignore = true)
    @Mapping(target = "ownerPhone", ignore = true)
    @Mapping(target = "ownerEmail", ignore = true)
    @Mapping(target = "ownerRealName", ignore = true)
    @Mapping(target = "ownerNickname", ignore = true)
    @Mapping(target = "ownerOccupation", ignore = true)
    @Mapping(target = "ownerIntroduction", ignore = true)
    @Mapping(target = "ownerJoinDate", ignore = true)
    @Mapping(target = "ownerHostSince", ignore = true)
    @Mapping(target = "ownerHomestayCount", ignore = true)
    @Mapping(target = "ownerHostRating", ignore = true)
    HomestayDTO toDTO(Homestay homestay);

    // ===================== DTO -> Entity =====================

    /**
     * HomestayDTO -> Homestay Entity
     * 
     * 注意：
     * - owner: 不从 DTO 反向映射（由 Service 层单独设置）
     * - amenities: 不从 DTO 反向映射（由 Service 层单独设置）
     * - id: 保留映射（用于更新场景）
     */
    @Mapping(target = "price", expression = "java(stringToBigDecimal(dto.getPrice()))")
    @Mapping(target = "status", expression = "java(stringToStatus(dto.getStatus()))")
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "amenities", ignore = true)
    @Mapping(target = "autoConfirm", ignore = true)
    Homestay toEntity(HomestayDTO dto);

    // ===================== Amenity 映射 =====================

    /**
     * Amenity Entity -> AmenityDTO
     * 嵌套 category 对象拆平为 categoryCode/categoryName/categoryIcon
     */
    @Mapping(target = "categoryCode", source = "category.code")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "categoryIcon", source = "category.icon")
    @Mapping(target = "active", source = "active")
    AmenityDTO amenityToDTO(Amenity amenity);

    // ===================== 自定义转换方法 =====================

    /**
     * BigDecimal -> String，null 安全
     */
    default String bigDecimalToString(BigDecimal value) {
        return value != null ? value.toString() : null;
    }

    /**
     * String -> BigDecimal，带异常保护（格式错误时返回 ZERO）
     */
    default BigDecimal stringToBigDecimal(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * HomestayStatus 枚举 -> String
     */
    default String statusToString(HomestayStatus status) {
        return status != null ? status.name() : null;
    }

    /**
     * String -> HomestayStatus 枚举，无效值时默认为 DRAFT
     */
    default HomestayStatus stringToStatus(String status) {
        if (status == null || status.isEmpty()) {
            return HomestayStatus.DRAFT;
        }
        try {
            return HomestayStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return HomestayStatus.DRAFT;
        }
    }

    /**
     * 解析房东显示名称：优先 realName，其次 username
     */
    default String resolveOwnerName(Homestay homestay) {
        if (homestay.getOwner() == null) {
            return null;
        }
        String realName = homestay.getOwner().getRealName();
        return (realName != null && !realName.isEmpty())
                ? realName
                : homestay.getOwner().getUsername();
    }

    /**
     * Set<Amenity> -> List<AmenityDTO>，null 安全
     */
    default List<AmenityDTO> amenitySetToList(Set<Amenity> amenities) {
        if (amenities == null || amenities.isEmpty()) {
            return new ArrayList<>();
        }
        return amenities.stream()
                .map(this::amenityToDTO)
                .collect(Collectors.toList());
    }
}