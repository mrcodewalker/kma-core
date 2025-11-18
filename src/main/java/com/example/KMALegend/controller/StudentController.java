package com.example.KMALegend.controller;

import com.example.KMALegend.dto.StudentDTO;
import com.example.KMALegend.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Tag(name = "Students", description = "Student management APIs")
public class StudentController {
    private final StudentService studentService;

    @Operation(summary = "Get all students", description = "Returns a list of all students")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved students",
                content = @Content(schema = @Schema(implementation = StudentDTO.class))),
        @ApiResponse(responseCode = "404", description = "No students found")
    })
    @GetMapping
    public ResponseEntity<?> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudents());
    }

    @Operation(summary = "Get student by code", description = "Returns a student by their student code")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Student found",
                content = @Content(schema = @Schema(implementation = StudentDTO.class))),
        @ApiResponse(responseCode = "404", description = "Student not found")
    })
    @GetMapping("/{studentCode}")
    public ResponseEntity<?> getStudentByCode(
        @Parameter(description = "Student code to search for") 
        @PathVariable String studentCode
    ) {
        return ResponseEntity.ok(studentService.getStudentByCode(studentCode));
    }
}
