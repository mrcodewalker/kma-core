package com.example.KMALegend.service;

import com.example.KMALegend.dto.ScoreBatchRequestDTO;
import com.example.KMALegend.entity.ScoreBatch;

public interface ScoreBatchService {
    ScoreBatch createOrUpdateScoreBatch(ScoreBatchRequestDTO requestDTO);
    ScoreBatch getScoreBatchByStudentCode(String studentCode);
    ScoreBatch updateScoreBatchOptimized(ScoreBatchRequestDTO requestDTO);
}

