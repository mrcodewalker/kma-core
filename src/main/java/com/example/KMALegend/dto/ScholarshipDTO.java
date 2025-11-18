package com.example.KMALegend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScholarshipDTO {
    private Long id;
    private Integer studentId;
    private Long ranking;
    private Float gpa;
    private Float asiaGpa;
} 