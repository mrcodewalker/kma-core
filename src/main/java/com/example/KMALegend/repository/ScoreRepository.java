package com.example.KMALegend.repository;

import com.example.KMALegend.entity.Score;
import com.example.KMALegend.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Long> {
    List<Score> findByStudent(Student student);
    List<Score> findByStudentAndSemester(Student student, String semester);
    @Query("SELECT s FROM Score s WHERE s.student.studentId = :studentId")
    List<Score> findByStudentId(@Param("studentId") Long studentId);
    @Query(value = "SELECT * FROM scores s WHERE s.semester = (SELECT subQuery.semester FROM scores subQuery ORDER BY subQuery.id DESC LIMIT 1)", nativeQuery = true)
    List<Score> findScoresWithLatestSemester();
    @Query("SELECT s FROM Score s WHERE s.student.studentCode = :studentCode")
    List<Score> findByStudentCode(@Param("studentCode") String studentCode);
    @Query("SELECT s FROM Score s WHERE s.student.studentCode IN (:studentCode)")
    List<Score> findListScoreByStudentCode(@Param("studentCode") List<String> studentCode);
} 