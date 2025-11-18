package com.example.KMALegend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "semester_ranking")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SemesterRanking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(name = "ranking")
    private Long ranking;

    @Column(name = "gpa", nullable = false)
    private Float gpa;

    @Column(name = "asia_gpa", nullable = false)
    private Float asiaGpa;
    public static Scholarship formData(SemesterRanking ranking){
        return Scholarship.builder()
                .gpa(ranking.getGpa())
                .student(ranking.getStudent())
                .ranking(ranking.getRanking())
                .asiaGpa(ranking.getAsiaGpa())
                .build();
    }
} 