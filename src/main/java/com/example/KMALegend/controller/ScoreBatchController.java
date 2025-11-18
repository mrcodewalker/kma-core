package com.example.KMALegend.controller;

import com.example.KMALegend.dto.ScoreBatchRequestDTO;
import com.example.KMALegend.entity.ScoreBatch;
import com.example.KMALegend.encode.DecryptedRequestWrapper;
import com.example.KMALegend.service.ScoreBatchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/score-batch")
@CrossOrigin(origins = {"https://kma-legend.click", "http://localhost:4200"})
@Tag(name = "Score Batch", description = "Score batch management APIs")
public class ScoreBatchController {

    private final ScoreBatchService scoreBatchService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "Create or update score batch", 
               description = "Creates a new score batch or updates existing one for a student")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Score batch created/updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/create-or-update")
    public ResponseEntity<?> createOrUpdateScoreBatch(HttpServletRequest request) {
        try {
            // Debug: Kiểm tra decrypted data
            String decryptedJson = (String) request.getAttribute("decryptedData");
            System.out.println("Decrypted JSON: " + decryptedJson);
            
            if (decryptedJson == null) {
                return ResponseEntity.badRequest().body("No decrypted data found. Check encryption interceptor.");
            }
            
            DecryptedRequestWrapper wrapper = new DecryptedRequestWrapper(request, objectMapper);
            ScoreBatchRequestDTO requestDTO = wrapper.getDecryptedBody(ScoreBatchRequestDTO.class);
            
            // Sử dụng method tối ưu cho update
            ScoreBatch result = scoreBatchService.updateScoreBatchOptimized(requestDTO);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace(); // Debug stack trace
            return ResponseEntity.badRequest().body("Error processing score batch: " + e.getMessage());
        }
    }

    @Operation(summary = "Get score batch by student code", 
               description = "Retrieves score batch information for a specific student")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Score batch retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Score batch not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/student/{studentCode}")
    public ResponseEntity<?> getScoreBatchByStudentCode(@PathVariable String studentCode) {
        try {
            ScoreBatch result = scoreBatchService.getScoreBatchByStudentCode(studentCode);
            if (result != null) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error retrieving score batch: " + e.getMessage());
        }
    }

    // Test endpoint để debug encryption
    @PostMapping("/test-encryption")
    public ResponseEntity<?> testEncryption(HttpServletRequest request) {
        try {
            String decryptedJson = (String) request.getAttribute("decryptedData");
            System.out.println("Test endpoint - Decrypted JSON: " + decryptedJson);
            
            if (decryptedJson == null) {
                return ResponseEntity.badRequest().body("No decrypted data found");
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "Encryption test successful",
                "decryptedData", decryptedJson
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error in test: " + e.getMessage());
        }
    }

    // Test endpoint đơn giản không cần encryption
    @PostMapping("/test-simple")
    public ResponseEntity<?> testSimple(@RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(Map.of(
            "message", "Simple test successful",
            "receivedData", data
        ));
    }
}

