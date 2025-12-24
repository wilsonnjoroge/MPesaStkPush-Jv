package com.MPesaStkPush.MPesaStkPush.repository;


import com.MPesaStkPush.MPesaStkPush.entity.MpesaTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MpesaTransactionRepository extends JpaRepository<MpesaTransaction, Long> {
    MpesaTransaction findByCheckoutRequestId(String checkoutRequestId);
}


