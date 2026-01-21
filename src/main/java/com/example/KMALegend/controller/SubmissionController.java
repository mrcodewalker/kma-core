package com.example.KMALegend.controller;

import com.example.KMALegend.dto.PageResponse;
import com.example.KMALegend.dto.SubmissionDTO;
import com.example.KMALegend.dto.SubmissionSearchCriteria;
import com.example.KMALegend.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
@CrossOrigin
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    public ResponseEntity<SubmissionDTO> submitForm(@RequestBody SubmissionDTO submissionDTO) {
        return ResponseEntity.ok(submissionService.submitForm(submissionDTO));
    }

    @GetMapping("/form/{formId}")
    public ResponseEntity<List<SubmissionDTO>> getSubmissionsByFormId(@PathVariable Long formId) {
        return ResponseEntity.ok(submissionService.getSubmissionsByFormId(formId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubmissionDTO> getSubmissionById(@PathVariable Long id) {
        return ResponseEntity.ok(submissionService.getSubmissionById(id));
    }

    @PostMapping("/search")
    public ResponseEntity<PageResponse<SubmissionDTO>> searchSubmissions(@RequestBody SubmissionSearchCriteria criteria) {
        return ResponseEntity.ok(submissionService.searchSubmissions(criteria));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubmission(@PathVariable Long id, @RequestParam Long studentId) {
        submissionService.deleteSubmission(id, studentId);
        return ResponseEntity.noContent().build();
    }
}
