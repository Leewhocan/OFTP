package com.example.demo.controller;

import com.example.demo.dto.OtpGenerateRequest;
import com.example.demo.dto.OtpValidateRequest;
import com.example.demo.model.OtpCode;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.OtpChannelService;
import com.example.demo.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final OtpService otpService;
    private final UserRepository userRepository;
    private final OtpChannelService channelService;

    @GetMapping("/profile")
    public ResponseEntity<?> profile(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "username", authentication.getName(),
                "role", authentication.getAuthorities().toString()
        ));
    }

    @PostMapping("/otp/generate")
    public ResponseEntity<?> generateOtp(
            @RequestBody OtpGenerateRequest request,
            @RequestParam(defaultValue = "all") String channel,
            Authentication authentication) {

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow();

        OtpCode otp = otpService.generateOtp(user, request.getOperationId());

        if ("all".equalsIgnoreCase(channel)) {
            channelService.sendOtpToAllChannels(otp.getCode(), user);
        } else {
            channelService.sendOtp(otp.getCode(), user, channel);
        }

        return ResponseEntity.ok(Map.of(
                "operationId", otp.getOperationId(),
                "expiresAt", otp.getExpiresAt().toString(),
                "channels", channelService.getAvailableChannels()
        ));
    }

    @PostMapping("/otp/validate")
    public ResponseEntity<?> validateOtp(@RequestBody OtpValidateRequest request) {
        boolean valid = otpService.validateOtp(request.getCode(), request.getOperationId());

        if (valid) {
            return ResponseEntity.ok(Map.of("message", "OTP is valid"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired OTP"));
        }
    }

    @GetMapping("/otp/channels")
    public ResponseEntity<?> getChannels() {
        return ResponseEntity.ok(Map.of("channels", channelService.getAvailableChannels()));
    }
}
