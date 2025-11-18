package com.example.KMALegend.encode;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.method.HandlerMethod;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@Component
public class EncryptionInterceptor implements HandlerInterceptor {
    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper;

    public EncryptionInterceptor(EncryptionService encryptionService, ObjectMapper objectMapper) {
        this.encryptionService = encryptionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println("EncryptionInterceptor - URI: " + request.getRequestURI());
        System.out.println("EncryptionInterceptor - Method: " + request.getMethod());
        System.out.println("EncryptionInterceptor - Handler: " + handler.getClass().getSimpleName());
        
        // Skip cho public key endpoint
        if (request.getRequestURI().contains("/api/v1/encryption/public-key")) {
            System.out.println("EncryptionInterceptor - Skipping public key endpoint");
            return true;
        }
        
        // Chỉ xử lý POST requests
        if (!request.getMethod().equals("POST")) {
            System.out.println("EncryptionInterceptor - Skipping non-POST request");
            return true;
        }
        
        // Kiểm tra handler type - nhưng không skip nếu không phải HandlerMethod
        if (!(handler instanceof HandlerMethod)) {
            System.out.println("EncryptionInterceptor - Handler is not HandlerMethod: " + handler.getClass().getSimpleName());
            // Không return true ở đây, tiếp tục xử lý
        }

        try {
            // Đọc body của request
            String body = new BufferedReader(new InputStreamReader(request.getInputStream()))
                    .lines().collect(Collectors.joining("\n"));
            
            System.out.println("EncryptionInterceptor - Raw body: " + body);

            // Chuyển đổi JSON thành đối tượng EncryptedRequest
            EncryptedRequest encryptedRequest = objectMapper.readValue(body, EncryptedRequest.class);
            System.out.println("EncryptionInterceptor - EncryptedRequest parsed successfully");

            // Giải mã dữ liệu thành JSON string
            String decryptedJson = encryptionService.decryptData(
                    encryptedRequest.getEncryptedKey(),
                    encryptedRequest.getEncryptedData(),
                    encryptedRequest.getIv()
            );
            
            System.out.println("EncryptionInterceptor - Decrypted JSON: " + decryptedJson);

            // Lưu JSON string vào request attribute
            request.setAttribute("decryptedData", decryptedJson);
            
        } catch (Exception e) {
            System.err.println("EncryptionInterceptor - Error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        
        return true;
    }
} 