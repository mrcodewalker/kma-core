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
public class SubmissionDTO {
    private Long submissionId;
    private Long formId;
    private Long studentId;
    private LocalDateTime submittedAt;
    private List<AnswerDTO> answers;
}
