package com.example.KMALegend.repository;

import com.example.KMALegend.entity.StudentFailed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentFailedRepository extends JpaRepository<StudentFailed, Long> {
    List<StudentFailed> findAll();
    List<StudentFailed> findByStudentCode(String studentCode);
    @Modifying
    @Query("DELETE FROM StudentFailed")
    @Transactional
    void deleteAllScores();
    @Modifying
    @Transactional
    @Query(value = "ALTER TABLE student_failed AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement();
} 