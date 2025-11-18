package com.example.KMALegend.common.responses;

import com.example.KMALegend.dto.ListScoreDTO;
import com.example.KMALegend.dto.SubjectDTO;
import lombok.*;

import java.util.List;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilterRankingResponse {
    private ListScoreDTO listScoreDTO;
    private List<SubjectDTO> subjectDTOS;
}
