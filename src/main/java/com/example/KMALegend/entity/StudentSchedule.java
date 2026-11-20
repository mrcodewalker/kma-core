package com.example.KMALegend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "student_schedules",
    indexes = {
        @Index(name = "idx_student_schedules_code", columnList = "student_code"),
        @Index(name = "idx_student_schedules_code_hash", columnList = "student_code, schedule_hash")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_code", nullable = false, length = 32)
    private String studentCode;

    @Column(name = "label", nullable = false, length = 255)
    private String label;

    @Column(name = "schedule_hash", nullable = false, length = 64)
    private String scheduleHash;

    @Lob
    @Column(name = "schedule_json", nullable = false, columnDefinition = "LONGTEXT")
    private String scheduleJson;

    @Column(name = "course_count", nullable = false)
    private Integer courseCount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
