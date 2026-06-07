package com.example.demo.service.otp;

import com.example.demo.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

@Component
@Slf4j
public class FileOtpChannel implements OtpChannel {

    @Override
    public void send(String code, User user) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("otp_codes.txt", true))) {
            writer.println(LocalDateTime.now() + " | User: " + user.getUsername() + " | OTP: " + code);
            log.info("OTP saved to file for user {}", user.getUsername());
        } catch (IOException e) {
            log.error("Failed to save OTP to file", e);
        }
    }

    @Override
    public String getChannelName() {
        return "file";
    }
}
