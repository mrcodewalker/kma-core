package com.example.KMALegend.repository;

import com.example.KMALegend.entity.Semester;
import com.example.KMALegend.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {
    List<Semester> findByStudent(Student student);
    @Query("SELECT s FROM Semester s WHERE s.student.studentCode = :studentCode")
    List<Semester> findByStudentCode(@Param("studentCode") String studentCode);
    @Query("SELECT s FROM Semester s WHERE s.student.studentId = :studentId")
    List<Semester> findByStudentId(@Param("studentId") Long studentId);
    @Query("SELECT s FROM Semester s WHERE s.subject.id = :subjectId")
    List<Semester> findBySubjectId(@Param("subjectId") Long subjectId);
    @Query("SELECT DISTINCT s.subject.id FROM Semester s")
    List<Long> findDistinctSubjectIds();
    @Modifying
    @Query("DELETE FROM Semester")
    @Transactional
    void deleteAllScores();
    @Modifying
    @Transactional
    @Query(value = "ALTER TABLE semester AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement();
} 