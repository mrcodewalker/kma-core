package com.example.KMALegend.service;

import com.example.KMALegend.dto.ScholarshipDTO;
import java.util.List;

public interface ScholarshipService {
    List<ScholarshipDTO> getAllScholarships();
    ScholarshipDTO getScholarshipById(Long id);
    ScholarshipDTO getScholarshipByStudentId(Integer studentId);
    ScholarshipDTO createScholarship(ScholarshipDTO scholarshipDTO);
    ScholarshipDTO updateScholarship(Long id, ScholarshipDTO scholarshipDTO);
    void deleteScholarship(Long id);
} 