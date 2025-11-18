package com.example.KMALegend.repository;

import com.example.KMALegend.entity.ScoreBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScoreBatchRepository extends JpaRepository<ScoreBatch, Long> {
    Optional<ScoreBatch> findByStudentCode(String studentCode);
}

