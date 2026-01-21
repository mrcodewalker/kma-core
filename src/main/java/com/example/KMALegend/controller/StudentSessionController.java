package com.example.KMALegend.controller;

import com.example.KMALegend.dto.StudentSessionDTO;
import com.example.KMALegend.encode.DecryptedRequestWrapper;
import com.example.KMALegend.service.StudentSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/student-sessions")
@RequiredArgsConstructor
@Tag(name = "Student Session", description = "API quản lý phiên đăng nhập sinh viên")
public class StudentSessionController {
    
    private final StudentSessionService studentSessionService;
    private final ObjectMapper objectMapper;
    
    @PostMapping("/list")
    @Operation(summary = "Lấy danh sách phiên đăng nhập sinh viên với phân trang và tìm kiếm")
    public ResponseEntity<?> getStudentSessions(HttpServletRequest request) {
        try {
            DecryptedRequestWrapper wrapper = new DecryptedRequestWrapper(request, objectMapper);
            Map<String, Object> requestData = wrapper.getDecryptedBody(Map.class);
            
            // Lấy các tham số từ request đã giải mã
            String studentCode = requestData.get("studentCode") != null ? 
                requestData.get("studentCode").toString() : null;
            int page = requestData.get("page") != null ? 
                Integer.parseInt(requestData.get("page").toString()) : 0;
            int size = requestData.get("size") != null ? 
                Integer.parseInt(requestData.get("size").toString()) : 10;
            
            // Xử lý trường hợp studentCode rỗng
            if (studentCode != null && studentCode.trim().isEmpty()) {
                studentCode = null;
            }
            
            Page<StudentSessionDTO> result = studentSessionService
                    .getStudentSessions(studentCode, page, size);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Bad Request",
                "message", "Invalid request format or decryption failed",
                "details", e.getMessage()
            ));
        }
    }
}