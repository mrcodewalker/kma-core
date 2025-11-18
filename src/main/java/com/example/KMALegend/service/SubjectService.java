package com.example.KMALegend.service;

import com.example.KMALegend.dto.SubjectDTO;
import com.example.KMALegend.entity.Subject;

import java.util.List;

public interface SubjectService {
    List<SubjectDTO> getAllSubjects();
    SubjectDTO getSubjectById(Long id);
    Subject createSubject(Subject subject);
    SubjectDTO updateSubject(Long id, SubjectDTO subjectDTO);
    void deleteSubject(Long id);
    Subject findSubjectByName(String subjectName);
    Subject findBySubjectName(String subjectName);
    List<Subject> findAll();
    boolean existBySubjectName(String subjectName);
    Subject findBySubjectId(Long id);
} 