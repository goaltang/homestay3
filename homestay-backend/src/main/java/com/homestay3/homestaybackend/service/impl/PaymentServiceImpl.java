package com.homestay3.homestaybackend.service.impl;

import com.alibaba.fastjson2.JSON;
import com.homestay3.homestaybackend.dto.OrderDTO;
import com.homestay3.homestaybackend.dto.payment.PaymentNotifyResult;
import com.homestay3.homestaybackend.dto.payment.PaymentRequest;
import com.homestay3.homestaybackend.dto.payment.PaymentResponse;
import com.homestay3.homestaybackend.dto.payment.PaymentStatusResponse;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.entity.Order;
import com.homestay3.homestaybackend.entity.PaymentRecord;
import com.homestay3.homestaybackend.model.OrderStatus;
import com.homestay3.homestaybackend.model.PaymentStatus;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.repository.PaymentRecordRepository;
import com.homestay3.homestaybackend.service.OrderService;
import com.homestay3.homestaybackend.service.PaymentService;
import com.homestay3.homestaybackend.service.gateway.AlipayGateway;
import com.homestay3.homestaybackend.service.gateway.PaymentGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final OrderService orderService;
    private final AlipayGateway alipayGateway;

    @Override
    @Transactional
    public String generatePaymentQRCode(Long orderId, String method) {
        try {
            log.info("开始生成支付，订单ID: {}, 支付方式: {}", orderId, method);
            
        // 查询订单
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));

        // 检查订单状态
        if (OrderStatus.COMPLETED.name().equals(order.getStatus()) || 
            OrderStatus.CANCELLED.name().equals(order.getStatus())) {
            throw new IllegalArgumentException("订单状态不正确，当前状态：" + order.getStatus());
        }

            // 生成商户订单号
            String outTradeNo = generateOutTradeNo(orderId);

            // 创建支付请求
            PaymentRequest request = PaymentRequest.builder()
                .outTradeNo(outTradeNo)
                .amount(order.getTotalAmount())
                .subject("民宿预订-" + order.getHomestay().getTitle())
                .body("订单号：" + order.getOrderNumber())
                .orderId(orderId)
                .build();
            
            // 创建支付
            PaymentResponse response;
            
            // 临时修改：支付宝直接使用二维码支付，避免页面跳转问题
            if ("alipay".equals(method.toLowerCase())) {
                log.info("使用支付宝二维码支付（临时方案），订单ID: {}", orderId);
                response = alipayGateway.createQRCodePayment(request);
                
                // 如果二维码支付也失败，尝试页面跳转支付
                if (!response.isSuccess()) {
                    log.warn("支付宝二维码支付失败，尝试页面跳转支付，订单ID: {}, 错误: {}", orderId, response.getMessage());
                    response = alipayGateway.createPagePayment(request);
                }
            } else {
                // 选择支付网关
                PaymentGateway gateway = selectPaymentGateway(method);
                response = gateway.createPayment(request);
                log.info("调用传统二维码支付，订单ID: {}, 支付方式: {}", orderId, method);
            }
            
            if (response.isSuccess()) {
                // 保存支付记录
                savePaymentRecord(orderId, method, outTradeNo, request, response);
                
                // 更新订单状态为待支付
                order.setStatus(OrderStatus.PAYMENT_PENDING.name());
                orderRepository.save(order);
                
                // 根据支付方式返回不同的支付数据
                String paymentData = null;
                if ("alipay".equals(method)) {
                    // 优先返回二维码，如果没有再返回页面跳转URL
                    paymentData = response.getQrCode() != null ? response.getQrCode() : response.getPaymentUrl();
                    log.info("支付宝支付生成成功，订单ID: {}, 二维码: {}, 支付URL: {}", 
                        orderId, response.getQrCode() != null ? "已生成" : "无", response.getPaymentUrl() != null ? "已生成" : "无");
                } else {
                    // 其他支付方式使用二维码
                    paymentData = response.getQrCode();
                log.info("支付二维码生成成功，订单ID: {}, 二维码: {}", orderId, response.getQrCode());
                }
                
                return paymentData;
        } else {
                log.error("生成支付失败，订单ID: {}, 错误: {}", orderId, response.getMessage());
                throw new RuntimeException("生成支付失败: " + response.getMessage());
            }
            
        } catch (Exception e) {
            log.error("生成支付异常，订单ID: " + orderId, e);
            throw new RuntimeException("生成支付失败", e);
        }
    }

    @Override
    public boolean checkPaymentStatus(Long orderId) {
        try {
            log.info("检查支付状态，订单ID: {}", orderId);
            
            // 查询最新的支付记录
            PaymentRecord paymentRecord = paymentRecordRepository
                .findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElse(null);
            
            if (paymentRecord == null) {
                log.info("未找到支付记录，订单ID: {}", orderId);
                return false;
            }
            
            // 如果支付记录已经是成功状态，直接返回
            if ("SUCCESS".equals(paymentRecord.getStatus())) {
                log.info("支付记录已是成功状态，订单ID: {}", orderId);
                return true;
            }
            
            // 选择支付网关查询状态
            PaymentGateway gateway = selectPaymentGateway(paymentRecord.getPaymentMethod());
            PaymentStatusResponse response = gateway.queryPayment(paymentRecord.getOutTradeNo());
            
            if (response.isSuccess() && response.getStatus() == PaymentStatus.PAID) {
                // 更新支付记录
                paymentRecord.setStatus("SUCCESS");
                paymentRecord.setTransactionId(response.getTransactionId());
                paymentRecordRepository.save(paymentRecord);
                
                // 更新订单状态
                updateOrderPaymentStatus(orderId, PaymentStatus.PAID);
                
                log.info("支付状态查询成功，订单已支付，订单ID: {}", orderId);
                return true;
            }
            
            log.info("支付状态查询完成，订单未支付，订单ID: {}", orderId);
            return false;
            
        } catch (Exception e) {
            log.error("检查支付状态异常，订单ID: " + orderId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public OrderDTO mockSuccessPayment(Long orderId) {
        log.info("模拟支付成功，订单ID: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
        
        // 确保订单未支付
        if (OrderStatus.PAID.name().equals(order.getStatus())) {
            throw new IllegalArgumentException("订单已支付，无需重复支付");
        }
        
        if (OrderStatus.COMPLETED.name().equals(order.getStatus()) || 
            OrderStatus.CANCELLED.name().equals(order.getStatus())) {
            throw new IllegalArgumentException("订单状态不正确，无法支付");
        }
        
        // 更新订单状态为已支付
        return orderService.updateOrderStatus(orderId, OrderStatus.PAID.name());
    }
    
    /**
     * 处理支付回调通知
     */
    @Transactional
    public void handlePaymentNotify(PaymentNotifyResult result) {
        try {
            log.info("开始处理支付回调，商户订单号: {}", result.getOutTradeNo());
            
            // 查找支付记录
            PaymentRecord paymentRecord = paymentRecordRepository
                .findByOutTradeNo(result.getOutTradeNo())
                .orElseThrow(() -> new RuntimeException("支付记录不存在"));
            
            // 防重复处理
            if ("SUCCESS".equals(paymentRecord.getStatus())) {
                log.info("支付记录已处理，跳过: {}", result.getOutTradeNo());
                return;
            }
            
            // 更新支付记录
            paymentRecord.setStatus("SUCCESS");
            paymentRecord.setTransactionId(result.getTransactionId());
            paymentRecord.setNotifyParams(JSON.toJSONString(result.getParams()));
            paymentRecordRepository.save(paymentRecord);
            
            // 更新订单状态
            updateOrderPaymentStatus(paymentRecord.getOrderId(), PaymentStatus.PAID);
            
            log.info("支付回调处理成功: orderId={}, outTradeNo={}", 
                paymentRecord.getOrderId(), result.getOutTradeNo());
            
        } catch (Exception e) {
            log.error("处理支付回调异常", e);
            throw new RuntimeException("处理支付回调失败", e);
        }
    }
    
    /**
     * 选择支付网关
     */
    private PaymentGateway selectPaymentGateway(String method) {
        switch (method.toLowerCase()) {
            case "alipay":
                return alipayGateway;
            case "wechat":
                // TODO: 微信支付网关实现
                throw new IllegalArgumentException("微信支付暂未实现");
            default:
                throw new IllegalArgumentException("不支持的支付方式: " + method);
        }
    }
    
    /**
     * 生成商户订单号
     */
    private String generateOutTradeNo(Long orderId) {
        return "HST" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + 
               String.format("%06d", orderId) + String.format("%03d", (int)(Math.random() * 1000));
    }
    
    /**
     * 保存支付记录
     */
    private void savePaymentRecord(Long orderId, String method, String outTradeNo, 
                                   PaymentRequest request, PaymentResponse response) {
        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setOrderId(orderId);
        paymentRecord.setPaymentMethod(method.toUpperCase());
        paymentRecord.setOutTradeNo(outTradeNo);
        paymentRecord.setAmount(request.getAmount());
        paymentRecord.setStatus("PENDING");
        paymentRecord.setRequestParams(JSON.toJSONString(request));
        paymentRecord.setResponseParams(JSON.toJSONString(response));
        
        paymentRecordRepository.save(paymentRecord);
        log.info("支付记录保存成功，订单ID: {}, 商户订单号: {}", orderId, outTradeNo);
    }
    
    /**
     * 更新订单支付状态
     */
    private void updateOrderPaymentStatus(Long orderId, PaymentStatus paymentStatus) {
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
            
            order.setPaymentStatus(paymentStatus);
            order.setStatus(OrderStatus.PAID.name());
            orderRepository.save(order);
            
            log.info("订单支付状态更新成功，订单ID: {}, 支付状态: {}", orderId, paymentStatus);
        } catch (Exception e) {
            log.error("更新订单支付状态失败，订单ID: " + orderId, e);
            throw new RuntimeException("更新订单支付状态失败", e);
        }
    }
} 