package com.example.KMALegend.service.impl;

import com.example.KMALegend.entity.StudentSchedule;
import com.example.KMALegend.repository.StudentScheduleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentScheduleServiceImplTest {

    @Mock
    private StudentScheduleRepository studentScheduleRepository;

    private StudentScheduleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StudentScheduleServiceImpl(studentScheduleRepository, new ObjectMapper());
    }

    @Test
    void computeScheduleHash_isOrderIndependent() {
        Map<String, String> c1 = Map.of(
                "course_code", "AT101",
                "course_name", "Toán rời rạc",
                "study_days", "01/09/2026 08/09/2026",
                "lessons", "1,2,3 1,2,3",
                "study_location", "101-A2",
                "teacher", "GV A"
        );
        Map<String, String> c2 = Map.of(
                "course_code", "AT102",
                "course_name", "Lập trình C",
                "study_days", "02/09/2026",
                "lessons", "4,5,6",
                "study_location", "202-A2",
                "teacher", "GV B"
        );

        String hash1 = service.computeScheduleHash(List.of(c1, c2));
        String hash2 = service.computeScheduleHash(List.of(c2, c1));

        assertFalse(hash1.isBlank());
        assertEquals(hash1, hash2);
    }

    @Test
    void deriveLabel_usesHintOrDateRange() {
        List<Map<String, String>> schedule = List.of(
                Map.of("study_days", "05/09/2026 20/11/2026"),
                Map.of("study_days", "01/09/2026 15/12/2026")
        );

        assertEquals("HK1 2026-2027 (01/09/2026 - 15/12/2026)", service.deriveLabel("  HK1 2026-2027 ", schedule));
        assertEquals("Kỳ 01/09/2026 - 15/12/2026", service.deriveLabel(null, schedule));
    }

    @Test
    void saveScheduleIfNotEmpty_skipsEmptyAndDeduplicatesByHash() {
        assertNull(service.saveScheduleIfNotEmpty("DT080231", "HK1", List.of()));
        verifyNoInteractions(studentScheduleRepository);

        List<Map<String, String>> schedule = List.of(
                Map.of(
                        "course_code", "AT101",
                        "course_name", "Môn A",
                        "study_days", "01/09/2026",
                        "lessons", "1,2",
                        "study_location", "101",
                        "teacher", "GV A"
                )
        );
        String expectedHash = service.computeScheduleHash(schedule);
        when(studentScheduleRepository.findByStudentCodeAndScheduleHash("DT080231", expectedHash))
                .thenReturn(Optional.empty());
        when(studentScheduleRepository.save(any(StudentSchedule.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        StudentSchedule saved = service.saveScheduleIfNotEmpty("dt080231", "HK1", schedule);
        assertNotNull(saved);
        assertEquals("DT080231", saved.getStudentCode());
        assertEquals(1, saved.getCourseCount());
        assertEquals(expectedHash, saved.getScheduleHash());

        StudentSchedule existing = StudentSchedule.builder()
                .id(42L)
                .studentCode("DT080231")
                .label("Old")
                .scheduleHash(expectedHash)
                .scheduleJson("[]")
                .courseCount(1)
                .build();
        when(studentScheduleRepository.findByStudentCodeAndScheduleHash("DT080231", expectedHash))
                .thenReturn(Optional.of(existing));

        service.saveScheduleIfNotEmpty("dt080231", "New Label", schedule);
        ArgumentCaptor<StudentSchedule> captor = ArgumentCaptor.forClass(StudentSchedule.class);
        verify(studentScheduleRepository, times(2)).save(captor.capture());
        assertEquals(42L, captor.getValue().getId());
        assertEquals("New Label", captor.getValue().getLabel());
    }

    @Test
    void getSavedSchedules_marksLiveScheduleAndDeserializes() {
        String json = "[{\"course_code\":\"AT101\",\"course_name\":\"Môn A\",\"study_days\":\"01/09/2026\",\"lessons\":\"1,2\",\"study_location\":\"101\",\"teacher\":\"GV\"}]";
        StudentSchedule s1 = StudentSchedule.builder()
                .id(1L)
                .studentCode("DT080231")
                .label("Lịch mới")
                .scheduleHash("hash-live")
                .scheduleJson(json)
                .courseCount(1)
                .createdAt(LocalDateTime.of(2026, 11, 20, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 11, 20, 10, 0))
                .build();
        StudentSchedule s2 = StudentSchedule.builder()
                .id(2L)
                .studentCode("DT080231")
                .label("Lịch cũ")
                .scheduleHash("hash-old")
                .scheduleJson(json)
                .courseCount(1)
                .createdAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                .build();

        when(studentScheduleRepository.findByStudentCodeOrderByUpdatedAtDescIdDesc("DT080231"))
                .thenReturn(List.of(s1, s2));

        List<Map<String, Object>> result = service.getSavedSchedules("dt080231", "hash-live");
        assertEquals(2, result.size());
        assertEquals(Boolean.TRUE, result.get(0).get("is_live"));
        assertEquals(Boolean.FALSE, result.get(1).get("is_live"));
        assertInstanceOf(List.class, result.get(0).get("schedule"));
    }
}
