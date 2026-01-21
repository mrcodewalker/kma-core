package com.example.KMALegend.dto;

import com.example.KMALegend.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionDTO {
    private Long questionId;
    private String questionText;
    private QuestionType questionType;
    private String options;
    private Boolean isRequired;
    private Integer questionOrder;
}
