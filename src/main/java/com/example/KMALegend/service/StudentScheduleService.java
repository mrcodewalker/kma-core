package com.example.KMALegend.service;

import com.example.KMALegend.entity.StudentSchedule;

import java.util.List;
import java.util.Map;

public interface StudentScheduleService {
    String computeScheduleHash(List<Map<String, String>> scheduleData);
    String deriveLabel(String semesterHint, List<Map<String, String>> scheduleData);
    StudentSchedule saveScheduleIfNotEmpty(String studentCode, String label, List<Map<String, String>> scheduleData);
    List<Map<String, Object>> getSavedSchedules(String studentCode, String liveHash);
}
