package com.example.KMALegend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreBatchRequestDTO {
    private StudentInfoDTO studentInfo;
    private List<ScoreItemDTO> scores;
    private LocalDateTime lastUpdated;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StudentInfoDTO {
        private Long studentId;
        private String studentCode;
        private String studentName;
        private String studentClass;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScoreItemDTO {
        private String scoreText;
        private Float scoreFirst;
        private Float scoreSecond;
        private Float scoreFinal;
        private Float scoreOverall;
        private String subjectName;
        private Integer subjectCredit;
        private Boolean isSelected;
    }
}

