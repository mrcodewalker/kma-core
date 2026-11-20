package com.example.KMALegend.controller;

import com.example.KMALegend.encode.DecryptedRequestWrapper;
import com.example.KMALegend.service.StudentScheduleService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
@CrossOrigin(origins = {"https://kmalegend.click", "https://kma-legend.click", "http://localhost:4200"})
@Tag(name = "Student Schedules", description = "Saved student schedule history APIs")
public class StudentScheduleController {

    private final StudentScheduleService studentScheduleService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "Sync and get saved schedules by student",
            description = "Optionally saves a non-empty schedule from client session, then returns all saved schedule periods for the student")
    @PostMapping("/by-student")
    public ResponseEntity<?> getSavedSchedulesByStudent(HttpServletRequest request) {
        try {
            DecryptedRequestWrapper wrapper = new DecryptedRequestWrapper(request, objectMapper);
            String decryptedJson = wrapper.getDecryptedJson();
            if (decryptedJson == null || decryptedJson.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "code", "400",
                        "message", "Missing decrypted request body"
                ));
            }

            String trimmed = decryptedJson.trim();
            String studentCode;
            String label = null;
            List<Map<String, String>> incomingSchedule = null;

            if (trimmed.startsWith("{")) {
                JsonNode node = objectMapper.readTree(trimmed);
                studentCode = node.path("studentCode").asText("").trim();
                if (node.hasNonNull("label")) {
                    label = node.path("label").asText("").trim();
                }
                JsonNode scheduleNode = node.get("schedule");
                if (scheduleNode != null && scheduleNode.isArray() && !scheduleNode.isEmpty()) {
                    incomingSchedule = objectMapper.convertValue(
                            scheduleNode,
                            new TypeReference<List<Map<String, String>>>() {}
                    );
                }
            } else {
                studentCode = trimmed;
            }

            if (studentCode.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "code", "400",
                        "message", "studentCode is required"
                ));
            }

            String liveHash = null;
            if (incomingSchedule != null && !incomingSchedule.isEmpty()) {
                String resolvedLabel = studentScheduleService.deriveLabel(label, incomingSchedule);
                studentScheduleService.saveScheduleIfNotEmpty(studentCode, resolvedLabel, incomingSchedule);
                liveHash = studentScheduleService.computeScheduleHash(incomingSchedule);
            }

            List<Map<String, Object>> savedSchedules = studentScheduleService.getSavedSchedules(studentCode, liveHash);
            return ResponseEntity.ok(Map.of(
                    "code", "200",
                    "message", "OK",
                    "data", Map.of(
                            "saved_schedules", savedSchedules
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "400",
                    "message", "Failed to load saved schedules: " + e.getMessage()
            ));
        }
    }
}
