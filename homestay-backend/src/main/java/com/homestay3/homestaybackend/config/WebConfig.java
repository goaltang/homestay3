package com.homestay3.homestaybackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);

    // Inject the upload directory path from application.properties/yml
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Ensure the path uses forward slashes and ends with a slash
        String resourcePath = "file:" + uploadDir.replace("\\", "/");
        if (!resourcePath.endsWith("/")) {
            resourcePath += "/";
        }

        logger.info("Configuring resource handler: Mapping /static/uploads/** to {}", resourcePath);

        registry.addResourceHandler("/static/uploads/**") // The URL path clients will use
                .addResourceLocations(resourcePath);       // The physical directory location on the server
    }
}