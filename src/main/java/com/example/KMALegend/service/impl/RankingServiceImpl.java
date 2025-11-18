package com.example.KMALegend.service.impl;

import com.example.KMALegend.common.responses.FilterRankingResponse;
import com.example.KMALegend.common.responses.RankingResponse;
import com.example.KMALegend.dto.ListScoreDTO;
import com.example.KMALegend.dto.RankingDTO;
import com.example.KMALegend.dto.ScoreDTO;
import com.example.KMALegend.dto.StudentDTO;
import com.example.KMALegend.entity.*;
import com.example.KMALegend.repository.*;
import com.example.KMALegend.service.RankingService;
import com.example.KMALegend.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {
    private final RankingRepository rankingRepository;
    private final StudentRepository studentRepository;
    private final SubjectService subjectService;
    private final StudentFailedRepository studentFailedRepository;
    private final ScoreRepository scoreRepository;
    private final SemesterRepository semesterRepository;
    private final SemesterRankingRepository semesterRankingRepository;
    private final ScholarshipRepository scholarshipRepository;

    // Helper method to validate student code prefix and number
    private boolean isValidStudentCode(String studentCode) {
        if (studentCode == null || studentCode.length() < 4) return false;
        
        String prefix = studentCode.substring(0, 2);
        try {
            int number = Integer.parseInt(studentCode.substring(2, 4));
            
            return switch (prefix) {
                case "CT" -> number >= 5;
                case "DT" -> number >= 4;
                case "AT" -> number >= 17;
                default -> false;
            };
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void updateGPA() throws Exception {
        List<Student> studentList = studentRepository.findAll();

        List<Subject> allSubjects = subjectService.findAll();

        Map<String, Subject> subjectMap = new HashMap<>();
        for (Subject subject : allSubjects) {
            if (subject != null && subject.getSubjectName() != null) {
                subjectMap.put(subject.getSubjectName(), subject);
            }
        }

        List<Ranking> newRankings = new ArrayList<>();

        for (Student student : studentList) {
            try {
                float gpa = 0f;
                int count = 0;
                int subjects = 0;

                List<Score> scoreList = this.scoreRepository.findByStudentCode(student.getStudentCode());
                if (scoreList != null && !scoreList.isEmpty()) {
                    for (Score score : scoreList) {
                        if (score.getSubject() == null || score.getSubject().getSubjectName() == null) continue;

                        if (score.getSubject().getSubjectName().contains("Giáo dục thể chất")
                        || score.getSubject().getSubjectName().contains("Thực hành vật")) {
                            subjects++;
                            continue;
                        }

                        Subject subject = subjectMap.get(score.getSubject().getSubjectName());
                        if (subject == null) continue;

                        if (score.getScoreText() == null) continue;

                        float scoreValue;
                        switch (score.getScoreText()) {
                            case "A+":
                                scoreValue = 4.0f;
                                break;
                            case "A":
                                scoreValue = 3.8f;
                                break;
                            case "B+":
                                scoreValue = 3.5f;
                                break;
                            case "B":
                                scoreValue = 3.0f;
                                break;
                            case "C+":
                                scoreValue = 2.4f;
                                break;
                            case "C":
                                scoreValue = 2.0f;
                                break;
                            case "D+":
                                scoreValue = 1.5f;
                                break;
                            case "D":
                                scoreValue = 1.0f;
                                break;
                            case "F":
                                scoreValue = 0.0f;
                                break;
                            default:
                                continue;
                        }
                        gpa += scoreValue * subject.getSubjectCredits();
                        count += subject.getSubjectCredits();
                        subjects++;
                    }
                }

                String studentCode = student.getStudentCode();
                if (studentCode == null) continue;

                if (!isValidStudentCode(studentCode)) {
                    continue;
                }

                if (subjects < 4) {
                    continue;
                }

                if (count > 0) {
                    float rawGPA = gpa / count;

                    if (rawGPA > 0) {
                        float roundedGPA = Math.round(rawGPA * 100) / 100f;

                        float rawAsiaGPA = rawGPA * 2.5f;
                        if (rawAsiaGPA > 0) {
                            float roundedAsiaGPA = Math.round(rawAsiaGPA * 100) / 100f;

                            Ranking ranking = Ranking.builder()
                                    .student(student)
                                    .gpa(roundedGPA)
                                    .ranking(1L)
                                    .asiaGpa(roundedAsiaGPA)
                                    .build();

                            newRankings.add(ranking);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Lỗi khi tính GPA cho sinh viên: " + student.getStudentCode() + ": " + e.getMessage());
            }
        }

        if (!newRankings.isEmpty()) {
            newRankings.sort(Comparator.comparing(Ranking::getGpa).reversed());
            for (int i = 0; i < newRankings.size(); i++) {
                newRankings.get(i).setRanking((long) (i + 1));
            }
        }

        try {
            this.rankingRepository.deleteAllRecords();
            this.rankingRepository.resetAutoIncrement();

            if (!newRankings.isEmpty()) {
                rankingRepository.saveAll(newRankings);
            }
        } catch (Exception e) {
            throw new Exception("Lỗi khi cập nhật bảng xếp hạng: " + e.getMessage());
        }
    }
    public List<RankingResponse> findSchoolRanking(String studentCode) {
        List<RankingResponse> result = new ArrayList<>();
        result.add(RankingResponse.formData(rankingRepository.findByStudentCode(studentCode)));
        List<Ranking> list = this.rankingRepository
                .findTopRankingsByStudentCodes(studentCode);
        result.add(RankingResponse.formData(list.get(1)));
        result.add(RankingResponse.formData(list.get(0)));
        result.add(RankingResponse.formData(list.get(2)));
        return result;
    }
    public List<RankingResponse> findListTopRanking() {
        List<Ranking> list = this.rankingRepository
                .findListTopRanking();
        List<RankingResponse> result = new ArrayList<>();
        result.add(RankingResponse.formData(list.get(1)));
        result.add(RankingResponse.formData(list.get(0)));
        result.add(RankingResponse.formData(list.get(2)));
        return result;
    }
    @Transactional
    public void updateSemesterRanking() {
        try {
            this.semesterRankingRepository.deleteAllRecords();
            this.semesterRankingRepository.resetAutoIncrement();

            List<Student> studentList = studentRepository.findAll();

            List<Semester> allScores = semesterRepository.findAll();
            Map<String, List<Semester>> scoresByStudent = allScores.stream()
                    .filter(score -> score.getStudent() != null && score.getStudent().getStudentCode() != null)
                    .collect(Collectors.groupingBy(score -> score.getStudent().getStudentCode()));

            List<Subject> allSubjects = subjectService.findAll();
            Map<String, Subject> subjectMap = allSubjects.stream()
                    .filter(subject -> subject.getSubjectName() != null)
                    .collect(Collectors.toMap(Subject::getSubjectName, subject -> subject, (existing, replacement) -> existing));

            List<SemesterRanking> newRankings = new ArrayList<>();

            for (Student student : studentList) {
                if (student == null || student.getStudentCode() == null) {
                    continue;
                }

                String studentCode = student.getStudentCode();

                if (!isValidStudentCode(studentCode)) {
                    continue;
                }

                float gpa = 0f;
                int count = 0;
                int subjects = 0;

                List<Semester> scoreList = scoresByStudent.getOrDefault(studentCode, Collections.emptyList());

                for (Semester score : scoreList) {
                    if (score.getSubject() == null || score.getSubject().getSubjectName() == null) {
                        continue;
                    }

                    String subjectName = score.getSubject().getSubjectName();

                    if (subjectName.contains("Giáo dục thể chất")
                    || subjectName.contains("Thực hành vật")) {
                        subjects++;
                        continue;
                    }

                    Subject subject = subjectMap.get(subjectName);
                    if (subject == null) continue;

                    String scoreText = score.getScoreText();
                    if (scoreText == null) continue;

                    float scoreValue;
                    switch (scoreText) {
                        case "A+": scoreValue = 4.0f; break;
                        case "A": scoreValue = 3.8f; break;
                        case "B+": scoreValue = 3.5f; break;
                        case "B": scoreValue = 3.0f; break;
                        case "C+": scoreValue = 2.4f; break;
                        case "C": scoreValue = 2.0f; break;
                        case "D+": scoreValue = 1.5f; break;
                        case "D": scoreValue = 1.0f; break;
                        case "F": scoreValue = 0.0f; break;
                        default: continue;
                    }

                    Long subjectCredits = subject.getSubjectCredits();
                    gpa += scoreValue * subjectCredits;
                    count += subjectCredits;
                    subjects++;
                }

                if (subjects <= 3) {
                    continue;
                }

                if (count > 0) {
                    float roundedGPA = Math.max(0.01f, Math.round((gpa / count) * 100) / 100f);
                    float roundedAsiaGPA = Math.max(0.01f, Math.round((gpa / count) * 2.5f * 100) / 100f);

                    newRankings.add(SemesterRanking.builder()
                            .student(student)
                            .gpa(roundedGPA)
                            .ranking(1L)
                            .asiaGpa(roundedAsiaGPA)
                            .build());
                }
            }

            newRankings.sort(Comparator.comparing(SemesterRanking::getAsiaGpa).reversed());
            for (int i = 0; i < newRankings.size(); i++) {
                newRankings.get(i).setId((long) i + 1);
                newRankings.get(i).setRanking((long) (i + 1));
            }

            semesterRankingRepository.saveAll(newRankings);

        } catch (Exception e) {
            throw new RuntimeException("Failed to update semester rankings", e);
        }
    }
    @Override
    public List<RankingDTO> getAllRankings() {
        return null;
    }

    @Override
    public RankingDTO getRankingById(Long id) {
        return null;
    }

    @Override
    public RankingDTO getRankingByStudentCode(String studentCode) {
        return null;
    }

    @Override
    public RankingDTO getRankingByStudentId(Long studentId) {
        return null;
    }

    @Override
    public RankingDTO createRanking(RankingDTO rankingDTO) {
        return null;
    }

    @Override
    public RankingDTO updateRanking(Long id, RankingDTO rankingDTO) {
        return null;
    }
    public void updateScholarShip() {
        this.scholarshipRepository.deleteAllRecords();
        this.scholarshipRepository.resetAutoIncrement();
        
        // Get all semester rankings and filter by valid student codes
        List<SemesterRanking> validRankings = this.semesterRankingRepository.findAll().stream()
                .filter(ranking -> ranking.getStudent() != null 
                        && isValidStudentCode(ranking.getStudent().getStudentCode()))
                .sorted(Comparator.comparing(SemesterRanking::getAsiaGpa).reversed())
                .collect(Collectors.toList());

        List<Scholarship> result = new ArrayList<>();
        long count = 0;
        for (SemesterRanking ranking : validRankings) {
            result.add(
                Scholarship.builder()
                    .ranking(++count)
                    .asiaGpa(ranking.getAsiaGpa())
                    .gpa(ranking.getGpa())
                    .student(ranking.getStudent())
                    .build()
            );
        }
        
        this.scholarshipRepository.saveAll(result);
    }
    @Override
    public void deleteRanking(Long id) {

    }
    public FilterRankingResponse getScoreByStudentCode(String studentCode) {
        Student student = this.findByStudentCode(studentCode);
        List<Score> list = scoreRepository.findByStudentId(student.getStudentId());
        List<ScoreDTO> data = new ArrayList<>();
        for (Score clone : list){
            ScoreDTO scoreResponse = ScoreDTO.builder()
                    .scoreFinal(clone.getScoreFinal())
                    .scoreSecond(clone.getScoreSecond())
                    .scoreOverall(clone.getScoreOverall())
                    .scoreFirst(clone.getScoreFirst())
                    .scoreText(clone.getScoreText())
                    .subjectName(clone.getSubject().getSubjectName())
                    .subjectCredit(clone.getSubject().getSubjectCredits())
                    .build();
            data.add(scoreResponse);
        }
        ListScoreDTO listScoreDTO = ListScoreDTO.builder()
                .studentDTO(StudentDTO.builder()
                        .studentClass(student.getStudentClass())
                        .studentName(student.getStudentName())
                        .studentCode(student.getStudentCode())
                        .build())
                .scoreDTOS(data)
                .build();
        return FilterRankingResponse.builder()
                .listScoreDTO(listScoreDTO)
                .build();
    }
    public Student findByStudentCode(String studentCode) {
        return studentRepository.findByStudentCode(studentCode);
    }
    public void updateSemesterTable() {
        this.semesterRepository.deleteAllScores();
        this.semesterRepository.resetAutoIncrement();
        this.studentFailedRepository.deleteAllScores();
        this.studentFailedRepository.resetAutoIncrement();
        List<Score> list = this.scoreRepository.findScoresWithLatestSemester();
        List<Semester> result = list.stream().map(
                scores -> {
                    if (scores.getScoreFinal()<4){
                        this.studentFailedRepository.save(StudentFailed.builder()
                                .studentCode(scores.getStudent().getStudentCode())
                                .build());
                    }
                    return Semester.builder()
                            .student(scores.getStudent())
                            .subject(scores.getSubject())
                            .scoreText(scores.getScoreText())
                            .scoreSecond(scores.getScoreSecond())
                            .scoreFinal(scores.getScoreFinal())
                            .scoreFirst(scores.getScoreFirst())
                            .scoreOverall(scores.getScoreOverall())
                            .build();
                }
        ).collect(Collectors.toList());
        this.semesterRepository.saveAll(result);
    }
}
