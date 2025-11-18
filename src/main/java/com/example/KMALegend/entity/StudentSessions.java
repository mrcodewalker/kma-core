package com.example.KMALegend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_session")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSessions {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "student_code")
    private String studentCode;
    @Column(name = "cookie_data", nullable = false)
    private String cookieData;
    @Column(name = "device_info")
    private String deviceInfo;
    @Column(name = "user_agent")
    private String userAgent;
    @Column(name = "location")
    private String location;
    @Column(name = "ip_address")
    private String ipAddress;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
