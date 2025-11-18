package com.example.KMALegend.service.impl;

import com.example.KMALegend.dto.ScholarshipDTO;
import com.example.KMALegend.service.ScholarshipService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScholarshipServiceImpl implements ScholarshipService {
    @Override
    public List<ScholarshipDTO> getAllScholarships() {
        return null;
    }

    @Override
    public ScholarshipDTO getScholarshipById(Long id) {
        return null;
    }

    @Override
    public ScholarshipDTO getScholarshipByStudentId(Integer studentId) {
        return null;
    }

    @Override
    public ScholarshipDTO createScholarship(ScholarshipDTO scholarshipDTO) {
        return null;
    }

    @Override
    public ScholarshipDTO updateScholarship(Long id, ScholarshipDTO scholarshipDTO) {
        return null;
    }

    @Override
    public void deleteScholarship(Long id) {

    }
}
