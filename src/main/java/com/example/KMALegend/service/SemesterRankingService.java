package com.example.KMALegend.service;

import com.example.KMALegend.dto.RankingDTO;
import com.example.KMALegend.dto.SubjectDTO;
import com.example.KMALegend.exception.ResourceNotFoundException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

public interface SemesterRankingService {
    void updateRanking();
    void updateGPA();
    List<RankingDTO> findRanking(String studentCode) throws ResourceNotFoundException;
    List<SubjectDTO> findSubjects(String studentCode);
    List<RankingDTO> getList100Students();
    List<RankingDTO> filterListStudents(String filterCode);
}
