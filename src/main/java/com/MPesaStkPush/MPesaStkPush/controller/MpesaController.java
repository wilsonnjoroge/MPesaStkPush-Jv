package com.MPesaStkPush.MPesaStkPush.controller;

import com.MPesaStkPush.MPesaStkPush.service.MPesaService;
import kong.unirest.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mpesa")
public class MpesaController {

    private final MPesaService mpesaService;

    @Autowired
    public MpesaController(MPesaService mpesaService) {
        this.mpesaService = mpesaService;
    }

    @PostMapping("/stkpush")
    public ResponseEntity<String> stkPush(@RequestParam String phoneNumber, @RequestParam Float amount) throws JSONException {

        String response = mpesaService.initiateStkPush(phoneNumber, amount);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/callback")
    public ResponseEntity<String> mpesaCallback(@RequestBody String callbackPayload) {
        try {
            mpesaService.handleCallback(callbackPayload);
        } catch (JSONException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error processing callback");
        }
        return ResponseEntity.ok("Callback received");
    }
}

