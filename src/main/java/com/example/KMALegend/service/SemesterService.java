package com.example.KMALegend.service;

import com.example.KMALegend.dto.CreateScoreDTO;
import com.example.KMALegend.dto.RankingDTO;
import com.example.KMALegend.dto.SubjectDTO;
import com.example.KMALegend.entity.Semester;

import java.util.List;

public interface SemesterService {
    Semester createSemester(Semester semester);
    List<Semester> findAll();
    void updateRanking();
    void deleteAll();
    List<RankingDTO> getScholarship(String studentCode);
    List<Semester> createNewScore(CreateScoreDTO scoreDTO);
    List<SubjectDTO> subjectResponse();
}
