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
            String homestaysPath = uploadPath + File.separator + "homestays";
            
            // 确保目录存在并有正确的权限
            createAndVerifyDirectory(uploadPath);
            createAndVerifyDirectory(avatarsPath);
            createAndVerifyDirectory(homestaysPath);
            
            // 使用标准化的绝对路径
            Path normalizedPath = Paths.get(uploadPath).toAbsolutePath().normalize();
            String fileUrl = "file:///" + normalizedPath.toString().replace('\\', '/') + "/";
            
            // 尝试使用另一种格式的URL
            String fileUrl2 = "file:" + normalizedPath.toString() + File.separator;
            
            log.info("开始配置静态资源:");
            log.info("项目根目录: {}", projectRoot);
            log.info("上传目录: {}", uploadPath);
            log.info("头像目录: {}", avatarsPath);
            log.info("民宿图片目录: {}", homestaysPath);
            log.info("资源URL1: {}", fileUrl);
            log.info("资源URL2: {}", fileUrl2);
            
            // 配置静态资源映射 - 使用多种方式确保可以访问
            registry.addResourceHandler("/uploads/**")
                   .addResourceLocations(fileUrl2)
                   .setCachePeriod(0); // 开发阶段禁用缓存
            
            // 特别为homestays目录添加映射
            Path homestaysNormalizedPath = Paths.get(homestaysPath).toAbsolutePath().normalize();
            String homestaysFileUrl = "file:" + homestaysNormalizedPath.toString() + File.separator;
            registry.addResourceHandler("/uploads/homestays/**")
                   .addResourceLocations(homestaysFileUrl)
                   .setCachePeriod(0);
            
            // 列出homestays目录中的文件
            File homestaysDir = new File(homestaysPath);
            if (homestaysDir.exists() && homestaysDir.isDirectory()) {
                log.info("民宿图片目录内容:");
                File[] files = homestaysDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        log.info("- {} ({}bytes, 可读:{}, 可写:{})", 
                            file.getName(), 
                            file.length(),
                            file.canRead(),
                            file.canWrite());
                    }
                } else {
                    log.warn("民宿图片目录为空或无法列出文件");
                }
            } else {
                log.warn("民宿图片目录不存在或不是有效目录");
            }
            
            // 测试目录访问权限
            testDirectoryAccess(new File(homestaysPath));
            
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
                // 将通配符*改为具体的前端URL列表
                .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173", 
                               "http://localhost:5174", "http://127.0.0.1:5174")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD")
                .allowedHeaders("*")
                .exposedHeaders("Content-Type", "Content-Length", "Authorization", 
                               "Access-Control-Allow-Origin", "Access-Control-Allow-Headers", 
                               "Access-Control-Allow-Methods", "Access-Control-Allow-Credentials")
                .allowCredentials(true)
                .maxAge(3600);
        
        log.info("CORS配置已更新: 允许特定来源的请求");
        log.info("允许的来源: http://localhost:5173, http://127.0.0.1:5173, http://localhost:5174, http://127.0.0.1:5174");
        log.info("允许的方法: GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH");
        log.info("允许的头信息: *");
    }
} 