package com.example.KMALegend.service;

import com.example.KMALegend.dto.CreateScoreDTO;
import com.example.KMALegend.dto.ScoreDTO;
import com.example.KMALegend.entity.Score;

import java.util.List;
import java.util.Scanner;

public interface ScoreService {
    List<ScoreDTO> getAllScores();
    List<ScoreDTO> getScoresByStudentId(Integer studentId);
    List<ScoreDTO> getScoresByStudentAndSemester(Integer studentId, String semester);
    ScoreDTO getScoreById(Long id);
    List<Score> createScore(CreateScoreDTO score);
    ScoreDTO updateScore(Long id, ScoreDTO scoreDTO);
    void deleteScore(Long id);
} 