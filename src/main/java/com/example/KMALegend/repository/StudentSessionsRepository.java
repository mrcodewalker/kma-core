package com.example.KMALegend.repository;

import com.example.KMALegend.entity.StudentSessions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentSessionsRepository extends JpaRepository<StudentSessions, Long> {
}
