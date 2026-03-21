package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.DisputeRecordDTO;
import com.homestay3.homestaybackend.service.DisputeRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理员端争议记录控制器
 */
@RestController
@RequestMapping("/api/admin/disputes")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class AdminDisputeController {

    private final DisputeRecordService disputeRecordService;

    /**
     * 分页获取所有争议记录
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getDisputeRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long orderId) {

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<DisputeRecordDTO> disputePage;

        if (orderId != null) {
            // 如果指定了订单ID，返回该订单的争议记录
            DisputeRecordDTO dispute = disputeRecordService.getDisputeRecordByOrderId(orderId);
            if (dispute != null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "data", dispute
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "data", Map.of(),
                        "message", "未找到争议记录"
                ));
            }
        } else {
            disputePage = disputeRecordService.getAllDisputeRecords(pageRequest);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", disputePage.getContent());
        result.put("total", disputePage.getTotalElements());
        result.put("totalPages", disputePage.getTotalPages());
        result.put("currentPage", disputePage.getNumber());

        return ResponseEntity.ok(result);
    }

    /**
     * 获取争议记录详情
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getDisputeRecordByOrderId(@PathVariable Long orderId) {
        DisputeRecordDTO dispute = disputeRecordService.getDisputeRecordByOrderId(orderId);

        if (dispute != null) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", dispute
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "未找到争议记录"
            ));
        }
    }
}
