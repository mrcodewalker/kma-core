package com.example.KMALegend.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "score_batches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "student_code", nullable = false)
    private String studentCode;

    @Column(name = "student_name", nullable = false)
    private String studentName;

    @Column(name = "student_class")
    private String studentClass;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @OneToMany(mappedBy = "scoreBatch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<ScoreItem> scoreItems;
}

