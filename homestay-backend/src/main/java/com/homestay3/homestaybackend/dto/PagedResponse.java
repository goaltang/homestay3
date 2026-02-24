package com.homestay3.homestaybackend.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 可序列化的分页响应DTO
 * 用于Redis缓存，避免PageImpl序列化问题
 */
@Data
public class PagedResponse<T> {
    
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean empty;
    
    public PagedResponse() {
        // 默认构造函数用于反序列化
    }
    
    @JsonCreator
    public PagedResponse(
            @JsonProperty("content") List<T> content,
            @JsonProperty("page") int page,
            @JsonProperty("size") int size,
            @JsonProperty("totalElements") long totalElements,
            @JsonProperty("totalPages") int totalPages,
            @JsonProperty("first") boolean first,
            @JsonProperty("last") boolean last,
            @JsonProperty("empty") boolean empty) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = first;
        this.last = last;
        this.empty = empty;
    }
    
    /**
     * 便捷构造函数，用于创建分页响应
     */
    public PagedResponse(List<T> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = (int) Math.ceil((double) totalElements / size);
        this.first = page == 0;
        this.last = page >= totalPages - 1;
        this.empty = content == null || content.isEmpty();
    }
    
    /**
     * 从Spring Data的Page对象创建PagedResponse
     */
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast(),
            page.isEmpty()
        );
    }
    
    /**
     * 转换为Spring Data的Page对象（如果需要）
     */
    public Page<T> toPage(Pageable pageable) {
        return new org.springframework.data.domain.PageImpl<>(content, pageable, totalElements);
    }
} 