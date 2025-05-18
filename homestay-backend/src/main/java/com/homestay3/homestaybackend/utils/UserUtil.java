package com.homestay3.homestaybackend.utils;

// import com.homestay3.homestaybackend.model.CustomUserDetails; // 移除或注释掉错误的导入
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 用户工具类，用于获取当前登录用户信息
 */
public class UserUtil {

    /**
     * 获取当前认证的用户信息
     *
     * @return Authentication 对象，如果未认证则返回 null
     */
    public static Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 获取当前登录用户的 Principal 对象
     *
     * @return Principal 对象，通常是 UserDetails 或用户名字符串，未认证则返回 null
     */
    public static Object getCurrentPrincipal() {
        Authentication authentication = getCurrentAuthentication();
        return (authentication == null) ? null : authentication.getPrincipal();
    }

    /**
     * 获取当前登录用户的 UserDetails 对象
     *
     * @return UserDetails 对象，如果 Principal 不是 UserDetails 类型或未认证则返回 null
     */
    public static UserDetails getCurrentUserDetails() {
        Object principal = getCurrentPrincipal();
        if (principal instanceof UserDetails) {
            return (UserDetails) principal;
        }
        return null;
    }

    /**
     * 获取当前登录用户的用户名
     *
     * @return 用户名，如果未认证则返回 null
     */
    public static String getCurrentUsername() {
        Authentication authentication = getCurrentAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            return (String) principal;
        }
        return null; // 或者抛出异常，根据需要
    }

    /**
     * 获取当前登录用户的 ID。
     * !!! 重要: 此方法现在假设 Authentication 的 'name' 存储的是用户 ID 的字符串表示形式。
     * 如果 'name' 存储的是用户名，此方法将失败。
     *
     * @return 用户ID，如果无法获取则抛出 IllegalStateException
     * @throws IllegalStateException 如果用户未认证或 Authentication 名称不是有效的 Long 类型
     */
    public static Long getCurrentUserId() {
        Authentication authentication = getCurrentAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String name = authentication.getName();
            if (name != null) {
                try {
                    // 尝试将 Authentication 的 name 解析为 Long
                    return Long.parseLong(name);
                } catch (NumberFormatException e) {
                    // 如果名称不是数字ID，则抛出异常，表明配置或使用方式有问题
                    throw new IllegalStateException("无法从 Authentication 名称获取用户 ID，名称不是有效的 Long: " + name, e);
                }
            }
        }
        // 如果 authentication 为 null 或未认证，或者 name 为 null
        throw new IllegalStateException("无法获取当前用户 ID，用户未认证或认证信息无效");
    }

    /**
     * 检查当前用户是否已认证
     *
     * @return 如果用户已认证且不是匿名用户，则返回 true
     */
    public static boolean isAuthenticated() {
        Authentication authentication = getCurrentAuthentication();
        return authentication != null &&
               authentication.isAuthenticated() &&
               !(authentication.getPrincipal() instanceof String && authentication.getPrincipal().equals("anonymousUser"));
    }
} 