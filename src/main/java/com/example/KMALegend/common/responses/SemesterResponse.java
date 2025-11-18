package com.example.KMALegend.common.responses;

import com.example.KMALegend.dto.RankingDTO;
import com.example.KMALegend.dto.SubjectDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SemesterResponse {
    List<SubjectDTO> subjectDTOS;
    List<RankingDTO> rankingDTOS;
}
