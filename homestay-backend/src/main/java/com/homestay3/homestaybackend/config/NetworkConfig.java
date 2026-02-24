package com.homestay3.homestaybackend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.InitializingBean;

/**
 * 网络配置类
 * 设置JVM级别的网络超时参数
 */
@Configuration
@Slf4j
public class NetworkConfig implements InitializingBean {
    
    @Override
    public void afterPropertiesSet() {
        // 设置JVM级别的网络超时参数
        System.setProperty("sun.net.client.defaultConnectTimeout", "60000");  // 60秒连接超时
        System.setProperty("sun.net.client.defaultReadTimeout", "120000");     // 120秒读取超时
        
        // 设置HTTP连接超时
        System.setProperty("http.connection.timeout", "60000");
        System.setProperty("http.socket.timeout", "120000");
        
        // 设置HTTPS连接超时
        System.setProperty("https.connection.timeout", "60000");
        System.setProperty("https.socket.timeout", "120000");
        
        log.info("网络超时配置完成: 连接超时=60s, 读取超时=120s");
    }
} 