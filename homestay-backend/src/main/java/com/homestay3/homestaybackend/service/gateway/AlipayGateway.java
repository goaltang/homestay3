package com.homestay3.homestaybackend.service.gateway;

import com.alibaba.fastjson2.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.homestay3.homestaybackend.config.PaymentConfig;
import com.homestay3.homestaybackend.dto.payment.*;
import com.homestay3.homestaybackend.model.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝支付网关实现
 */
@Component
@Slf4j
public class AlipayGateway implements PaymentGateway {
    
    private final PaymentConfig paymentConfig;
    private final AlipayClient alipayClient;
    
    public AlipayGateway(PaymentConfig paymentConfig) {
        this.paymentConfig = paymentConfig;
        
        // 使用标准构造器，不传递超时参数（SDK会使用默认值）
        this.alipayClient = new DefaultAlipayClient(
            paymentConfig.getAlipay().getGatewayUrl(),
            paymentConfig.getAlipay().getAppId(),
            paymentConfig.getAlipay().getPrivateKey(),
            "json",
            "UTF-8",
            paymentConfig.getAlipay().getPublicKey(),
            "RSA2"
        );
        
        log.info("支付宝网关初始化成功，APPID: {}, 网关URL: {}", 
            paymentConfig.getAlipay().getAppId(), 
            paymentConfig.getAlipay().getGatewayUrl());
    }
    
    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        // 优先使用页面跳转支付，体验更好
        return createPagePayment(request);
    }
    
    /**
     * 创建页面跳转支付（推荐方式）
     */
    public PaymentResponse createPagePayment(PaymentRequest request) {
        // 添加重试机制
        int maxRetries = paymentConfig.getAlipay().getMaxRetries();
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("开始创建支付宝页面支付订单，商户订单号: {}, 金额: {}, 尝试次数: {}/{}", 
                    request.getOutTradeNo(), request.getAmount(), attempt, maxRetries);
                
                // 创建支付宝页面支付请求
                AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
                alipayRequest.setNotifyUrl(paymentConfig.getAlipay().getNotifyUrl());
                alipayRequest.setReturnUrl(paymentConfig.getAlipay().getReturnUrl());
                
                // 构建请求参数
                Map<String, Object> bizContent = new HashMap<>();
                bizContent.put("out_trade_no", request.getOutTradeNo());
                bizContent.put("total_amount", request.getAmount().toString());
                bizContent.put("subject", request.getSubject());
                bizContent.put("timeout_express", "30m");  // 页面支付可以设置更长的超时时间
                bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");  // 电脑网站支付产品码
                
                if (request.getBody() != null) {
                    bizContent.put("body", request.getBody());
                }
                
                alipayRequest.setBizContent(JSON.toJSONString(bizContent));
                
                log.info("支付宝页面支付请求参数: {}", JSON.toJSONString(bizContent));
                
                // 生成支付页面表单HTML（包含自动跳转）
                String paymentForm = alipayClient.pageExecute(alipayRequest).getBody();
                
                log.info("支付宝页面支付表单生成成功，订单号: {}", request.getOutTradeNo());
                
                return PaymentResponse.builder()
                    .success(true)
                    .paymentUrl(paymentForm)  // 返回HTML表单，前端直接渲染即可跳转
                    .outTradeNo(request.getOutTradeNo())
                    .message("支付页面生成成功")
                    .build();
                    
            } catch (AlipayApiException e) {
                log.error("支付宝创建页面支付订单异常，尝试次数: {}/{}, 错误: {}", attempt, maxRetries, e.getMessage());
                
                // 如果是网络超时且还有重试次数，则继续重试
                if (attempt < maxRetries && isNetworkTimeoutError(e)) {
                    log.info("检测到网络超时，等待{}毫秒后重试...", paymentConfig.getAlipay().getRetryDelay());
                    try {
                        Thread.sleep(paymentConfig.getAlipay().getRetryDelay());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }
                
                return PaymentResponse.builder()
                    .success(false)
                    .message("创建支付订单失败: " + e.getMessage())
                    .errorCode("ALIPAY_API_ERROR")
                    .build();
            } catch (Exception e) {
                log.error("支付宝创建页面支付订单未知异常，尝试次数: {}/{}", attempt, maxRetries, e);
                
                // 如果是网络相关异常且还有重试次数，则继续重试
                if (attempt < maxRetries && isNetworkError(e)) {
                    log.info("检测到网络异常，等待{}毫秒后重试...", paymentConfig.getAlipay().getRetryDelay());
                    try {
                        Thread.sleep(paymentConfig.getAlipay().getRetryDelay());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }
                
                return PaymentResponse.builder()
                    .success(false)
                    .message("系统异常，请稍后重试")
                    .errorCode("SYSTEM_ERROR")
                    .build();
            }
        }
        
        // 所有重试都失败了
        return PaymentResponse.builder()
            .success(false)
            .message("网络连接超时，请检查网络后重试")
            .errorCode("NETWORK_TIMEOUT")
            .build();
    }
    
    /**
     * 创建扫码支付（备用方式）
     */
    public PaymentResponse createQRCodePayment(PaymentRequest request) {
        // 添加重试机制
        int maxRetries = paymentConfig.getAlipay().getMaxRetries();
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("开始创建支付宝扫码支付订单，商户订单号: {}, 金额: {}, 尝试次数: {}/{}", 
                    request.getOutTradeNo(), request.getAmount(), attempt, maxRetries);
                
                // 创建支付宝当面付请求
                AlipayTradePrecreateRequest alipayRequest = new AlipayTradePrecreateRequest();
                alipayRequest.setNotifyUrl(paymentConfig.getAlipay().getNotifyUrl());
                
                // 构建请求参数
                Map<String, Object> bizContent = new HashMap<>();
                bizContent.put("out_trade_no", request.getOutTradeNo());
                bizContent.put("total_amount", request.getAmount().toString());
                bizContent.put("subject", request.getSubject());
                bizContent.put("store_id", "homestay_store_001");
                bizContent.put("timeout_express", "10m");
                
                if (request.getBody() != null) {
                    bizContent.put("body", request.getBody());
                }
                
                alipayRequest.setBizContent(JSON.toJSONString(bizContent));
                
                log.info("支付宝扫码支付请求参数: {}", JSON.toJSONString(bizContent));
                
                // 发起请求
                AlipayTradePrecreateResponse response = alipayClient.execute(alipayRequest);
                
                if (response.isSuccess()) {
                    log.info("支付宝扫码订单创建成功，二维码: {}", response.getQrCode());
                    return PaymentResponse.builder()
                        .success(true)
                        .qrCode(response.getQrCode())
                        .outTradeNo(request.getOutTradeNo())
                        .message("二维码生成成功")
                        .build();
                } else {
                    log.error("支付宝创建扫码订单失败: code={}, msg={}, subCode={}, subMsg={}", 
                        response.getCode(), response.getMsg(), response.getSubCode(), response.getSubMsg());
                    return PaymentResponse.builder()
                        .success(false)
                        .message(response.getSubMsg() != null ? response.getSubMsg() : response.getMsg())
                        .errorCode(response.getSubCode())
                        .build();
                }
            } catch (AlipayApiException e) {
                log.error("支付宝创建扫码支付订单异常，尝试次数: {}/{}, 错误: {}", attempt, maxRetries, e.getMessage());
                
                // 如果是网络超时且还有重试次数，则继续重试
                if (attempt < maxRetries && isNetworkTimeoutError(e)) {
                    log.info("检测到网络超时，等待{}毫秒后重试...", paymentConfig.getAlipay().getRetryDelay());
                    try {
                        Thread.sleep(paymentConfig.getAlipay().getRetryDelay());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }
                
                return PaymentResponse.builder()
                    .success(false)
                    .message("创建支付订单失败: " + e.getMessage())
                    .errorCode("ALIPAY_API_ERROR")
                    .build();
            } catch (Exception e) {
                log.error("支付宝创建扫码支付订单未知异常，尝试次数: {}/{}", attempt, maxRetries, e);
                
                // 如果是网络相关异常且还有重试次数，则继续重试
                if (attempt < maxRetries && isNetworkError(e)) {
                    log.info("检测到网络异常，等待{}毫秒后重试...", paymentConfig.getAlipay().getRetryDelay());
                    try {
                        Thread.sleep(paymentConfig.getAlipay().getRetryDelay());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }
                
                return PaymentResponse.builder()
                    .success(false)
                    .message("系统异常，请稍后重试")
                    .errorCode("SYSTEM_ERROR")
                    .build();
            }
        }
        
        // 所有重试都失败了
        return PaymentResponse.builder()
            .success(false)
            .message("网络连接超时，请检查网络后重试")
            .errorCode("NETWORK_TIMEOUT")
            .build();
    }
    
    /**
     * 判断是否为网络超时错误
     */
    private boolean isNetworkTimeoutError(AlipayApiException e) {
        String message = e.getMessage().toLowerCase();
        return message.contains("timeout") || 
               message.contains("connect timed out") ||
               message.contains("read timed out") ||
               message.contains("sockettimeoutexception");
    }
    
    /**
     * 判断是否为网络相关错误
     */
    private boolean isNetworkError(Exception e) {
        String message = e.getMessage().toLowerCase();
        return message.contains("timeout") || 
               message.contains("connection") ||
               message.contains("network") ||
               message.contains("socket") ||
               e.getClass().getSimpleName().toLowerCase().contains("timeout") ||
               e.getClass().getSimpleName().toLowerCase().contains("connect");
    }
    
    @Override
    public PaymentStatusResponse queryPayment(String outTradeNo) {
        try {
            log.info("查询支付宝订单状态，商户订单号: {}", outTradeNo);
            
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", outTradeNo);
            request.setBizContent(JSON.toJSONString(bizContent));
            
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            
            if (response.isSuccess()) {
                String tradeStatus = response.getTradeStatus();
                PaymentStatus status = convertAlipayStatus(tradeStatus);
                
                log.info("支付宝订单查询成功，状态: {}, 转换后状态: {}", tradeStatus, status);
                
                return PaymentStatusResponse.builder()
                    .success(true)
                    .outTradeNo(outTradeNo)
                    .transactionId(response.getTradeNo())
                    .status(status)
                    .amount(response.getTotalAmount() != null ? new BigDecimal(response.getTotalAmount()) : null)
                    .message("查询成功")
                    .build();
            } else {
                log.error("支付宝订单查询失败: code={}, msg={}, subCode={}, subMsg={}", 
                    response.getCode(), response.getMsg(), response.getSubCode(), response.getSubMsg());
                return PaymentStatusResponse.builder()
                    .success(false)
                    .message(response.getSubMsg() != null ? response.getSubMsg() : response.getMsg())
                    .build();
            }
        } catch (AlipayApiException e) {
            log.error("查询支付宝订单状态异常", e);
            return PaymentStatusResponse.builder()
                .success(false)
                .message("查询失败: " + e.getMessage())
                .build();
        } catch (Exception e) {
            log.error("查询支付宝订单状态未知异常", e);
            return PaymentStatusResponse.builder()
                .success(false)
                .message("系统异常，请稍后重试")
                .build();
        }
    }
    
    @Override
    public boolean verifyNotify(Map<String, String> params) {
        try {
            log.info("验证支付宝回调签名");
            boolean result = AlipaySignature.rsaCheckV1(
                params,
                paymentConfig.getAlipay().getPublicKey(),
                "UTF-8",
                "RSA2"
            );
            log.info("支付宝回调签名验证结果: {}", result);
            return result;
        } catch (AlipayApiException e) {
            log.error("验证支付宝回调签名失败", e);
            return false;
        }
    }
    
    @Override
    public PaymentNotifyResult handleNotify(Map<String, String> params) {
        try {
            log.info("处理支付宝支付回调");
            
            String outTradeNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            String totalAmount = params.get("total_amount");
            String tradeStatus = params.get("trade_status");
            
            log.info("支付宝回调参数: outTradeNo={}, tradeNo={}, amount={}, status={}", 
                outTradeNo, tradeNo, totalAmount, tradeStatus);
            
            // 只处理支付成功的回调
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                return PaymentNotifyResult.builder()
                    .success(true)
                    .outTradeNo(outTradeNo)
                    .transactionId(tradeNo)
                    .amount(new BigDecimal(totalAmount))
                    .params(params)
                    .message("支付成功")
                    .build();
            } else {
                log.warn("支付宝回调状态不是成功状态: {}", tradeStatus);
                return PaymentNotifyResult.builder()
                    .success(false)
                    .outTradeNo(outTradeNo)
                    .params(params)
                    .message("支付状态异常: " + tradeStatus)
                    .build();
            }
        } catch (Exception e) {
            log.error("处理支付宝回调异常", e);
            return PaymentNotifyResult.builder()
                .success(false)
                .message("处理回调失败: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * 转换支付宝支付状态到系统状态
     */
    private PaymentStatus convertAlipayStatus(String alipayStatus) {
        if (alipayStatus == null) {
            return PaymentStatus.UNPAID;
        }
        
        switch (alipayStatus) {
            case "WAIT_BUYER_PAY":
                return PaymentStatus.UNPAID;
            case "TRADE_SUCCESS":
            case "TRADE_FINISHED":
                return PaymentStatus.PAID;
            case "TRADE_CLOSED":
                return PaymentStatus.PAYMENT_FAILED;
            default:
                log.warn("未知的支付宝支付状态: {}", alipayStatus);
                return PaymentStatus.UNPAID;
        }
    }
} 