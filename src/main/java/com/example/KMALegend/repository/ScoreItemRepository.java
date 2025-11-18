package com.example.KMALegend.repository;

import com.example.KMALegend.entity.ScoreItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ScoreItemRepository extends JpaRepository<ScoreItem, Long> {
    List<ScoreItem> findByScoreBatch_BatchId(Long batchId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM ScoreItem s WHERE s.scoreBatch.batchId = :batchId")
    void deleteByScoreBatch_BatchId(@Param("batchId") Long batchId);
}

