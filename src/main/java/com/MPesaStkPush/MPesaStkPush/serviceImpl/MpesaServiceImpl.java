package com.MPesaStkPush.MPesaStkPush.serviceImpl;

import com.MPesaStkPush.MPesaStkPush.entity.MpesaTransaction;
import com.MPesaStkPush.MPesaStkPush.repository.MpesaTransactionRepository;
import com.MPesaStkPush.MPesaStkPush.service.MPesaService;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.json.JSONException;
import kong.unirest.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class MpesaServiceImpl implements MPesaService {

    @Value("${mpesa.consumer_key}")
    private String consumerKey;

    @Value("${mpesa.consumer_secret}")
    private String consumerSecret;

    @Value("${mpesa.shortcode}")
    private String shortCode;

    @Value("${mpesa.passkey}")
    private String passKey;

    @Value("${mpesa.stkpush_url}")
    private String stkPushUrl;

    @Autowired
    private MpesaTransactionRepository transactionRepository;

    @Value("${mpesa.log_file_path}")
    private String LOG_FILE_PATH;

    @Override
    public String initiateStkPush(String phoneNumber, Float amount) throws JSONException {
        // Format the phone number
        phoneNumber = formatPhoneNumber(phoneNumber);

        String token = getMpesaAccessToken();

        MpesaTransaction transaction = new MpesaTransaction();
        transaction.setPhoneNumber(phoneNumber);
        transaction.setAmount(Double.valueOf(amount));
        transaction.setTimestamp(LocalDateTime.now());

        transactionRepository.save(transaction);

        // Build the STK push request JSON
        JSONObject stkPushRequest = new JSONObject();
        try {
            stkPushRequest.put("BusinessShortCode", shortCode);
            stkPushRequest.put("Password", getEncodedPassword());
            stkPushRequest.put("Timestamp", getCurrentTimestamp());
            stkPushRequest.put("TransactionType", "CustomerPayBillOnline");
            stkPushRequest.put("Amount", amount);
            stkPushRequest.put("PartyA", phoneNumber);
            stkPushRequest.put("PartyB", shortCode);
            stkPushRequest.put("PhoneNumber", phoneNumber);
            stkPushRequest.put("CallBackURL", "https://f3fe-197-155-71-138.ngrok-free.app/api/mpesa/callback");
            stkPushRequest.put("AccountReference", "MPESA_STK");
            stkPushRequest.put("TransactionDesc", "Payment");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Make HTTP POST request to MPesa API
        HttpResponse<String> response = Unirest.post(stkPushUrl)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(stkPushRequest.toString())
                .asString();

        return response.getBody();
    }

    private String formatPhoneNumber(String phoneNumber) {
        return "254" + phoneNumber.substring(1);
    }

    @Override
    public void handleCallback(String callbackPayload) throws JSONException {
        JSONObject callbackJson = new JSONObject(callbackPayload);
        JSONObject body = callbackJson.getJSONObject("Body");
        JSONObject stkCallback = body.getJSONObject("stkCallback");

        String merchantRequestId = stkCallback.getString("MerchantRequestID");
        String checkoutRequestId = stkCallback.getString("CheckoutRequestID");
        int resultCode = stkCallback.getInt("ResultCode");
        String resultDesc = stkCallback.getString("ResultDesc");

        // Find the transaction in the database using the checkoutRequestId
        MpesaTransaction transaction = transactionRepository.findByCheckoutRequestId(checkoutRequestId);

        // Log the transaction result
        String status;
        if (transaction != null) {
            transaction.setResultCode(String.valueOf(resultCode));
            transaction.setResultDesc(resultDesc);
            transactionRepository.save(transaction);

            // Log different statuses based on resultCode
            if (resultCode == 0) {
                status = "Success";
                logCallbackToFile(checkoutRequestId, status, "Transaction completed successfully.");
            } else if (resultCode == 1) {
                status = "Failed";
                logCallbackToFile(checkoutRequestId, status, "Transaction failed due to a wrong PIN or insufficient funds.");
            } else {
                status = "Unknown";
                logCallbackToFile(checkoutRequestId, status, "Transaction status unknown: " + resultDesc);
            }
        } else {
            // Handle case where the transaction does not exist
            String message = "Transaction not found with CheckoutRequestID: " + checkoutRequestId;
            System.out.println(message);
            logCallbackToFile(checkoutRequestId, "Not Found", message);
        }
    }


    private void logCallbackToFile(String transactionId, String status, String message) {
        String logEntry = String.format("%s | Transaction ID: %s | Status: %s | Message: %s%n",
                LocalDateTime.now(), transactionId, status, message);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE_PATH, true))) {
            writer.write(logEntry);
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }

    private String getMpesaAccessToken() throws JSONException {
        HttpResponse<String> response = Unirest.get("https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials")
                .basicAuth(consumerKey, consumerSecret)
                .asString();

        JSONObject jsonObject = new JSONObject(response.getBody());
        return jsonObject.getString("access_token");
    }

    private String getEncodedPassword() {
        // Encode password using base64 (ShortCode + PassKey + Timestamp)
        String dataToEncode = shortCode + passKey + getCurrentTimestamp();
        return Base64.getEncoder().encodeToString(dataToEncode.getBytes());
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }
}

