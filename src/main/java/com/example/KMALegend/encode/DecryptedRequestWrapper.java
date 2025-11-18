package com.example.KMALegend.encode;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;

public class DecryptedRequestWrapper {
    private final HttpServletRequest request;
    private final ObjectMapper objectMapper;

    public DecryptedRequestWrapper(HttpServletRequest request, ObjectMapper objectMapper) {
        this.request = request;
        this.objectMapper = objectMapper;
    }

    public String getDecryptedJson() {
        return (String) request.getAttribute("decryptedData");
    }

    public <T> T getDecryptedBody(Class<T> valueType) throws Exception {
        String decryptedJson = getDecryptedJson();
        return objectMapper.readValue(decryptedJson, valueType);
    }
} 