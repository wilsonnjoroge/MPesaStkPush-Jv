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
    private String phoneNumber;
    private Double amount;
    private String accountReference;
    private String transactionDesc;
    private String merchantRequestId;
    private String checkoutRequestId;
    private LocalDateTime timestamp;
    private String resultCode;
    private String resultDesc;

}

