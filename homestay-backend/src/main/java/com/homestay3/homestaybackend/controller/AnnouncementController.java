package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.entity.Announcement;
import com.homestay3.homestaybackend.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    /**
     * 获取已发布的公告列表（用户端）
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getPublishedAnnouncements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category) {

        Sort sort = Sort.by(Sort.Direction.DESC, "priority")
                       .and(Sort.by(Sort.Direction.DESC, "publishedAt"));
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<Announcement> announcements = announcementService.getPublishedAnnouncements(category, pageRequest);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", announcements.getContent(),
            "total", announcements.getTotalElements(),
            "totalPages", announcements.getTotalPages(),
            "currentPage", announcements.getNumber()
        ));
    }

    /**
     * 获取已发布公告详情（用户端）
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getAnnouncement(@PathVariable Long id) {
        try {
            Announcement announcement = announcementService.getPublishedById(id);
            return ResponseEntity.ok(Map.of("success", true, "data", announcement));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", "公告不存在或已下线"));
        }
    }
}
