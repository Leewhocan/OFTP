package com.example.demo.service.otp;

import com.example.demo.model.User;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailOtpChannel implements OtpChannel {

    private final JavaMailSender mailSender;

    @Override
    public void send(String code, User user) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("noreply@demo.com");
            helper.setTo(user.getUsername() + "@example.com");
            helper.setSubject("Your OTP Code");
            helper.setText("Your OTP code is: " + code);
            mailSender.send(message);
            log.info("OTP sent via Email to {}", user.getUsername());
        } catch (Exception e) {
            log.error("Failed to send email", e);
        }
    }

    @Override
    public String getChannelName() {
        return "email";
    }
}
