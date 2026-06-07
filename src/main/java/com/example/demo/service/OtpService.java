package com.example.demo.service;

import com.example.demo.config.OtpConfig;
import com.example.demo.model.OtpCode;
import com.example.demo.model.User;
import com.example.demo.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository otpRepository;
    private final OtpConfig otpConfig;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpCode generateOtp(User user, String operationId) {
        String code = generateRandomCode(otpConfig.getLength());

        OtpCode otpCode = OtpCode.builder()
                .code(code)
                .operationId(operationId)
                .status(OtpCode.OtpStatus.ACTIVE)
                .user(user)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusSeconds(otpConfig.getTtlSeconds()))
                .build();

        return otpRepository.save(otpCode);
    }

    public boolean validateOtp(String code, String operationId) {
        Optional<OtpCode> otpOpt = otpRepository
                .findByCodeAndOperationIdAndStatus(code, operationId, OtpCode.OtpStatus.ACTIVE);

        if (otpOpt.isEmpty()) {
            return false;
        }

        OtpCode otp = otpOpt.get();

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            otp.setStatus(OtpCode.OtpStatus.EXPIRED);
            otpRepository.save(otp);
            return false;
        }

        otp.setStatus(OtpCode.OtpStatus.USED);
        otpRepository.save(otp);
        return true;
    }

    public void updateOtpConfig(int length, int ttlSeconds) {
        otpConfig.setLength(length);
        otpConfig.setTtlSeconds(ttlSeconds);
    }

    public OtpConfig getOtpConfig() {
        return otpConfig;
    }

    private String generateRandomCode(int length) {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(secureRandom.nextInt(10));
        }
        return code.toString();
    }
}
