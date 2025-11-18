package com.example.KMALegend.repository;

import com.example.KMALegend.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer> {
    Student findByStudentCode(String studentCode);
    Student findByStudentId(Long studentId);
    List<Student> findByStudentName(String studentName);
    List<Student> findByStudentClass(String studentClass);
    @Query("SELECT COUNT(s) > 0 FROM Student s WHERE s.studentCode = :studentCode")
    boolean existByStudentCode(String studentCode);
    @Query("SELECT s FROM Student s WHERE s.studentName LIKE %:studentName% AND s.studentName IS NOT NULL")
    List<Student> findStudentsBySimilarName(@Param("studentName") String studentName);
    @Query("SELECT DISTINCT SUBSTRING(st.studentCode, 1, 6) FROM Student st")
    List<String> findDistinctClass();
    @Query("SELECT DISTINCT SUBSTRING(st.studentCode, 1, 4) FROM Student st")
    List<String> findDistinctBlockDetail();
    @Query("SELECT DISTINCT SUBSTRING(st.studentCode, 1, 2) FROM Student st")
    List<String> findDistinctMajor();
    @Query("SELECT s FROM Student s WHERE s.studentCode IN (:studentCode)")
    List<Student> findListStudentByStudentCode(@Param("studentCode") List<String> studentCode);
} 