package com.example.KMALegend.entity;

import com.example.KMALegend.enums.QuestionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id", nullable = false)
    private Form form;

    @Column(name = "question_text", nullable = false)
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    // Storing options as JSON string or comma-separated values for simplicity
    // Example: "Option A,Option B,Option C"
    @Column(name = "options", columnDefinition = "TEXT")
    private String options;

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private Boolean isRequired = false;
    
    @Column(name = "question_order")
    private Integer questionOrder;
}
