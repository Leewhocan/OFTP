package com.example.demo.service.otp;

import com.example.demo.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

@Component
@Slf4j
public class SmsOtpChannel implements OtpChannel {

    private static final String SMPP_HOST = "localhost";
    private static final int SMPP_PORT = 2775;

    @Override
    public void send(String code, User user) {
        try (Socket socket = new Socket(SMPP_HOST, SMPP_PORT); PrintWriter out = new PrintWriter(socket.getOutputStream(), true); BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            log.info("Connected to SMPP simulator, sending SMS to {}: {}", user.getUsername(), code);

            out.println("SMS to " + user.getUsername() + ": OTP code is " + code);

            log.info("OTP sent via SMS to {}: {}", user.getUsername(), code);
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", user.getUsername(), e.getMessage());
        }
    }

    @Override
    public String getChannelName() {
        return "sms";
    }
}
