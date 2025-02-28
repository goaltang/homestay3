package com.homestay3.homestaybackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("密码重置请求");
            
            String emailContent = String.format("""
                <html>
                <body>
                    <h2>密码重置请求</h2>
                    <p>您好，</p>
                    <p>我们收到了您的密码重置请求。请点击下面的链接重置您的密码：</p>
                    <p><a href="%s">重置密码</a></p>
                    <p>如果您没有请求重置密码，请忽略此邮件。</p>
                    <p>此链接将在24小时后失效。</p>
                    <p>谢谢！</p>
                </body>
                </html>
                """, resetLink);
            
            helper.setText(emailContent, true);
            mailSender.send(message);
            log.info("密码重置邮件已发送至: {}", toEmail);
        } catch (Exception e) {
            log.error("发送密码重置邮件失败: {}", e.getMessage());
            throw new RuntimeException("发送密码重置邮件失败: " + e.getMessage());
        }
    }
} 