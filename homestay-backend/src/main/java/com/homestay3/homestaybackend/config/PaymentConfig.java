package com.homestay3.homestaybackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 支付配置类
 */
@Configuration
@ConfigurationProperties(prefix = "payment")
@Data
public class PaymentConfig {
    
    private AlipayConfig alipay = new AlipayConfig();
    private WechatConfig wechat = new WechatConfig();
    private int timeoutMinutes = 10;
    private int retryCount = 3;
    private boolean asyncNotifyEnabled = true;
    
    @Data
    public static class AlipayConfig {
        private String appId;
        private String privateKey;
        private String publicKey;
        private String gatewayUrl;
        private String notifyUrl;
        private String returnUrl;
        private int connectTimeout = 60000;  // 60秒连接超时
        private int readTimeout = 120000;    // 120秒读取超时
        private int maxRetries = 3;          // 最大重试次数
        private int retryDelay = 3000;       // 重试延迟(毫秒)
    }
    
    @Data
    public static class WechatConfig {
        private String appId;
        private String mchId;
        private String apiKey;
        private String certPath;
        private String notifyUrl;
    }
} 