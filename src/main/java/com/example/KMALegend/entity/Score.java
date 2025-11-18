package com.example.KMALegend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "scores")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Score {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(name = "score_text")
    private String scoreText;

    @Column(name = "score_first", nullable = false)
    private Float scoreFirst;

    @Column(name = "score_second", nullable = false)
    private Float scoreSecond;

    @Column(name = "score_final", nullable = false)
    private Float scoreFinal;

    @Column(name = "score_over_rall", nullable = false)
    private Float scoreOverall;

    @Column(name = "semester")
    private String semester;
} 