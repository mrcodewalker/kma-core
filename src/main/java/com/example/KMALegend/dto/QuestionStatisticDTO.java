package com.example.KMALegend.dto;

import com.example.KMALegend.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionStatisticDTO {
    private Long questionId;
    private String questionText;
    private QuestionType questionType;
    // For Choice/Checkboxes: Option -> Count
    private Map<String, Long> answerCounts;
    // For Text: Check logic (maybe total count or recent answers)
    private Long totalAnswers;
}
