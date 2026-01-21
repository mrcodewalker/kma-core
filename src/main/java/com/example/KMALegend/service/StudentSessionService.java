package com.example.KMALegend.service;

import com.example.KMALegend.dto.StudentSessionDTO;
import com.example.KMALegend.entity.StudentSessions;
import com.example.KMALegend.repository.StudentSessionRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentSessionService {
    
    private final StudentSessionRepository studentSessionRepository;
    private final ModelMapper modelMapper;
    
    public Page<StudentSessionDTO> getStudentSessions(String studentCode, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<StudentSessions> studentSessions = studentSessionRepository
                .findByStudentCodeContaining(studentCode, pageable);
        
        return studentSessions.map(session -> modelMapper.map(session, StudentSessionDTO.class));
    }
}