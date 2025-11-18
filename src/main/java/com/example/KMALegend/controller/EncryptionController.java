package com.example.KMALegend.controller;

import com.example.KMALegend.encode.RSAKeyPairGenerator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/encryption")
public class EncryptionController {
    private final RSAKeyPairGenerator rsaKeyPairGenerator;

    public EncryptionController(RSAKeyPairGenerator rsaKeyPairGenerator) {
        this.rsaKeyPairGenerator = rsaKeyPairGenerator;
    }

    @GetMapping("/public-key")
    public String getPublicKey() {
        return rsaKeyPairGenerator.getPublicKeyAsBase64();
    }
} 