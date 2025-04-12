package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.OrderDTO;
import com.homestay3.homestaybackend.exception.ResourceNotFoundException;
import com.homestay3.homestaybackend.model.Order;
import com.homestay3.homestaybackend.model.OrderStatus;
import com.homestay3.homestaybackend.repository.OrderRepository;
import com.homestay3.homestaybackend.service.OrderService;
import com.homestay3.homestaybackend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Override
    public String generatePaymentQRCode(Long orderId, String method) {
        // 查询订单
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));

        // 检查订单状态
        // 目前只要订单未完成或未取消都可以支付
        if (OrderStatus.COMPLETED.name().equals(order.getStatus()) || 
            OrderStatus.CANCELLED.name().equals(order.getStatus())) {
            throw new IllegalArgumentException("订单状态不正确，当前状态：" + order.getStatus());
        }

        // 生成支付ID
        String paymentId = UUID.randomUUID().toString();

        // 生成模拟二维码URL
        String qrCodeUrl;
        if ("wechat".equals(method)) {
            qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?data=wechat:pay:" + orderId + ":" + paymentId + "&size=200x200";
        } else if ("alipay".equals(method)) {
            qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?data=alipay:pay:" + orderId + ":" + paymentId + "&size=200x200";
        } else {
            qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?data=unknown:pay:" + orderId + ":" + paymentId + "&size=200x200";
        }

        return qrCodeUrl;
    }

    @Override
    public boolean checkPaymentStatus(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
        
        return OrderStatus.PAID.name().equals(order.getStatus());
    }

    @Override
    @Transactional
    public OrderDTO mockSuccessPayment(Long orderId) {
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
} 