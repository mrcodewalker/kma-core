package com.example.KMALegend.repository;

import com.example.KMALegend.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long>, JpaSpecificationExecutor<Submission> {
    List<Submission> findAllByFormFormId(Long formId);
    List<Submission> findAllByStudentStudentId(Long studentId);
}
