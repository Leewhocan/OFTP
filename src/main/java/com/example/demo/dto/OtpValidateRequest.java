package com.example.demo.dto;

import lombok.Data;

@Data
public class OtpValidateRequest {

    private String code;
    private String operationId;
}
