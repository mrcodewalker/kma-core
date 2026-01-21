package com.example.KMALegend.repository;

import com.example.KMALegend.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findAllByFormFormId(Long formId);
    List<Question> findAllByFormFormIdOrderByQuestionOrderAsc(Long formId);
}
