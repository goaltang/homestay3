package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.CheckInCredentialDTO;
import com.homestay3.homestaybackend.dto.CheckInDTO;
import com.homestay3.homestaybackend.dto.CheckOutDTO;
import com.homestay3.homestaybackend.exception.AccessDeniedException;
import com.homestay3.homestaybackend.service.CheckInService;
import com.homestay3.homestaybackend.service.CheckOutService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 入住相关 API 控制器
 * 提供入住办理、入住凭证、自助入住等功能
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CheckInController {

    private static final Logger log = LoggerFactory.getLogger(CheckInController.class);

    private final CheckInService checkInService;
    private final CheckOutService checkOutService;

    /**
     * 房东设置准备入住（生成入住凭证）
     * 将订单状态从 PAID 转为 READY_FOR_CHECKIN
     */
    @PutMapping("/{id}/prepare-checkin")
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD')")
    public ResponseEntity<?> prepareCheckIn(
            @PathVariable Long id,
            @RequestBody CheckInCredentialDTO credentialDTO,
            Authentication authentication) {
        try {
            log.info("设置准备入住 - 订单ID: {}, 用户: {}", id, authentication.getName());
            CheckInDTO result = checkInService.prepareCheckIn(id, credentialDTO);
            return ResponseEntity.ok(result);
        } catch (AccessDeniedException e) {
            log.warn("设置准备入住权限不足: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("设置准备入住参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("设置准备入住失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "设置准备入住失败: " + e.getMessage()));
        }
    }

    /**
     * 获取入住凭证
     * 房东和客人都可以查看
     */
    @GetMapping("/{id}/checkin-credential")
    public ResponseEntity<?> getCheckInCredential(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            log.debug("获取入住凭证 - 订单ID: {}, 用户: {}", id, authentication.getName());
            // 权限校验：只允许房东或订单客人查看
            checkInService.validateAccess(id, authentication.getName());
            CheckInCredentialDTO credential = checkInService.getCheckInCredential(id);
            return ResponseEntity.ok(credential);
        } catch (AccessDeniedException e) {
            log.warn("获取入住凭证权限不足: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("获取入住凭证失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 办理入住（房东/管理员手动操作）
     * 将订单状态从 READY_FOR_CHECKIN 转为 CHECKED_IN
     */
    @PutMapping("/{id}/check-in")
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD', 'ADMIN')")
    public ResponseEntity<?> performCheckIn(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            log.info("办理入住 - 订单ID: {}, 用户: {}", id, authentication.getName());
            CheckInDTO result = checkInService.performCheckIn(id);
            return ResponseEntity.ok(result);
        } catch (AccessDeniedException e) {
            log.warn("办理入住权限不足: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("办理入住参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("办理入住失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "办理入住失败: " + e.getMessage()));
        }
    }

    /**
     * 自助入住（输入入住码）
     * 公开接口，通过入住码验证
     */
    @PostMapping("/checkin/self")
    public ResponseEntity<?> selfCheckIn(@RequestBody Map<String, String> request) {
        try {
            String checkInCode = request.get("checkInCode");
            if (checkInCode == null || checkInCode.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "入住码不能为空"));
            }
            log.info("自助入住 - 入住码: {}", checkInCode);
            CheckInDTO result = checkInService.selfCheckIn(checkInCode);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("自助入住参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("自助入住失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "自助入住失败: " + e.getMessage()));
        }
    }

    /**
     * 客人确认到达
     */
    @PostMapping("/{id}/confirm-arrival")
    public ResponseEntity<?> confirmArrival(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            log.info("确认到达 - 订单ID: {}, 用户: {}", id, authentication.getName());
            checkInService.validateAccess(id, authentication.getName());
            CheckInDTO result = checkInService.confirmArrival(id);
            return ResponseEntity.ok(result);
        } catch (AccessDeniedException e) {
            log.warn("确认到达权限不足: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("确认到达失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "确认到达失败: " + e.getMessage()));
        }
    }

    /**
     * 取消准备入住
     * 将订单状态从 READY_FOR_CHECKIN 退回 PAID
     */
    @PutMapping("/{id}/cancel-prepare")
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD')")
    public ResponseEntity<?> cancelPreparation(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            log.info("取消准备入住 - 订单ID: {}, 用户: {}", id, authentication.getName());
            CheckInDTO result = checkInService.cancelPreparation(id);
            return ResponseEntity.ok(result);
        } catch (AccessDeniedException e) {
            log.warn("取消准备入住权限不足: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("取消准备入住参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("取消准备入住失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "取消准备入住失败: " + e.getMessage()));
        }
    }

    /**
     * 获取入住记录
     */
    @GetMapping("/{id}/checkin-record")
    public ResponseEntity<?> getCheckInRecord(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            log.debug("获取入住记录 - 订单ID: {}, 用户: {}", id, authentication.getName());
            checkInService.validateAccess(id, authentication.getName());
            CheckInDTO result = checkInService.getCheckInRecord(id);
            return ResponseEntity.ok(result);
        } catch (AccessDeniedException e) {
            log.warn("获取入住记录权限不足: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("获取入住记录失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== 退房相关接口 ====================

    /**
     * 办理退房
     * 将订单状态从 CHECKED_IN 转为 CHECKED_OUT
     */
    @PutMapping("/{id}/check-out")
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD', 'ADMIN')")
    public ResponseEntity<?> performCheckOut(
            @PathVariable Long id,
            @RequestBody(required = false) CheckOutDTO checkOutDTO,
            Authentication authentication) {
        try {
            log.info("办理退房 - 订单ID: {}, 用户: {}", id, authentication.getName());
            CheckOutDTO result = checkOutService.performCheckOut(id, checkOutDTO);
            return ResponseEntity.ok(result);
        } catch (AccessDeniedException e) {
            log.warn("办理退房权限不足: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("办理退房参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("办理退房失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "办理退房失败: " + e.getMessage()));
        }
    }

    /**
     * 自助退房
     */
    @PostMapping("/{id}/checkout/self")
    public ResponseEntity<?> selfCheckOut(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            log.info("自助退房 - 订单ID: {}, 用户: {}", id, authentication.getName());
            CheckOutDTO result = checkOutService.selfCheckOut(id);
            return ResponseEntity.ok(result);
        } catch (AccessDeniedException e) {
            log.warn("自助退房权限不足: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("自助退房参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("自助退房失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "自助退房失败: " + e.getMessage()));
        }
    }

    /**
     * 押金操作
     */
    @PostMapping("/{id}/deposit")
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD')")
    public ResponseEntity<?> processDeposit(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            String action = (String) request.get("action");
            BigDecimal amount = request.get("amount") != null
                    ? new BigDecimal(request.get("amount").toString()) : null;
            String note = (String) request.get("note");

            log.info("押金操作 - 订单ID: {}, 操作: {}, 金额: {}, 用户: {}", id, action, amount, authentication.getName());
            CheckOutDTO result = checkOutService.processDeposit(id, action, amount, note);
            return ResponseEntity.ok(result);
        } catch (AccessDeniedException e) {
            log.warn("押金操作权限不足: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("押金操作参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("押金操作失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "押金操作失败: " + e.getMessage()));
        }
    }

    /**
     * 获取退房记录
     */
    @GetMapping("/{id}/checkout-record")
    public ResponseEntity<?> getCheckOutRecord(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            log.debug("获取退房记录 - 订单ID: {}, 用户: {}", id, authentication.getName());
            checkOutService.validateAccess(id, authentication.getName());
            CheckOutDTO result = checkOutService.getCheckOutRecord(id);
            return ResponseEntity.ok(result);
        } catch (AccessDeniedException e) {
            log.warn("获取退房记录权限不足: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("获取退房记录失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 确认结算
     * 将订单状态从 CHECKED_OUT 转为 COMPLETED
     */
    @PutMapping("/{id}/checkout/settle")
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD', 'ADMIN')")
    public ResponseEntity<?> confirmSettlement(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            log.info("确认结算 - 订单ID: {}, 用户: {}", id, authentication.getName());
            CheckOutDTO result = checkOutService.confirmSettlement(id);
            return ResponseEntity.ok(result);
        } catch (AccessDeniedException e) {
            log.warn("确认结算权限不足: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("确认结算参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("确认结算失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "确认结算失败: " + e.getMessage()));
        }
    }

    /**
     * 更新额外费用
     */
    @PutMapping("/{id}/extra-charges")
    @PreAuthorize("hasAnyRole('HOST', 'LANDLORD')")
    public ResponseEntity<?> updateExtraCharges(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            BigDecimal extraCharges = request.get("extraCharges") != null
                    ? new BigDecimal(request.get("extraCharges").toString()) : null;
            String description = (String) request.get("description");

            log.info("更新额外费用 - 订单ID: {}, 费用: {}, 用户: {}", id, extraCharges, authentication.getName());
            CheckOutDTO result = checkOutService.updateExtraCharges(id, extraCharges, description);
            return ResponseEntity.ok(result);
        } catch (AccessDeniedException e) {
            log.warn("更新额外费用权限不足: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("更新额外费用参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("更新额外费用失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "更新额外费用失败: " + e.getMessage()));
        }
    }
}
