package com.example.KMALegend.service.impl;

import com.example.KMALegend.dto.StudentDTO;
import com.example.KMALegend.entity.Student;
import com.example.KMALegend.repository.StudentRepository;
import com.example.KMALegend.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;

    @Override
    public Student findByStudentCode(String studentCode) {
        return studentRepository.findByStudentCode(studentCode);
    }

    @Override
    public Student updateStudentInfo(Student incoming) {
        Student existing = studentRepository.findByStudentCode(incoming.getStudentCode());
        if (existing == null) {
            return null;
        }
        boolean changed = false;
        if (isBlank(existing.getGender()) && !isBlank(incoming.getGender())) {
            existing.setGender(incoming.getGender());
            changed = true;
        }
        if (isBlank(existing.getBirthday()) && !isBlank(incoming.getBirthday())) {
            existing.setBirthday(incoming.getBirthday());
            changed = true;
        }
        if (isBlank(existing.getBirthPlace()) && !isBlank(incoming.getBirthPlace())) {
            existing.setBirthPlace(incoming.getBirthPlace());
            changed = true;
        }
        if (isBlank(existing.getIdCard()) && !isBlank(incoming.getIdCard())) {
            existing.setIdCard(incoming.getIdCard());
            changed = true;
        }
        if (isBlank(existing.getBankAccount()) && !isBlank(incoming.getBankAccount())) {
            existing.setBankAccount(incoming.getBankAccount());
            changed = true;
        }
        if (isBlank(existing.getEnrollSemester()) && !isBlank(incoming.getEnrollSemester())) {
            existing.setEnrollSemester(incoming.getEnrollSemester());
            changed = true;
        }
        if (isBlank(existing.getPhone()) && !isBlank(incoming.getPhone())) {
            existing.setPhone(incoming.getPhone());
            changed = true;
        }
        if (isBlank(existing.getEmail()) && !isBlank(incoming.getEmail())) {
            existing.setEmail(incoming.getEmail());
            changed = true;
        }
        if (isBlank(existing.getStudentName()) && !isBlank(incoming.getStudentName())) {
            existing.setStudentName(incoming.getStudentName());
            changed = true;
        }
        if (isBlank(existing.getStudentClass()) && !isBlank(incoming.getStudentClass())) {
            existing.setStudentClass(incoming.getStudentClass());
            changed = true;
        }
        return changed ? studentRepository.save(existing) : existing;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    @Override
    public List<StudentDTO> getAllStudents() {
        return null;
    }

    @Override
    public StudentDTO getStudentById(Integer id) {
        return null;
    }

    @Override
    public StudentDTO getStudentByCode(String code) {
        return null;
    }

    @Override
    public Student createStudent(Student student) {
        Student clone = studentRepository.findByStudentCode(student.getStudentCode());
        if (clone == null) {
            return studentRepository.save(student);
        }
        return null;
    }

    @Override
    public StudentDTO updateStudent(Integer id, StudentDTO studentDTO) {
        return null;
    }

    @Override
    public void deleteStudent(Integer id) {

    }

    @Override
    public boolean existByStudentCode(String studentCode) {
        return studentRepository.existByStudentCode(studentCode);
    }

    @Override
    public List<StudentDTO> getStudentsByName(String studentName) {
        List<Student> students = this.studentRepository.findStudentsBySimilarName(studentName);
        List<StudentDTO> list = new ArrayList<>();
        for (Student student : students) {
            StudentDTO studentResponse = StudentDTO.builder()
                    .studentCode(student.getStudentCode())
                    .studentName(student.getStudentName())
                    .studentClass(student.getStudentClass())
                    .build();
            list.add(studentResponse);
        }
        return list;
    }
}
