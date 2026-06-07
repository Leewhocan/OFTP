package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.service.otp.OtpChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OtpChannelService {

    private final List<OtpChannel> channels;

    public void sendOtp(String code, User user, String channelName) {
        channels.stream()
                .filter(ch -> ch.getChannelName().equalsIgnoreCase(channelName))
                .findFirst()
                .ifPresentOrElse(
                        ch -> ch.send(code, user),
                        () -> {
                            throw new IllegalArgumentException("Unknown channel: " + channelName);
                        }
                );
    }

    public void sendOtpToAllChannels(String code, User user) {
        channels.forEach(ch -> ch.send(code, user));
    }

    public List<String> getAvailableChannels() {
        return channels.stream()
                .map(OtpChannel::getChannelName)
                .toList();
    }
}
