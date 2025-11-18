package com.example.KMALegend.service;

import com.example.KMALegend.dto.RankingDTO;
import java.util.List;

public interface RankingService {
    List<RankingDTO> getAllRankings();
    void updateGPA() throws Exception;
    RankingDTO getRankingById(Long id);
    RankingDTO getRankingByStudentCode(String studentCode);
    RankingDTO getRankingByStudentId(Long studentId);
    RankingDTO createRanking(RankingDTO rankingDTO);
    RankingDTO updateRanking(Long id, RankingDTO rankingDTO);
    void deleteRanking(Long id);
} 