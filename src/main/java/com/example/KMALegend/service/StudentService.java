package com.example.KMALegend.service;

import com.example.KMALegend.dto.StudentDTO;
import com.example.KMALegend.entity.Student;

import java.util.List;

public interface StudentService {
    List<StudentDTO> getAllStudents();
    StudentDTO getStudentById(Integer id);
    StudentDTO getStudentByCode(String code);
    Student createStudent(Student student);
    StudentDTO updateStudent(Integer id, StudentDTO studentDTO);
    void deleteStudent(Integer id);
    boolean existByStudentCode(String studentCode);
    List<StudentDTO> getStudentsByName(String studentName);
    Student findByStudentCode(String studentCode);
} 