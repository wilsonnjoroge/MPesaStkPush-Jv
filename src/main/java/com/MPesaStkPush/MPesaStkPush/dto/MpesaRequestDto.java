package com.MPesaStkPush.MPesaStkPush.dto;

import lombok.Data;

@Data
public class MpesaRequestDto {

    private String accountUuid;
    private String merchantReference;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private Double amount;
    private String currency;
    private String description;
    private String callbackUrl;
}

