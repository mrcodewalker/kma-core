package com.example.KMALegend.controller;

import com.example.KMALegend.encode.DecryptedRequestWrapper;
import com.example.KMALegend.service.impl.LoginServiceImpl;
import com.example.KMALegend.service.impl.StudentSessionServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication APIs")
public class LoginController {
    private final LoginServiceImpl loginService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "Login user", description = "Authenticates a user and returns their profile information")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully authenticated",
                content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "400", description = "Missing username or password")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(HttpServletRequest request) throws IOException {
        try {
            DecryptedRequestWrapper wrapper = new DecryptedRequestWrapper(request, objectMapper);
            Map<String, String> credentials = wrapper.getDecryptedBody(Map.class);
            String username = credentials.get("username");
            String password = credentials.get("password");
            return loginService.login(username, password, request);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Bad Request",
                "message", "Invalid request format or decryption failed",
                "details", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Get virtual calendar", description = "Returns the user's virtual calendar information")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved calendar",
                content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "400", description = "Missing username or password")
    })
    @PostMapping("/virtual-calendar")
    public ResponseEntity<?> getVirtualCalendar(HttpServletRequest request, HttpSession session) throws IOException {
        try {
            DecryptedRequestWrapper wrapper = new DecryptedRequestWrapper(request, objectMapper);
            Map<String, String> credentials = wrapper.getDecryptedBody(Map.class);
            String username = credentials.get("username");
            String password = credentials.get("password");
            return loginService.loginVirtualCalendar(username, password, session, request);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Bad Request",
                "message", "Invalid request format or decryption failed",
                "details", e.getMessage()
            ));
        }
    }
} 