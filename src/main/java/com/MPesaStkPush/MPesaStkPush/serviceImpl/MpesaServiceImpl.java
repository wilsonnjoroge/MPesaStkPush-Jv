package com.MPesaStkPush.MPesaStkPush.serviceImpl;

import com.MPesaStkPush.MPesaStkPush.dto.MpesaRequestDto;
import com.MPesaStkPush.MPesaStkPush.entity.MpesaTransaction;
import com.MPesaStkPush.MPesaStkPush.repository.MpesaTransactionRepository;
import com.MPesaStkPush.MPesaStkPush.service.MPesaService;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
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
    public String initiateStkPush(MpesaRequestDto requestDto) throws JSONException {

        String phoneNumber = formatPhoneNumber(requestDto.getPhone());
        Double amount = requestDto.getAmount();
        String token = getMpesaAccessToken();

        MpesaTransaction transaction = new MpesaTransaction();
        transaction.setPhoneNumber(phoneNumber);
        transaction.setAmount(Double.valueOf(amount));
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setAccountReference("MPESA_STK");
        transaction.setTransactionDesc("Payment");

        transactionRepository.save(transaction);

        JSONObject stkPushRequest = new JSONObject();
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

        HttpResponse<String> response = Unirest.post(stkPushUrl)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(stkPushRequest.toString())
                .asString();

        JSONObject responseJson = new JSONObject(response.getBody());

        if (responseJson.has("CheckoutRequestID")) {
            transaction.setMerchantRequestId(responseJson.getString("MerchantRequestID"));
            transaction.setCheckoutRequestId(responseJson.getString("CheckoutRequestID"));
            transactionRepository.save(transaction);
        }

        return response.getBody();
    }

    private String formatPhoneNumber(String phoneNumber) {
        return "254" + phoneNumber.substring(1);
    }

    private String getMpesaAccessToken() throws JSONException {
        HttpResponse<String> response = Unirest.get(
                "https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials")
                .basicAuth(consumerKey, consumerSecret)
                .asString();

        JSONObject jsonObject = new JSONObject(response.getBody());
        return jsonObject.getString("access_token");
    }

    private String getEncodedPassword() {
        String timestamp = getCurrentTimestamp();
        String dataToEncode = shortCode + passKey + timestamp;
        return Base64.getEncoder().encodeToString(dataToEncode.getBytes());
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    @Override
    public void handleCallback(String callbackPayload) throws JSONException {

        JSONObject stkCallback = new JSONObject(callbackPayload)
                .getJSONObject("Body")
                .getJSONObject("stkCallback");

        String merchantRequestId = stkCallback.getString("MerchantRequestID");
        String checkoutRequestId = stkCallback.getString("CheckoutRequestID");
        int resultCode = stkCallback.getInt("ResultCode");
        String resultDesc = stkCallback.getString("ResultDesc");

        MpesaTransaction transaction =
                transactionRepository.findByCheckoutRequestId(checkoutRequestId);

        if (transaction != null) {

            transaction.setMerchantRequestId(merchantRequestId);
            transaction.setResultCode(String.valueOf(resultCode));
            transaction.setResultDesc(resultDesc);

            if (resultCode == 0 && stkCallback.has("CallbackMetadata")) {

                JSONArray items = stkCallback
                        .getJSONObject("CallbackMetadata")
                        .getJSONArray("Item");

                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);

                    if ("MpesaReceiptNumber".equals(item.getString("Name"))) {
                        transaction.setMpesaReceiptNumber(
                                item.getString("Value"));
                    }
                }
            }

            transactionRepository.save(transaction);

            logCallbackToFile(checkoutRequestId,
                    resultCode == 0 ? "Success" : "Failed",
                    resultDesc);

        } else {
            logCallbackToFile(checkoutRequestId,
                    "Not Found",
                    "Transaction not found in DB");
        }
    }

    private void logCallbackToFile(String transactionId,
                                   String status,
                                   String message) {

        String logEntry = String.format(
                "%s | Transaction ID: %s | Status: %s | Message: %s%n",
                LocalDateTime.now(), transactionId, status, message);

        try (BufferedWriter writer =
                     new BufferedWriter(new FileWriter(LOG_FILE_PATH, true))) {

            writer.write(logEntry);

        } catch (IOException e) {
            System.err.println("Failed to write to log file: "
                    + e.getMessage());
        }
    }
}
