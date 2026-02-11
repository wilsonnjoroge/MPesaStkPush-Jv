package com.MPesaStkPush.MPesaStkPush.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
@Entity
@Table(name = "mpesa_transactions")
public class MpesaTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    // ===== Business Data =====
    private String accountUuid;
    private String merchantReference;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String currency;
    private String description;
    private String callbackUrl;

    // ===== Payment Data =====
    private Double amount;
    private String accountReference;
    private String transactionDesc;

    // ===== Daraja IDs =====
    private String merchantRequestId;
    private String checkoutRequestId;
    private String mpesaReceiptNumber;

    // ===== Status =====
    private String resultCode;
    private String resultDesc;

    private LocalDateTime timestamp;
}


