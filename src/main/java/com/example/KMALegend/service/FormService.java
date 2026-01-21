package com.example.KMALegend.service;

import com.example.KMALegend.dto.FormDTO;
import com.example.KMALegend.dto.FormSearchCriteria;
import com.example.KMALegend.dto.FormStatisticsDTO;
import com.example.KMALegend.dto.PageResponse;
import java.util.List;

public interface FormService {
    FormDTO createForm(FormDTO formDTO);
    FormDTO getFormById(Long formId);
    void deleteForm(Long formId);
    PageResponse<FormDTO> searchForms(FormSearchCriteria criteria);
    FormStatisticsDTO getFormStatistics(Long formId);
    // void deleteForm(Long formId); // Optional
}
