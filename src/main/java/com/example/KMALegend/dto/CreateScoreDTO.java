package com.example.KMALegend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Component
@Data
@Builder
public class CreateScoreDTO {
    @JsonProperty("list_student")
    private List<String> listStudent;
    @JsonProperty("subject_name")
    private String subjectName;

    @JsonProperty("score_text")
    private String scoreText;

    @JsonProperty("score_first")
    private Float scoreFirst;

    @JsonProperty("score_second")
    private Float scoreSecond;

    @JsonProperty("score_final")
    private Float scoreFinal;

    @JsonProperty("score_over_rall")
    private Float scoreOverall;
}
