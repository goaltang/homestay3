package com.homestay3.homestaybackend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentialsException(BadCredentialsException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "用户名或密码错误");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        logger.error("系统发生运行时异常，路径: {}, 异常: {}", path, ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("timestamp", new Date());
        errorResponse.put("path", path);
        
        // 为房东房源列表API提供特定的错误处理
        if (path.contains("/api/homestays/owner")) {
            errorResponse.put("message", "获取房源列表失败，请重试");
            errorResponse.put("data", new ArrayList<>());
            errorResponse.put("total", 0);
            errorResponse.put("page", 0);
            errorResponse.put("size", 10);
            errorResponse.put("pages", 0);
        } else if (path.contains("/api/homestays") && !path.contains("/api/homestays/")) {
            errorResponse.put("message", "系统处理失败，请重试");
            errorResponse.put("data", new ArrayList<>());
        } else {
            errorResponse.put("message", "系统处理失败，请重试");
        }
        
        // 开发环境下返回详细错误信息
        String activeProfile = System.getProperty("spring.profiles.active", "dev");
        if ("dev".equals(activeProfile) || "development".equals(activeProfile)) {
            errorResponse.put("error", ex.getMessage());
            errorResponse.put("errorType", ex.getClass().getSimpleName());
        }
        
        return ResponseEntity.ok(errorResponse);
    }
    
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, Object>> handleNullPointerException(NullPointerException ex, WebRequest request) {
        logger.error("系统发生空指针异常: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "数据处理异常，请重试");
        errorResponse.put("timestamp", new Date());
        errorResponse.put("path", request.getDescription(false).replace("uri=", ""));
        
        String path = request.getDescription(false);
        if (path.contains("/api/homestays") && !path.contains("/api/homestays/")) {
            errorResponse.put("data", new ArrayList<>());
        }
        
        return ResponseEntity.ok(errorResponse);
    }
    
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(
            org.springframework.dao.DataAccessException ex, WebRequest request) {
        logger.error("数据库访问异常: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "数据服务暂时不可用，请稍后重试");
        errorResponse.put("timestamp", new Date());
        errorResponse.put("path", request.getDescription(false).replace("uri=", ""));
        
        String path = request.getDescription(false);
        if (path.contains("/api/homestays") && !path.contains("/api/homestays/")) {
            errorResponse.put("data", new ArrayList<>());
        }
        
        return ResponseEntity.ok(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex, WebRequest request) {
        logger.error("系统发生未捕获异常: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "系统暂时繁忙，请稍后重试");
        errorResponse.put("timestamp", new Date());
        errorResponse.put("path", request.getDescription(false).replace("uri=", ""));
        
        // 为获取房源列表的API提供空数据而不是错误
        String path = request.getDescription(false);
        if (path.contains("/api/homestays") && !path.contains("/api/homestays/")) {
            errorResponse.put("data", new ArrayList<>());
            errorResponse.put("message", "暂无房源数据");
        }
        
        // 开发环境下返回详细错误信息，生产环境下隐藏
        String activeProfile = System.getProperty("spring.profiles.active", "dev");
        if ("dev".equals(activeProfile) || "development".equals(activeProfile)) {
            errorResponse.put("error", ex.getMessage());
            errorResponse.put("type", ex.getClass().getSimpleName());
        }
        
        return ResponseEntity.ok(errorResponse);
    }
} 