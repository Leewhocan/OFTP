package com.example.demo.service.otp;

import com.example.demo.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@Slf4j
public class TelegramOtpChannel implements OtpChannel {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String TELEGRAM_API = "http://localhost:8089/bot123456:ABC-DEF1234ghiklm/sendMessage";

    @Override
    public void send(String code, User user) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "chat_id", user.getId().intValue(),
                    "text", "Your OTP code is: " + code
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(TELEGRAM_API, request, String.class);

            log.info("OTP sent via Telegram to {}: {} | Response: {}", user.getUsername(), code, response);
        } catch (Exception e) {
            log.error("Failed to send Telegram OTP to {}: {}", user.getUsername(), e.getMessage());
        }
    }

    @Override
    public String getChannelName() {
        return "telegram";
    }
}
