package com.homestay3.homestaybackend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * HTTP客户端配置
 * 提高网络连接的稳定性和超时设置
 */
@Configuration
@Slf4j
public class HttpClientConfig {
    
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder
            .setConnectTimeout(Duration.ofSeconds(60))  // 连接超时60秒
            .setReadTimeout(Duration.ofSeconds(120))    // 读取超时120秒
            .build();
        
        log.info("RestTemplate配置完成，连接超时: 60s, 读取超时: 120s");
        return restTemplate;
    }
} 