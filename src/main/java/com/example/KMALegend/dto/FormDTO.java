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
public class FormDTO {
    private Long formId;
    private String title;
    private String description;
    private Long creatorId;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private List<QuestionDTO> questions;
}
