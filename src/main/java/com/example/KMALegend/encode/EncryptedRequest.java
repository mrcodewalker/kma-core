package com.example.KMALegend.encode;

import lombok.Data;

@Data
public class EncryptedRequest {
    private String encryptedKey;
    private String encryptedData;
    private String iv;
} 