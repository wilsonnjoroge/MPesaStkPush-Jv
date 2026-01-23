package com.MPesaStkPush.MPesaStkPush.service;

import kong.unirest.json.JSONException;

//Interface
public interface MPesaService {
    String initiateStkPush(String phoneNumber, Float amount) throws JSONException;
    void handleCallback(String callbackPayload) throws JSONException;
}

