package com.homestay3.homestaybackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web MVC配置
 * 处理资源映射和跨域请求
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    /**
     * 添加资源处理器
     * @param registry 资源处理器注册表
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 添加静态资源映射
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        
        // 添加上传文件夹映射
        String uploadDir = "file:./uploads/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadDir);
                
        // 添加API文件访问映射 - 将/api/files/路径映射到uploads目录
        registry.addResourceHandler("/api/files/**")
                .addResourceLocations(uploadDir)
                .setCachePeriod(3600)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        // 将resourcePath映射到正确的物理路径
                        // /api/files/avatar/123.jpg → /uploads/avatar/123.jpg
                        Resource resource = location.createRelative(resourcePath);
                        return resource.exists() && resource.isReadable() ? resource : null;
                    }
                });
    }
} 