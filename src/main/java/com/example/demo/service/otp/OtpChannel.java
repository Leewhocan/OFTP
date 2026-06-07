package com.example.demo.service.otp;

import com.example.demo.model.User;

public interface OtpChannel {

    void send(String code, User user);

    String getChannelName();
}
