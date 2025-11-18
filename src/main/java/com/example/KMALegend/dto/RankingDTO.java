package com.example.KMALegend.dto;

import com.example.KMALegend.entity.Ranking;
import com.example.KMALegend.entity.Scholarship;
import com.example.KMALegend.entity.SemesterRanking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankingDTO {
    private Long id;
    private Long studentId;
    private String studentCode;
    private String studentName;
    private String studentClass;
    private Long ranking;
    private Float gpa;
    private Float asiaGpa;
    public static RankingDTO formData(SemesterRanking ranking){
        return RankingDTO.builder()
                .gpa(ranking.getGpa())
                .studentName(ranking.getStudent().getStudentName())
                .ranking(ranking.getRanking())
                .studentCode(ranking.getStudent().getStudentCode())
                .asiaGpa(ranking.getAsiaGpa())
                .studentClass(ranking.getStudent().getStudentClass())
                .build();
    }
    public static RankingDTO convert(Scholarship ranking){
        return RankingDTO.builder()
                .gpa(ranking.getGpa())
                .studentName(ranking.getStudent().getStudentName())
                .ranking(ranking.getRanking())
                .studentCode(ranking.getStudent().getStudentCode())
                .asiaGpa(ranking.getAsiaGpa())
                .studentClass(ranking.getStudent().getStudentClass())
                .build();
    }
    public static RankingDTO mappingFromObject(Object[] objects){
        Long ranking = (Long) objects[0];
        String studentCodeFromDb = (String) objects[1];
        String studentName = (String) objects[2];
        String studentClass = (String) objects[3];
        Float gpa = (Float) objects[4];
        Float asiaGpa = (Float) objects[5];

        return RankingDTO.builder()
                .ranking(ranking)
                .studentCode(studentCodeFromDb)
                .studentName(studentName)
                .studentClass(studentClass)
                .gpa(gpa)
                .asiaGpa(asiaGpa)
                .build();
    }
} 