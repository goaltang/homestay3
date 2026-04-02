package com.homestay3.homestaybackend.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ContentFilterService {

    private static final Set<String> SENSITIVE_WORDS = new HashSet<>(Arrays.asList(
            // 示例敏感词，实际使用时替换为真实的敏感词库
            "傻逼", "蠢货", "废物", "垃圾",
            "操", "妈的", "麻痹", "草泥马",
            "fuck", "shit", "asshole", "bitch"
    ));

    private static final Pattern SENSITIVE_PATTERN;

    static {
        StringBuilder patternBuilder = new StringBuilder();
        for (String word : SENSITIVE_WORDS) {
            if (patternBuilder.length() > 0) {
                patternBuilder.append("|");
            }
            // 转义特殊字符
            patternBuilder.append("\\Q").append(word).append("\\E");
        }
        SENSITIVE_PATTERN = Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE);
    }

    /**
     * 检查内容是否包含敏感词
     * @param content 待检查的内容
     * @return true if 包含敏感词, false otherwise
     */
    public boolean containsSensitiveWords(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        return SENSITIVE_PATTERN.matcher(content).find();
    }

    /**
     * 过滤内容中的敏感词，替换为星号
     * @param content 待过滤的内容
     * @return 过滤后的内容
     */
    public String filter(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        return SENSITIVE_PATTERN.matcher(content).replaceAll(match -> {
            String word = match.group();
            return "*".repeat(word.length());
        });
    }

    /**
     * 验证内容，如果不通过则抛出异常
     * @param content 待验证的内容
     * @throws IllegalArgumentException 如果内容包含敏感词
     */
    public void validate(String content) {
        if (containsSensitiveWords(content)) {
            throw new IllegalArgumentException("内容包含敏感词，请修改后重试");
        }
    }

    /**
     * 获取敏感词列表（用于管理）
     */
    public Set<String> getSensitiveWords() {
        return SENSITIVE_WORDS;
    }
}
