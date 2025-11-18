package com.example.KMALegend.service.impl;

import com.example.KMALegend.dto.StudentFailedDTO;
import com.example.KMALegend.entity.StudentFailed;
import com.example.KMALegend.repository.StudentFailedRepository;
import com.example.KMALegend.service.StudentFailedService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentFailedServiceImpl implements StudentFailedService {
    private final StudentFailedRepository studentFailedRepository;
    @Override
    public StudentFailed createStudent(StudentFailedDTO studentFailed) {
        return this.studentFailedRepository.save(
                StudentFailed.builder()
                        .studentCode(studentFailed.getStudentCode())
                        .build());
    }

    @Override
    public List<StudentFailed> findAllRecords() {
        return this.studentFailedRepository.findAll();
    }

    @Override
    public List<StudentFailed> findByStudentCode(String studentCode) {
        return this.studentFailedRepository.findByStudentCode(studentCode);
    }
}
