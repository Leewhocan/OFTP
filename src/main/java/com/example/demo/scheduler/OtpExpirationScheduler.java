package com.example.demo.scheduler;

import com.example.demo.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class OtpExpirationScheduler {

    private final OtpRepository otpRepository;

    @Scheduled(fixedRateString = "${otp.scheduler-interval-ms:30000}")
    public void expireOldOtps() {
        int expired = otpRepository.expireOldCodes(LocalDateTime.now());
        if (expired > 0) {
            log.info("Expired {} OTP codes", expired);
        }
    }
}
