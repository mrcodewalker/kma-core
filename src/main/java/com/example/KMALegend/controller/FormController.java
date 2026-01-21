package com.example.KMALegend.controller;

import com.example.KMALegend.dto.FormDTO;
import com.example.KMALegend.dto.FormSearchCriteria;
import com.example.KMALegend.dto.FormStatisticsDTO;
import com.example.KMALegend.dto.PageResponse;
import com.example.KMALegend.service.FormService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forms")
@RequiredArgsConstructor
@CrossOrigin
public class FormController {

    private final FormService formService;

    @PostMapping
    public ResponseEntity<FormDTO> createForm(@RequestBody FormDTO formDTO) {
        return ResponseEntity.ok(formService.createForm(formDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteForm(@PathVariable Long id) {
        formService.deleteForm(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormDTO> getFormById(@PathVariable Long id) {
        return ResponseEntity.ok(formService.getFormById(id));
    }

    @PostMapping("/search")
    public ResponseEntity<PageResponse<FormDTO>> searchForms(@RequestBody FormSearchCriteria criteria) {
        return ResponseEntity.ok(formService.searchForms(criteria));
    }

    @GetMapping("/{id}/statistics")
    public ResponseEntity<FormStatisticsDTO> getFormStatistics(@PathVariable Long id) {
        return ResponseEntity.ok(formService.getFormStatistics(id));
    }
}
