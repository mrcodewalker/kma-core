package com.example.KMALegend.encode;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.*;
import java.util.Base64;

@Component
public class RSAKeyPairGenerator {
    private KeyPair keyPair;
    private static final Logger logger = LoggerFactory.getLogger(RSAKeyPairGenerator.class);

    @PostConstruct
    public void init() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048); // Độ dài key 2048 bits
        this.keyPair = keyPairGenerator.generateKeyPair();
//        logger.info("Generated key pair: {}", this.keyPair.getPublic() + ", "+this.keyPair.getPrivate());
    }
    
    public String getPublicKeyAsBase64() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }
    
    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }
    
    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }
} 