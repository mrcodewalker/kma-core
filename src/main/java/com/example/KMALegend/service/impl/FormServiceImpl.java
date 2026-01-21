package com.example.KMALegend.service.impl;

import com.example.KMALegend.dto.*;
import com.example.KMALegend.entity.Answer;
import com.example.KMALegend.entity.Form;
import com.example.KMALegend.entity.Question;
import com.example.KMALegend.entity.Submission;
import com.example.KMALegend.enums.QuestionType;
import com.example.KMALegend.exception.ResourceNotFoundException;
import com.example.KMALegend.repository.AnswerRepository;
import com.example.KMALegend.repository.FormRepository;
import com.example.KMALegend.repository.QuestionRepository;
import com.example.KMALegend.repository.SubmissionRepository;
import com.example.KMALegend.service.FormService;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormServiceImpl implements FormService {

    private final FormRepository formRepository;
    private final QuestionRepository questionRepository;
    private final SubmissionRepository submissionRepository;
    private final AnswerRepository answerRepository;

    @Override
    @Transactional
    public FormDTO createForm(FormDTO formDTO) {
        Form form = Form.builder()
                .title(formDTO.getTitle())
                .description(formDTO.getDescription())
                .creatorId(formDTO.getCreatorId())
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .build();
        
        form = formRepository.save(form);

        List<QuestionDTO> questionDTOs = formDTO.getQuestions();
        if (questionDTOs != null) {
            List<Question> questions = new ArrayList<>();
            for (QuestionDTO qDto : questionDTOs) {
                Question question = Question.builder()
                        .form(form)
                        .questionText(qDto.getQuestionText())
                        .questionType(qDto.getQuestionType())
                        .options(qDto.getOptions())
                        .isRequired(qDto.getIsRequired())
                        .questionOrder(qDto.getQuestionOrder())
                        .build();
                questions.add(question);
            }
            questionRepository.saveAll(questions);
        }

        return getFormById(form.getFormId());
    }

    @Override
    public FormDTO getFormById(Long formId) {
        Form form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found with id: " + formId));
        
        List<Question> questions = questionRepository.findAllByFormFormIdOrderByQuestionOrderAsc(formId);
        List<QuestionDTO> questionDTOs = questions.stream().map(this::mapToQuestionDTO).collect(Collectors.toList());

        return FormDTO.builder()
                .formId(form.getFormId())
                .title(form.getTitle())
                .description(form.getDescription())
                .creatorId(form.getCreatorId())
                .isActive(form.getIsActive())
                .createdAt(form.getCreatedAt())
                .questions(questionDTOs)
                .build();
    }

    @Override
    public void deleteForm(Long formId) {
        Form form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found with id: " + formId));
        formRepository.delete(form);
    }
    
    @Override
    public PageResponse<FormDTO> searchForms(FormSearchCriteria criteria) {
        Pageable pageable = PageRequest.of(criteria.getPageIndex(), criteria.getPageSize(), 
            Sort.by(StringUtils.hasText(criteria.getSortDirection()) && criteria.getSortDirection().equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC,
            StringUtils.hasText(criteria.getSortBy()) ? criteria.getSortBy() : "createdAt"));

        Specification<Form> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(criteria.getKeyword())) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + criteria.getKeyword().toLowerCase() + "%"));
            }
            if (criteria.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), criteria.getIsActive()));
            }
            if (criteria.getCreatorId() != null) {
                predicates.add(cb.equal(root.get("creatorId"), criteria.getCreatorId()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Form> page = formRepository.findAll(spec, pageable);
        List<FormDTO> content = page.getContent().stream().map(form -> FormDTO.builder()
                .formId(form.getFormId())
                .title(form.getTitle())
                .description(form.getDescription())
                .creatorId(form.getCreatorId())
                .isActive(form.getIsActive())
                .createdAt(form.getCreatedAt())
                .build()).collect(Collectors.toList());

        return PageResponse.<FormDTO>builder()
                .content(content)
                .pageIndex(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    @Override
    public FormStatisticsDTO getFormStatistics(Long formId) {
        Form form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found with id: " + formId));

        List<Question> questions = questionRepository.findAllByFormFormIdOrderByQuestionOrderAsc(formId);
        List<Submission> submissions = submissionRepository.findAllByFormFormId(formId);
        long totalSubmissions = submissions.size();

        List<QuestionStatisticDTO> questionStats = questions.stream().map(question -> {
            QuestionStatisticDTO stat = QuestionStatisticDTO.builder()
                    .questionId(question.getQuestionId())
                    .questionText(question.getQuestionText())
                    .questionType(question.getQuestionType())
                    .totalAnswers(0L) // Default
                    .build();
            return stat;
        }).collect(Collectors.toList());
        
        if (!submissions.isEmpty()) {
            List<Long> submissionIds = submissions.stream().map(Submission::getSubmissionId).collect(Collectors.toList());
            List<Answer> allAnswers = answerRepository.findAll().stream()
                    .filter(a -> submissionIds.contains(a.getSubmission().getSubmissionId()))
                    .collect(Collectors.toList());
            
            Map<Long, List<Answer>> answersByQuestion = allAnswers.stream()
                    .collect(Collectors.groupingBy(a -> a.getQuestion().getQuestionId()));

            questionStats.forEach(stat -> {
                List<Answer> qAnswers = answersByQuestion.getOrDefault(stat.getQuestionId(), new ArrayList<>());
                stat.setTotalAnswers((long) qAnswers.size());

                if (stat.getQuestionType() == QuestionType.MULTIPLE_CHOICE || 
                    stat.getQuestionType() == QuestionType.CHECKBOXES || 
                    stat.getQuestionType() == QuestionType.DROPDOWN) {
                    
                    Map<String, Long> counts = qAnswers.stream()
                            .map(Answer::getAnswerText)
                            .flatMap(text -> {
                                if (stat.getQuestionType() == QuestionType.CHECKBOXES) {
                                    return Arrays.stream(new String[]{text}); 
                                }
                                return Arrays.stream(new String[]{text});
                            })
                            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
                    stat.setAnswerCounts(counts);
                }
            });
        }

        return FormStatisticsDTO.builder()
                .formId(form.getFormId())
                .title(form.getTitle())
                .totalSubmissions(totalSubmissions)
                .questionStatistics(questionStats)
                .build();
    }

    private QuestionDTO mapToQuestionDTO(Question question) {
        return QuestionDTO.builder()
                .questionId(question.getQuestionId())
                .questionText(question.getQuestionText())
                .questionType(question.getQuestionType())
                .options(question.getOptions())
                .isRequired(question.getIsRequired())
                .questionOrder(question.getQuestionOrder())
                .build();
    }
}
