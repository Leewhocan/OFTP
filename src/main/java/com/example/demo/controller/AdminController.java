package com.example.demo.controller;

import com.example.demo.config.OtpConfig;
import com.example.demo.model.User;
import com.example.demo.repository.OtpRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final OtpService otpService;

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        List<Map<String, Object>> users = userRepository.findAll().stream()
                .filter(u -> !u.getRoles().contains("ROLE_ADMIN"))
                .map(u -> Map.<String, Object>of(
                "id", u.getId(),
                "username", u.getUsername(),
                "roles", u.getRoles()
        ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userRepository.findById(id).ifPresent(user -> {
            if (!user.getRoles().contains("ROLE_ADMIN")) {
                otpRepository.deleteByUserId(user.getId());
                userRepository.delete(user);
            }
        });
        return ResponseEntity.ok(Map.of("message", "User and related OTP codes deleted"));
    }

    @GetMapping("/otp/config")
    public ResponseEntity<?> getOtpConfig() {
        OtpConfig config = otpService.getOtpConfig();
        return ResponseEntity.ok(Map.of(
                "length", config.getLength(),
                "ttlSeconds", config.getTtlSeconds()
        ));
    }

    @PutMapping("/otp/config")
    public ResponseEntity<?> updateOtpConfig(@RequestBody Map<String, Integer> request) {
        int length = request.getOrDefault("length", 6);
        int ttl = request.getOrDefault("ttlSeconds", 300);
        otpService.updateOtpConfig(length, ttl);
        return ResponseEntity.ok(Map.of(
                "message", "OTP config updated",
                "length", length,
                "ttlSeconds", ttl
        ));
    }
}
