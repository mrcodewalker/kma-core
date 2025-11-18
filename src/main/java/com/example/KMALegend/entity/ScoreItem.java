package com.example.KMALegend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "score_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    @JsonBackReference
    private ScoreBatch scoreBatch;

    @Column(name = "score_text")
    private String scoreText;

    @Column(name = "score_first", nullable = false)
    private Float scoreFirst;

    @Column(name = "score_second", nullable = false)
    private Float scoreSecond;

    @Column(name = "score_final", nullable = false)
    private Float scoreFinal;

    @Column(name = "score_overall", nullable = false)
    private Float scoreOverall;

    @Column(name = "subject_name", nullable = false)
    private String subjectName;

    @Column(name = "subject_credit", nullable = false)
    private Integer subjectCredit;

    @Column(name = "is_selected", nullable = false)
    private Boolean isSelected;
}

