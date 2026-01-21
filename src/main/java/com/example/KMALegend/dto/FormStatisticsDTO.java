package com.example.KMALegend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormStatisticsDTO {
    private Long formId;
    private String title;
    private Long totalSubmissions;
    private List<QuestionStatisticDTO> questionStatistics;
}
