package com.example.KMALegend.service;

import com.example.KMALegend.dto.PageResponse;
import com.example.KMALegend.dto.SubmissionDTO;
import com.example.KMALegend.dto.SubmissionSearchCriteria;
import java.util.List;

public interface SubmissionService {
    SubmissionDTO submitForm(SubmissionDTO submissionDTO);
    List<SubmissionDTO> getSubmissionsByFormId(Long formId);
    PageResponse<SubmissionDTO> searchSubmissions(SubmissionSearchCriteria criteria);
    SubmissionDTO getSubmissionById(Long submissionId);
    void deleteSubmission(Long submissionId, Long studentId);
}
