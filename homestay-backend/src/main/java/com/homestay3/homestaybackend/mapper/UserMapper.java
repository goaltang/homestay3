package com.homestay3.homestaybackend.mapper;

import com.homestay3.homestaybackend.dto.UserDTO;
import com.homestay3.homestaybackend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * User 实体与 DTO 之间的映射器
 * 
 * 使用 MapStruct 自动生成转换代码，替代手动编写的 UserDTO(User user) 构造函数
 * 
 * @Mapper(componentModel = "spring") - 让 Spring 自动管理这个 Mapper
 */
@Mapper(componentModel = "spring")
public interface UserMapper {
    
    // 获取 Mapper 实例（用于测试或非 Spring 环境）
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);
    
    /**
     * Entity -> DTO 转换
     * 
     * 注意：这里演示了字段类型不匹配时的处理方式
     * - languages: Entity 是 String (逗号分隔)，DTO 是 List<String>
     * - companions: Entity 是 String (JSON)，DTO 是 List<Map>
     * 
     * 使用 @Mapping(target = "...", ignore = true) 忽略复杂转换
     * 或者使用表达式进行自定义转换
     */
    @Mapping(target = "languages", expression = "java(stringToList(user.getLanguages()))")
    @Mapping(target = "companions", ignore = true)  // 暂时忽略复杂字段
    UserDTO toDTO(User user);
    
    /**
     * DTO -> Entity 转换
     * 
     * 注意：
     * - password 字段在 DTO 中不存在，需要忽略
     * - resetToken 等敏感字段也忽略
     * - id 在创建时通常由数据库生成，这里也忽略
     */
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "resetToken", ignore = true)
    @Mapping(target = "resetTokenExpiry", ignore = true)
    @Mapping(target = "homestays", ignore = true)
    @Mapping(target = "gender", ignore = true)
    @Mapping(target = "birthday", ignore = true)
    @Mapping(target = "idCardFront", ignore = true)
    @Mapping(target = "idCardBack", ignore = true)
    @Mapping(target = "languages", expression = "java(listToString(dto.getLanguages()))")
    @Mapping(target = "companions", ignore = true)
    User toEntity(UserDTO dto);
    
    // 自定义转换方法：String -> List<String>
    default List<String> stringToList(String value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(value.split(","));
    }
    
    // 自定义转换方法：List<String> -> String
    default String listToString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return String.join(",", list);
    }
}
