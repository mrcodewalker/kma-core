package com.example.KMALegend.service;

import com.example.KMALegend.dto.StudentFailedDTO;
import com.example.KMALegend.entity.StudentFailed;

import java.util.List;

public interface StudentFailedService {
    StudentFailed createStudent(StudentFailedDTO studentFailed);
    List<StudentFailed> findAllRecords();
    List<StudentFailed> findByStudentCode(String studentCode);
}
