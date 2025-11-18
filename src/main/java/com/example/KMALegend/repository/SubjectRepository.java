package com.example.KMALegend.repository;

import com.example.KMALegend.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    @Query("SELECT s FROM Subject s WHERE s.subjectName = :subjectName")
    Subject findFirstBySubjectName(String subjectName);
    @Query("SELECT COUNT(s) FROM Subject s WHERE s.subjectName = :subjectName")
    boolean existBySubjectName(String subjectName);
} 