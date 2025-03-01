package com.homestay3.homestaybackend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            // 获取项目根目录的绝对路径
            String projectRoot = System.getProperty("user.dir");
            String uploadPath = projectRoot + File.separator + "uploads";
            String avatarsPath = uploadPath + File.separator + "avatars";
            
            // 确保目录存在并有正确的权限
            createAndVerifyDirectory(uploadPath);
            createAndVerifyDirectory(avatarsPath);
            
            // 使用标准化的绝对路径
            Path normalizedPath = Paths.get(uploadPath).toAbsolutePath().normalize();
            String fileUrl = "file:///" + normalizedPath.toString().replace('\\', '/') + "/";
            
            // 尝试使用另一种格式的URL
            String fileUrl2 = "file:" + normalizedPath.toString() + "/";
            
            log.info("开始配置静态资源:");
            log.info("项目根目录: {}", projectRoot);
            log.info("上传目录: {}", uploadPath);
            log.info("头像目录: {}", avatarsPath);
            log.info("资源URL1: {}", fileUrl);
            log.info("资源URL2: {}", fileUrl2);
            
            // 配置静态资源映射 - 使用多种方式确保可以访问
            registry.addResourceHandler("/uploads/**")
                   .addResourceLocations(fileUrl)
                   .setCachePeriod(0); // 开发阶段禁用缓存
            
            // 添加额外的映射，确保可以通过多种路径访问
            registry.addResourceHandler("/static/uploads/**")
                   .addResourceLocations(fileUrl)
                   .setCachePeriod(0);
            
            // 列出头像目录中的文件
            File avatarsDir = new File(avatarsPath);
            if (avatarsDir.exists() && avatarsDir.isDirectory()) {
                log.info("头像目录内容:");
                File[] files = avatarsDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        log.info("- {} ({}bytes, 可读:{}, 可写:{})", 
                            file.getName(), 
                            file.length(),
                            file.canRead(),
                            file.canWrite());
                    }
                }
            }
            
            // 测试目录访问权限
            testDirectoryAccess(new File(avatarsPath));
            
        } catch (Exception e) {
            log.error("配置静态资源时出错:", e);
            throw new RuntimeException("静态资源配置失败", e);
        }
    }

    private void createAndVerifyDirectory(String path) {
        File directory = new File(path);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            log.info("创建目录 {}: {}", path, created ? "成功" : "失败");
        }
        
        if (!directory.canRead() || !directory.canWrite()) {
            log.error("目录权限不足: {} - 读:{} 写:{}", 
                    path, directory.canRead(), directory.canWrite());
            throw new RuntimeException("目录权限不足: " + path);
        }
        
        log.info("目录已验证: {} - 存在:{} 可读:{} 可写:{}", 
                path, directory.exists(), directory.canRead(), directory.canWrite());
    }
    
    private void testDirectoryAccess(File directory) {
        try {
            // 测试创建临时文件
            File testFile = File.createTempFile("test", ".tmp", directory);
            boolean canWrite = testFile.exists();
            boolean canRead = testFile.canRead();
            testFile.delete();
            
            log.info("目录访问测试 - 位置: {}", directory.getAbsolutePath());
            log.info("可以创建文件: {}", canWrite);
            log.info("可以读取文件: {}", canRead);
        } catch (Exception e) {
            log.error("目录访问测试失败:", e);
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173") // 明确指定前端域名
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .exposedHeaders("Content-Type", "Content-Length", "Authorization", "Access-Control-Allow-Origin", "Access-Control-Allow-Headers", "Access-Control-Allow-Methods")
                .allowCredentials(true)
                .maxAge(3600);
        
        log.info("CORS配置已更新: 允许来自http://localhost:5173的请求");
        log.info("允许的方法: GET, POST, PUT, DELETE, OPTIONS, HEAD");
        log.info("允许的头信息: *");
        log.info("暴露的头信息: Content-Type, Content-Length, Authorization, Access-Control-Allow-Origin, Access-Control-Allow-Headers, Access-Control-Allow-Methods");
    }
} 