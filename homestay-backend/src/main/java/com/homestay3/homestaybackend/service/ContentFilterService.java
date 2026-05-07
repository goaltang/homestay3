package com.homestay3.homestaybackend.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ContentFilterService {

    // 默认敏感词库（作为兜底）
    private static final Set<String> DEFAULT_SENSITIVE_WORDS = new HashSet<>(Arrays.asList(
            "傻逼", "蠢货", "废物", "垃圾",
            "操", "妈的", "麻痹", "草泥马",
            "fuck", "shit", "asshole", "bitch"
    ));

    @Value("${app.sensitive-words:}")
    private String sensitiveWordsConfig;

    private Set<String> sensitiveWords = new HashSet<>();
    private Pattern sensitivePattern;

    @PostConstruct
    public void init() {
        // 先加载默认词库
        sensitiveWords.addAll(DEFAULT_SENSITIVE_WORDS);

        // 如果配置了自定义敏感词，则追加（支持逗号分隔）
        if (sensitiveWordsConfig != null && !sensitiveWordsConfig.trim().isEmpty()) {
            String[] customWords = sensitiveWordsConfig.split(",");
            for (String word : customWords) {
                String trimmed = word.trim();
                if (!trimmed.isEmpty()) {
                    sensitiveWords.add(trimmed);
                }
            }
        }

        rebuildPattern();
    }

    private void rebuildPattern() {
        if (sensitiveWords.isEmpty()) {
            sensitivePattern = Pattern.compile("(?!.*)"); // 匹配空
            return;
        }
        StringBuilder patternBuilder = new StringBuilder();
        for (String word : sensitiveWords) {
            if (patternBuilder.length() > 0) {
                patternBuilder.append("|");
            }
            patternBuilder.append("\\Q").append(word).append("\\E");
        }
        sensitivePattern = Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE);
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
        return sensitivePattern.matcher(content).find();
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
        return sensitivePattern.matcher(content).replaceAll(match -> {
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
        return Collections.unmodifiableSet(sensitiveWords);
    }

    /**
     * 动态更新敏感词库（可用于管理后台热更新）
     * @param words 新的敏感词集合
     */
    public synchronized void updateSensitiveWords(Set<String> words) {
        sensitiveWords.clear();
        sensitiveWords.addAll(DEFAULT_SENSITIVE_WORDS);
        if (words != null) {
            sensitiveWords.addAll(words);
        }
        rebuildPattern();
    }
}
