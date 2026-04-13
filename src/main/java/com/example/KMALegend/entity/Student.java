package com.example.KMALegend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "student_code", nullable = false)
    private String studentCode;

    @Column(name = "student_name", nullable = false)
    private String studentName;

    @Column(name = "student_class")
    private String studentClass;

    @Column(name = "birthday")
    private String birthday;

    @Column(name = "birth_place")
    private String birthPlace;

    @Column(name = "id_card")
    private String idCard;

    @Column(name = "bank_account")
    private String bankAccount;

    @Column(name = "enroll_semester")
    private String enrollSemester;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "gender")
    private String gender;
}
