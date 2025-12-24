package com.MPesaStkPush.MPesaStkPush.dto;

import lombok.Data;

@Data
public class MpesaRequestDto {
    private String phoneNumber;
    private Double amount;
    private String accountReference;
    private String transactionDesc;
    private String shortCode;
    private String passkey;
    private String callbackUrl;
}

