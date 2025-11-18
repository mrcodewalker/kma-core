package com.example.KMALegend.encode;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Base64;

@Service
public class EncryptionService {
    private final RSAKeyPairGenerator rsaKeyPairGenerator;
    private final ObjectMapper objectMapper;

    public EncryptionService(RSAKeyPairGenerator rsaKeyPairGenerator, ObjectMapper objectMapper) {
        this.rsaKeyPairGenerator = rsaKeyPairGenerator;
        this.objectMapper = objectMapper;
    }

    public String decryptData(String encryptedKey, String encryptedData, String iv) throws Exception {
        // Decrypt the RSA-encrypted AES key
        byte[] decryptedKeyHex = decryptRSA(Base64.getDecoder().decode(encryptedKey));

        // Convert the hex string back to bytes for the AES key
        String keyHex = new String(decryptedKeyHex);
        byte[] aesKeyBytes = hexStringToByteArray(keyHex);

        // Create AES key
        SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");

        // Convert IV from hex to bytes
        byte[] ivBytes = hexStringToByteArray(iv);

        // Decrypt the data
        return decryptAES(encryptedData, aesKey, ivBytes);
    }

    public <T> T decryptDataToObject(String encryptedKey, String encryptedData, String iv, Class<T> valueType) throws Exception {
        String decryptedJson = decryptData(encryptedKey, encryptedData, iv);
        return objectMapper.readValue(decryptedJson, valueType);
    }

    private byte[] decryptRSA(byte[] encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        PrivateKey privateKey = rsaKeyPairGenerator.getPrivateKey();
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(encryptedData);
    }

    private String decryptAES(String encryptedData, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

        // The encrypted data is in CryptoJS's format, which is base64
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedBytes);
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}