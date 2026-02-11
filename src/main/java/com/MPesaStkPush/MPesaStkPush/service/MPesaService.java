package com.MPesaStkPush.MPesaStkPush.service;

import com.MPesaStkPush.MPesaStkPush.dto.MpesaRequestDto;

import kong.unirest.json.JSONException;

//Interface
public interface MPesaService {
    String initiateStkPush(MpesaRequestDto requestDto) throws JSONException;
    void handleCallback(String callbackPayload) throws JSONException;
}

