package com.homestay3.homestaybackend.service;

import com.homestay3.homestaybackend.dto.DisputeRecordDTO;
import com.homestay3.homestaybackend.entity.DisputeRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 争议记录服务接口
 */
public interface DisputeRecordService {

    /**
     * 创建争议记录（发起争议时）
     */
    DisputeRecord createDisputeRecord(Long orderId, String reason, Long raisedBy);

    /**
     * 更新争议记录（解决争议时）
     */
    DisputeRecord resolveDisputeRecord(Long orderId, String resolution, Long resolvedBy, String note);

    /**
     * 根据订单ID获取争议记录
     */
    DisputeRecordDTO getDisputeRecordByOrderId(Long orderId);

    /**
     * 分页获取所有争议记录（管理员）
     */
    Page<DisputeRecordDTO> getAllDisputeRecords(Pageable pageable);

    /**
     * 分页获取用户发起的争议记录
     */
    Page<DisputeRecordDTO> getDisputeRecordsByUser(Long userId, Pageable pageable);
}
