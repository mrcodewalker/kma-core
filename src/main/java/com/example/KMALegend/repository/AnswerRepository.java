package com.example.KMALegend.repository;

import com.example.KMALegend.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findAllBySubmissionSubmissionId(Long submissionId);
}
