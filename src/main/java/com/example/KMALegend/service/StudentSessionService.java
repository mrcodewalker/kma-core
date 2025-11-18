package com.example.KMALegend.service;

import com.example.KMALegend.entity.StudentSessions;
import jakarta.servlet.http.HttpServletRequest;

public interface StudentSessionService {
    void saveUserSession(StudentSessions studentSessions, HttpServletRequest request);
}
