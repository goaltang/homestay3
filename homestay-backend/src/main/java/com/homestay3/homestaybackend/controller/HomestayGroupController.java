package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.HomestayGroupDTO;
import com.homestay3.homestaybackend.dto.HomestayGroupRequest;
import com.homestay3.homestaybackend.dto.HomestayDTO;
import com.homestay3.homestaybackend.service.HomestayGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/host/groups")
@RequiredArgsConstructor
@Slf4j
public class HomestayGroupController {

    private final HomestayGroupService groupService;

    @GetMapping
    @PreAuthorize("hasAnyRole('LANDLORD', 'HOST')")
    public ResponseEntity<List<HomestayGroupDTO>> getMyGroups(Authentication authentication) {
        String username = authentication.getName();
        log.info("获取房东 {} 的房源分组列表", username);
        List<HomestayGroupDTO> groups = groupService.getGroupsByOwner(username);
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/{groupId}")
    @PreAuthorize("hasAnyRole('LANDLORD', 'HOST')")
    public ResponseEntity<HomestayGroupDTO> getGroup(
            @PathVariable Long groupId,
            Authentication authentication) {
        String username = authentication.getName();
        log.info("获取房源分组详情: groupId={}, owner={}", groupId, username);
        return ResponseEntity.ok(groupService.getGroupById(groupId, username));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LANDLORD', 'HOST')")
    public ResponseEntity<?> createGroup(
            @Valid @RequestBody HomestayGroupRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        log.info("创建房源分组: name={}, owner={}", request.getName(), username);
        try {
            HomestayGroupDTO created = groupService.createGroup(request, username);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{groupId}")
    @PreAuthorize("hasAnyRole('LANDLORD', 'HOST')")
    public ResponseEntity<?> updateGroup(
            @PathVariable Long groupId,
            @Valid @RequestBody HomestayGroupRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        log.info("更新房源分组: groupId={}, owner={}", groupId, username);
        try {
            HomestayGroupDTO updated = groupService.updateGroup(groupId, request, username);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "更新分组失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{groupId}")
    @PreAuthorize("hasAnyRole('LANDLORD', 'HOST')")
    public ResponseEntity<?> deleteGroup(
            @PathVariable Long groupId,
            Authentication authentication) {
        String username = authentication.getName();
        log.info("删除房源分组: groupId={}, owner={}", groupId, username);
        try {
            groupService.deleteGroup(groupId, username);
            return ResponseEntity.ok(Map.of("message", "删除分组成功"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{groupId}/assign")
    @PreAuthorize("hasAnyRole('LANDLORD', 'HOST')")
    public ResponseEntity<?> assignHomestays(
            @PathVariable Long groupId,
            @RequestBody Map<String, List<Long>> request,
            Authentication authentication) {
        String username = authentication.getName();
        List<Long> homestayIds = request.get("homestayIds");
        log.info("批量分配房源到分组: groupId={}, count={}, owner={}", groupId, homestayIds != null ? homestayIds.size() : 0, username);
        try {
            groupService.assignHomestaysToGroup(groupId, homestayIds, username);
            return ResponseEntity.ok(Map.of("message", "分配房源成功"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/remove")
    @PreAuthorize("hasAnyRole('LANDLORD', 'HOST')")
    public ResponseEntity<?> removeHomestays(
            @RequestBody Map<String, List<Long>> request,
            Authentication authentication) {
        String username = authentication.getName();
        List<Long> homestayIds = request.get("homestayIds");
        log.info("批量移除房源分组: count={}, owner={}", homestayIds != null ? homestayIds.size() : 0, username);
        try {
            groupService.removeHomestaysFromGroup(homestayIds, username);
            return ResponseEntity.ok(Map.of("message", "移除房源分组成功"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{groupId}/homestays")
    @PreAuthorize("hasAnyRole('LANDLORD', 'HOST')")
    public ResponseEntity<Page<HomestayDTO>> getGroupHomestays(
            @PathVariable Long groupId,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String username = authentication.getName();
        log.info("获取分组房源: groupId={}, owner={}", groupId, username);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(groupService.getHomestaysByGroup(groupId, username, pageable));
    }
}
