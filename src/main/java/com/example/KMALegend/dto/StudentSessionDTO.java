package com.example.KMALegend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSessionDTO {
    private Long id;
    private String studentCode;
    private String cookieData;
    private String deviceInfo;
    private String userAgent;
    private String location;
    private String ipAddress;
    private LocalDateTime createdAt;
}