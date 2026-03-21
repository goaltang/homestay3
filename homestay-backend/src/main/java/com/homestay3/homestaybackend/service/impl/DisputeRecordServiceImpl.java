package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.DisputeRecordDTO;
import com.homestay3.homestaybackend.entity.DisputeRecord;
import com.homestay3.homestaybackend.entity.Order;
import com.homestay3.homestaybackend.entity.User;
import com.homestay3.homestaybackend.repository.DisputeRecordRepository;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import com.homestay3.homestaybackend.service.DisputeRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 争议记录服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DisputeRecordServiceImpl implements DisputeRecordService {

    private final DisputeRecordRepository disputeRecordRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public DisputeRecord createDisputeRecord(Long orderId, String reason, Long raisedBy) {
        log.info("创建争议记录，订单ID: {}, 原因: {}, 发起人: {}", orderId, reason, raisedBy);

        // 检查是否已有未解决的争议记录
        Optional<DisputeRecord> existingRecord = disputeRecordRepository
                .findFirstByOrderIdOrderByCreatedAtDesc(orderId);

        if (existingRecord.isPresent() && existingRecord.get().getResolution() == null) {
            // 已有未解决的争议，不创建新记录
            log.warn("订单 {} 已有未解决的争议记录，不重复创建", orderId);
            return existingRecord.get();
        }

        DisputeRecord record = DisputeRecord.builder()
                .orderId(orderId)
                .disputeReason(reason)
                .raisedBy(raisedBy)
                .raisedAt(LocalDateTime.now())
                .build();

        DisputeRecord saved = disputeRecordRepository.save(record);
        log.info("争议记录创建成功，ID: {}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public DisputeRecord resolveDisputeRecord(Long orderId, String resolution, Long resolvedBy, String note) {
        log.info("更新争议记录，订单ID: {}, 仲裁结果: {}, 仲裁人: {}", orderId, resolution, resolvedBy);

        DisputeRecord record = disputeRecordRepository
                .findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new RuntimeException("未找到争议记录，订单ID: " + orderId));

        record.setResolution(resolution);
        record.setResolvedBy(resolvedBy);
        record.setResolvedAt(LocalDateTime.now());
        record.setResolutionNote(note);

        DisputeRecord saved = disputeRecordRepository.save(record);
        log.info("争议记录更新成功，ID: {}, 仲裁结果: {}", saved.getId(), resolution);
        return saved;
    }

    @Override
    public DisputeRecordDTO getDisputeRecordByOrderId(Long orderId) {
        Optional<DisputeRecord> recordOpt = disputeRecordRepository
                .findFirstByOrderIdOrderByCreatedAtDesc(orderId);

        if (recordOpt.isEmpty()) {
            return null;
        }

        return convertToDTO(recordOpt.get(), null);
    }

    @Override
    public Page<DisputeRecordDTO> getAllDisputeRecords(Pageable pageable) {
        return disputeRecordRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(record -> convertToDTO(record, null));
    }

    @Override
    public Page<DisputeRecordDTO> getDisputeRecordsByUser(Long userId, Pageable pageable) {
        return disputeRecordRepository.findByRaisedByOrderByCreatedAtDesc(userId, pageable)
                .map(record -> convertToDTO(record, null));
    }

    private DisputeRecordDTO convertToDTO(DisputeRecord record, Order order) {
        if (order == null) {
            order = orderRepository.findById(record.getOrderId()).orElse(null);
        }

        DisputeRecordDTO.DisputeRecordDTOBuilder builder = DisputeRecordDTO.builder()
                .id(record.getId())
                .orderId(record.getOrderId())
                .disputeReason(record.getDisputeReason())
                .raisedBy(record.getRaisedBy())
                .raisedAt(record.getRaisedAt())
                .resolution(record.getResolution())
                .resolvedBy(record.getResolvedBy())
                .resolvedAt(record.getResolvedAt())
                .resolutionNote(record.getResolutionNote())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt());

        // 转换仲裁结果文本
        if ("APPROVED".equals(record.getResolution())) {
            builder.resolutionText("批准退款");
        } else if ("REJECTED".equals(record.getResolution())) {
            builder.resolutionText("拒绝退款");
        }

        // 获取用户名
        if (record.getRaisedBy() != null) {
            userRepository.findById(record.getRaisedBy())
                    .ifPresent(user -> builder.raisedByUsername(user.getUsername()));
        }
        if (record.getResolvedBy() != null) {
            userRepository.findById(record.getResolvedBy())
                    .ifPresent(user -> builder.resolvedByUsername(user.getUsername()));
        }

        // 获取订单相关信息
        if (order != null) {
            builder.orderNumber(order.getOrderNumber())
                    .refundAmount(order.getRefundAmount() != null ? order.getRefundAmount().doubleValue() : null)
                    .refundReason(order.getRefundReason());

            if (order.getHomestay() != null) {
                builder.homestayTitle(order.getHomestay().getTitle());
            }
            if (order.getGuest() != null) {
                builder.guestUsername(order.getGuest().getUsername());
            }
        }

        return builder.build();
    }
}
