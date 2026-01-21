package com.example.KMALegend.repository;

import com.example.KMALegend.entity.StudentSessions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentSessionRepository extends JpaRepository<StudentSessions, Long> {
    
    @Query("SELECT s FROM StudentSessions s WHERE " +
           "(:studentCode IS NULL OR s.studentCode LIKE %:studentCode%) " +
           "ORDER BY s.createdAt DESC")
    Page<StudentSessions> findByStudentCodeContaining(
            @Param("studentCode") String studentCode, 
            Pageable pageable);
}