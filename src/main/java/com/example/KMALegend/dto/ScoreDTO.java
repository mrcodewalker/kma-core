package com.example.KMALegend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreDTO {
    private Long id;
    private Long studentId;
    private Long subjectId;
    private String scoreText;
    private Float scoreFirst;
    private Float scoreSecond;
    private Float scoreFinal;
    private Float scoreOverall;
    private String semester;
    private String subjectName;
    private Long subjectCredit;
} 