package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.DisputeRecordDTO;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.DisputeRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 房东端争议记录控制器
 */
@RestController
@RequestMapping("/api/host/disputes")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class HostDisputeController {

    private final DisputeRecordService disputeRecordService;
    private final UserRepository userRepository;

    /**
     * 分页获取当前房东发起的争议记录
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getMyDisputeRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "用户未登录"
            ));
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<DisputeRecordDTO> disputePage = disputeRecordService.getDisputeRecordsByUser(currentUser.getId(), pageRequest);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", disputePage.getContent());
        result.put("total", disputePage.getTotalElements());
        result.put("totalPages", disputePage.getTotalPages());
        result.put("currentPage", disputePage.getNumber());

        return ResponseEntity.ok(result);
    }

    /**
     * 获取当前房东发起的争议记录详情
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getMyDisputeRecordByOrderId(@PathVariable Long orderId) {
        DisputeRecordDTO dispute = disputeRecordService.getDisputeRecordByOrderId(orderId);

        if (dispute != null) {
            // 验证该争议是否属于当前房东
            User currentUser = getCurrentUser();
            if (currentUser != null && dispute.getRaisedBy() != null
                    && dispute.getRaisedBy().equals(currentUser.getId())) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "data", dispute
                ));
            } else if (dispute.getRaisedBy() == null) {
                // 如果 raisedBy 为空（数据问题），允许查看
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "data", dispute
                ));
            }
        }

        return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "未找到争议记录或无权查看"
        ));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username).orElse(null);
    }
}
