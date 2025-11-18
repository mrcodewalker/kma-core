package com.example.KMALegend.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class StudentDTO {
    private Integer studentId;
    private String studentCode;
    private String studentName;
    private String studentClass;
} 