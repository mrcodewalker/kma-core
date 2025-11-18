package com.example.KMALegend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.stereotype.Component;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Component
@Data
@Builder
public class StudentFailedDTO {
    @JsonProperty("student_code")
    private String studentCode;
}
