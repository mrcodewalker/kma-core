package com.example.KMALegend.service.impl;

import com.example.KMALegend.entity.StudentSchedule;
import com.example.KMALegend.repository.StudentScheduleRepository;
import com.example.KMALegend.service.StudentScheduleService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StudentScheduleServiceImpl implements StudentScheduleService {

    private static final DateTimeFormatter DMY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final StudentScheduleRepository studentScheduleRepository;
    private final ObjectMapper objectMapper;

    @Override
    public String computeScheduleHash(List<Map<String, String>> scheduleData) {
        if (scheduleData == null || scheduleData.isEmpty()) {
            return "";
        }
        try {
            List<TreeMap<String, String>> normalized = new ArrayList<>();
            for (Map<String, String> item : scheduleData) {
                TreeMap<String, String> sortedMap = new TreeMap<>();
                if (item != null) {
                    item.forEach((k, v) -> sortedMap.put(k, v == null ? "" : v.trim()));
                }
                normalized.add(sortedMap);
            }
            normalized.sort(Comparator
                    .comparing((TreeMap<String, String> m) -> m.getOrDefault("course_code", ""))
                    .thenComparing(m -> m.getOrDefault("course_name", ""))
                    .thenComparing(m -> m.getOrDefault("study_days", ""))
                    .thenComparing(m -> m.getOrDefault("lessons", ""))
                    .thenComparing(m -> m.getOrDefault("study_location", ""))
                    .thenComparing(m -> m.getOrDefault("teacher", "")));

            String canonicalJson = objectMapper.writeValueAsString(normalized);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(canonicalJson.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute schedule hash", e);
        }
    }

    @Override
    public String deriveLabel(String semesterHint, List<Map<String, String>> scheduleData) {
        if (semesterHint != null && !semesterHint.isBlank()) {
            return semesterHint.trim();
        }
        if (scheduleData != null && !scheduleData.isEmpty()) {
            LocalDate minDate = null;
            LocalDate maxDate = null;
            for (Map<String, String> course : scheduleData) {
                String studyDays = course.get("study_days");
                if (studyDays == null || studyDays.isBlank()) {
                    continue;
                }
                for (String token : studyDays.trim().split("\\s+")) {
                    try {
                        LocalDate date = LocalDate.parse(token.trim(), DMY_FORMAT);
                        if (minDate == null || date.isBefore(minDate)) {
                            minDate = date;
                        }
                        if (maxDate == null || date.isAfter(maxDate)) {
                            maxDate = date;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            if (minDate != null && maxDate != null) {
                return "Lịch học " + minDate.format(DMY_FORMAT) + " - " + maxDate.format(DMY_FORMAT);
            }
        }
        return "Lịch học (" + LocalDate.now().format(DMY_FORMAT) + ")";
    }

    @Override
    @Transactional
    public StudentSchedule saveScheduleIfNotEmpty(String studentCode, String label, List<Map<String, String>> scheduleData) {
        if (studentCode == null || studentCode.isBlank() || scheduleData == null || scheduleData.isEmpty()) {
            return null;
        }
        String normalizedCode = studentCode.trim().toUpperCase();
        String hash = computeScheduleHash(scheduleData);
        String resolvedLabel = (label != null && !label.isBlank()) ? label.trim() : deriveLabel(null, scheduleData);

        try {
            String json = objectMapper.writeValueAsString(scheduleData);
            Optional<StudentSchedule> existingOpt = studentScheduleRepository
                    .findByStudentCodeAndScheduleHash(normalizedCode, hash);

            if (existingOpt.isPresent()) {
                StudentSchedule existing = existingOpt.get();
                existing.setLabel(resolvedLabel);
                existing.setCourseCount(scheduleData.size());
                existing.setScheduleJson(json);
                existing.setUpdatedAt(LocalDateTime.now());
                return studentScheduleRepository.save(existing);
            }

            StudentSchedule created = StudentSchedule.builder()
                    .studentCode(normalizedCode)
                    .label(resolvedLabel)
                    .scheduleHash(hash)
                    .scheduleJson(json)
                    .courseCount(scheduleData.size())
                    .build();
            return studentScheduleRepository.save(created);
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist student schedule", e);
        }
    }

    @Override
    public List<Map<String, Object>> getSavedSchedules(String studentCode, String liveHash) {
        if (studentCode == null || studentCode.isBlank()) {
            return List.of();
        }
        String normalizedCode = studentCode.trim().toUpperCase();
        List<StudentSchedule> entities = studentScheduleRepository
                .findByStudentCodeOrderByUpdatedAtDescIdDesc(normalizedCode);

        List<Map<String, Object>> results = new ArrayList<>();
        for (StudentSchedule entity : entities) {
            List<Map<String, String>> parsedSchedule = List.of();
            try {
                parsedSchedule = objectMapper.readValue(
                        entity.getScheduleJson(),
                        new TypeReference<List<Map<String, String>>>() {}
                );
            } catch (Exception ignored) {
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", entity.getId());
            item.put("label", entity.getLabel());
            item.put("course_count", entity.getCourseCount());
            item.put("created_at", entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null);
            item.put("updated_at", entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null);
            item.put("is_live", liveHash != null && !liveHash.isBlank() && liveHash.equals(entity.getScheduleHash()));
            item.put("schedule", parsedSchedule);
            results.add(item);
        }
        return results;
    }
}
