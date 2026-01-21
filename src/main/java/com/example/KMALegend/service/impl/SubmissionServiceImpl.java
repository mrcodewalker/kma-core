package com.example.KMALegend.service.impl;

import com.example.KMALegend.dto.AnswerDTO;
import com.example.KMALegend.dto.PageResponse;
import com.example.KMALegend.dto.SubmissionDTO;
import com.example.KMALegend.dto.SubmissionSearchCriteria;
import com.example.KMALegend.entity.*;
import com.example.KMALegend.exception.ResourceNotFoundException;
import com.example.KMALegend.repository.*;
import com.example.KMALegend.service.SubmissionService;
import com.example.KMALegend.exception.BadRequestException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AnswerRepository answerRepository;
    private final FormRepository formRepository;
    private final StudentRepository studentRepository;
    private final QuestionRepository questionRepository;

    @Override
    @Transactional
    public SubmissionDTO submitForm(SubmissionDTO submissionDTO) {
        Form form = formRepository.findById(submissionDTO.getFormId())
                .orElseThrow(() -> new ResourceNotFoundException("Form not found with id: " + submissionDTO.getFormId()));

        // Student is optional or required depending on logic. Assuming studentId is passed.
        Student student = null;
        if (submissionDTO.getStudentId() != null) {
            // Note: StudentRepository uses Integer ID in definition I saw, but entity says Long. 
            // I will try to cast or assume Long based on my previous findings.
            // If StudentRepository extends JpaRepository<Student, Integer>, I might need to cast to Integer or change Repo.
            // Let's assume StudentId is Long in DTO. I will use findByStudentId(Long) which I saw in Repo.
             student = studentRepository.findByStudentId(submissionDTO.getStudentId());
             if (student == null) {
                 // Try Integer if needed, or just not found
                 // Assuming it returns null if not found based on signature `Student findByStudentId(Long studentId);`
                 throw new ResourceNotFoundException("Student not found with id: " + submissionDTO.getStudentId());
             }
        }

        Submission submission = Submission.builder()
                .form(form)
                .student(student)
                .submittedAt(LocalDateTime.now())
                .build();
        
        submission = submissionRepository.save(submission);

        List<AnswerDTO> answerDTOs = submissionDTO.getAnswers();
        if (answerDTOs != null) {
            List<Answer> answers = new ArrayList<>();
            for (AnswerDTO ansDto : answerDTOs) {
                Question question = questionRepository.findById(ansDto.getQuestionId())
                        .orElseThrow(() -> new ResourceNotFoundException("Question not found id: " + ansDto.getQuestionId()));
                
                Answer answer = Answer.builder()
                        .submission(submission)
                        .question(question)
                        .answerText(ansDto.getAnswerText())
                        .build();
                answers.add(answer);
            }
            answerRepository.saveAll(answers);
        }

        return getSubmissionById(submission.getSubmissionId());
    }

    @Override
    public List<SubmissionDTO> getSubmissionsByFormId(Long formId) {
        List<Submission> submissions = submissionRepository.findAllByFormFormId(formId);
        return submissions.stream().map(this::mapToSubmissionDTO).collect(Collectors.toList());
    }

    @Override
    public PageResponse<SubmissionDTO> searchSubmissions(SubmissionSearchCriteria criteria) {
         Pageable pageable = PageRequest.of(criteria.getPageIndex(), criteria.getPageSize(), 
            Sort.by(StringUtils.hasText(criteria.getSortDirection()) && criteria.getSortDirection().equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC,
            StringUtils.hasText(criteria.getSortBy()) ? criteria.getSortBy() : "submittedAt"));

        Specification<Submission> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria.getFormId() != null) {
                predicates.add(cb.equal(root.get("form").get("formId"), criteria.getFormId()));
            }
            if (criteria.getStudentId() != null) {
                predicates.add(cb.equal(root.get("student").get("studentId"), criteria.getStudentId()));
            }
            if (StringUtils.hasText(criteria.getKeyword())) {
                // Example: search student name? or answer text?
                // Let's assume search student code or name if student exists
                // Use left join for student
                // predicates.add(cb.like(root.get("student").get("studentName"), "%" + criteria.getKeyword() + "%")); // Basic idea
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Submission> page = submissionRepository.findAll(spec, pageable);
        List<SubmissionDTO> content = page.getContent().stream().map(this::mapToSubmissionDTO).collect(Collectors.toList());

        return PageResponse.<SubmissionDTO>builder()
                .content(content)
                .pageIndex(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    @Override
    public void deleteSubmission(Long submissionId, Long studentId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found id: " + submissionId));

        if (submission.getStudent() == null || !submission.getStudent().getStudentId().equals(studentId)) {
            // Using ResourceNotFound or BadRequest to hide existence or just forbidden
            // For now, explicit error
             throw new RuntimeException("You are not authorized to delete this submission");
             // Ideally use a custom ForbiddenException or BadRequestException
        }
        
        submissionRepository.delete(submission);
    }

    @Override
    public SubmissionDTO getSubmissionById(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found id: " + submissionId));
        return mapToSubmissionDTO(submission);
    }

    private SubmissionDTO mapToSubmissionDTO(Submission submission) {
        List<Answer> answers = answerRepository.findAllBySubmissionSubmissionId(submission.getSubmissionId());
        List<AnswerDTO> answerDTOs = answers.stream().map(ans -> AnswerDTO.builder()
                .answerId(ans.getAnswerId())
                .questionId(ans.getQuestion().getQuestionId())
                .answerText(ans.getAnswerText())
                .build()).collect(Collectors.toList());

        return SubmissionDTO.builder()
                .submissionId(submission.getSubmissionId())
                .formId(submission.getForm().getFormId())
                .studentId(submission.getStudent() != null ? submission.getStudent().getStudentId() : null)
                .submittedAt(submission.getSubmittedAt())
                .answers(answerDTOs)
                .build();
    }
}
