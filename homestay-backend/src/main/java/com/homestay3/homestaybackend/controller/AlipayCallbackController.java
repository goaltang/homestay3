package com.homestay3.homestaybackend.controller;

import com.homestay3.homestaybackend.dto.payment.PaymentNotifyResult;
import com.homestay3.homestaybackend.service.PaymentService;
import com.homestay3.homestaybackend.service.gateway.AlipayGateway;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝回调处理控制器
 */
@RestController
@RequestMapping("/api/payment/alipay")
@RequiredArgsConstructor
@Slf4j
public class AlipayCallbackController {

    private final AlipayGateway alipayGateway;
    private final PaymentService paymentService;

    /**
     * 支付宝异步通知处理
     */
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        try {
            log.info("收到支付宝异步通知");

            // 获取回调参数
            Map<String, String> params = new HashMap<>();
            Map<String, String[]> requestParams = request.getParameterMap();
            for (String name : requestParams.keySet()) {
                String[] values = requestParams.get(name);
                if (values != null && values.length > 0) {
                    params.put(name, values[0]);
                }
            }

            log.info("支付宝回调参数: out_trade_no={}, trade_status={}, total_amount={}, trade_no={}",
                    params.get("out_trade_no"), params.get("trade_status"),
                    params.get("total_amount"), params.get("trade_no"));

            // 验证签名
            if (!alipayGateway.verifyNotify(params)) {
                log.error("支付宝回调签名验证失败");
                return "fail";
            }

            // 处理回调
            PaymentNotifyResult result = alipayGateway.handleNotify(params);
            if (result.isSuccess()) {
                // 更新订单状态
                paymentService.handlePaymentNotify(result);
                log.info("支付宝回调处理成功");
                return "success";
            } else {
                log.error("处理支付宝回调失败: {}", result.getMessage());
                return "fail";
            }

        } catch (Exception e) {
            log.error("处理支付宝回调异常", e);
            return "fail";
        }
    }
}