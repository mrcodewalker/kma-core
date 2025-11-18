package com.example.KMALegend.controller;

import com.example.KMALegend.dto.SubjectDTO;
import com.example.KMALegend.entity.Subject;
import com.example.KMALegend.service.SubjectService;
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
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
@Tag(name = "Subjects", description = "Subject management APIs")
public class SubjectController {
    private final SubjectService subjectService;

    @Operation(summary = "Get all subjects", description = "Returns a list of all subjects")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved subjects",
                content = @Content(schema = @Schema(implementation = SubjectDTO.class))),
        @ApiResponse(responseCode = "404", description = "No subjects found")
    })
    @GetMapping
    public ResponseEntity<?> getAllSubjects() {
        return ResponseEntity.ok(subjectService.getAllSubjects());
    }

    @Operation(summary = "Get subject by ID", description = "Returns a subject by its ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subject found",
                content = @Content(schema = @Schema(implementation = SubjectDTO.class))),
        @ApiResponse(responseCode = "404", description = "Subject not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getSubjectById(
        @Parameter(description = "Subject ID") 
        @PathVariable Long id
    ) {
        return ResponseEntity.ok(subjectService.getSubjectById(id));
    }

    @Operation(summary = "Create subject", description = "Creates a new subject")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subject created successfully",
                content = @Content(schema = @Schema(implementation = SubjectDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping
    public ResponseEntity<?> createSubject(@RequestBody Subject subject) {
        return ResponseEntity.ok(subjectService.createSubject(subject));
    }

    @Operation(summary = "Update subject", description = "Updates an existing subject")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subject updated successfully"),
        @ApiResponse(responseCode = "404", description = "Subject not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSubject(
        @Parameter(description = "Subject ID") 
        @PathVariable Long id,
        @RequestBody SubjectDTO subjectDTO
    ) {
        subjectService.updateSubject(id, subjectDTO);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete subject", description = "Deletes a subject")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subject deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Subject not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSubject(
        @Parameter(description = "Subject ID") 
        @PathVariable Long id
    ) {
        subjectService.deleteSubject(id);
        return ResponseEntity.ok().build();
    }
}
