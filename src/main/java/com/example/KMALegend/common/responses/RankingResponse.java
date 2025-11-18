package com.example.KMALegend.common.responses;

import com.example.KMALegend.entity.Ranking;
import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankingResponse {
    private String studentName;
    private String studentCode;
    private String studentClass;
    private Long ranking;
    private Float gpa;
    private Float asiaGpa;
    public static RankingResponse formData(Ranking ranking){
        return RankingResponse.builder()
                .gpa(ranking.getGpa())
                .studentName(ranking.getStudent().getStudentName())
                .ranking(ranking.getRanking())
                .studentCode(ranking.getStudent().getStudentCode())
                .asiaGpa(ranking.getAsiaGpa())
                .studentClass(ranking.getStudent().getStudentClass())
                .build();
    }
}
