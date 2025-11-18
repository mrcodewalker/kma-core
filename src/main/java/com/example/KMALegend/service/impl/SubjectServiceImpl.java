package com.example.KMALegend.service.impl;

import com.example.KMALegend.dto.SubjectDTO;
import com.example.KMALegend.entity.Subject;
import com.example.KMALegend.repository.SubjectRepository;
import com.example.KMALegend.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {
    private final SubjectRepository subjectRepository;
    @Override
    public List<SubjectDTO> getAllSubjects() {
        return null;
    }

    @Override
    public SubjectDTO getSubjectById(Long id) {
        return null;
    }

    @Override
    public Subject createSubject(Subject subject) {
        if (subjectRepository.findFirstBySubjectName(subject.getSubjectName())!=null){
            return null;
        }
        return subjectRepository.save(subject);
    }

    @Override
    public SubjectDTO updateSubject(Long id, SubjectDTO subjectDTO) {
        return null;
    }

    @Override
    public void deleteSubject(Long id) {

    }
    @Override
    public Subject findSubjectByName(String subjectName) {
        return subjectRepository.findFirstBySubjectName(subjectName);
    }
    @Override
    public Subject findBySubjectId(Long id) {
        Optional<Subject> subject = subjectRepository.findById(id);
        return subject.orElse(null);
    }
    @Override
    public boolean existBySubjectName(String subjectName) {
        return subjectRepository.existBySubjectName(subjectName);
    }
    @Override
    public List<Subject> findAll() {
        return subjectRepository.findAll();
    }

    @Override
    public Subject findBySubjectName(String subjectName) {
        List<Subject> list = this.subjectRepository.findAll();
        for (Subject clone: list){
            if(clone.getSubjectName().equalsIgnoreCase(subjectName)
                    || Normalizer.normalize(clone.getSubjectName(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").equalsIgnoreCase(
                    Normalizer.normalize(subjectName, Normalizer.Form.NFD).replaceAll("\\p{M}", ""))){
                return clone;
            }
        }
        return null;
    }
}
