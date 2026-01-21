package com.example.KMALegend.service.impl;

import com.example.KMALegend.entity.StudentSessions;
import com.example.KMALegend.repository.StudentSessionsRepository;
import com.example.KMALegend.service.StudentSessionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class StudentSessionServiceImpl  {
    private final StudentSessionsRepository studentSessionsRepository;
    public void saveUserSession(StudentSessions studentSessions, HttpServletRequest request) {
        studentSessions.setDeviceInfo(request.getHeader("User-Agent"));
        studentSessions.setUserAgent(request.getHeader("User-Agent"));
        studentSessions.setIpAddress(getClientIp(request));
        studentSessions.setLocation(this.getLocationFromIp(getClientIp(request)));
        this.studentSessionsRepository.save(studentSessions);
    }
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0];
        }
        return request.getRemoteAddr();
    }
    private String getLocationFromIp(String ip) {
        try {
            String url = "http://ip-api.com/json/" + ip + "?fields=country,regionName,city";
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(response);
            String country = json.path("country").asText("");
            String region = json.path("regionName").asText("");
            String city = json.path("city").asText("");
            return String.format("%s, %s, %s", city, region, country).replaceAll("(^[ ,]+|[ ,]+$)", "");
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
