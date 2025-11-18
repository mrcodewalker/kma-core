package com.example.KMALegend.service.impl;

import com.example.KMALegend.dto.CreateScoreDTO;
import com.example.KMALegend.dto.RankingDTO;
import com.example.KMALegend.dto.SubjectDTO;
import com.example.KMALegend.entity.Semester;
import com.example.KMALegend.entity.Student;
import com.example.KMALegend.entity.Subject;
import com.example.KMALegend.repository.ScoreRepository;
import com.example.KMALegend.repository.SemesterRepository;
import com.example.KMALegend.repository.StudentRepository;
import com.example.KMALegend.repository.SubjectRepository;
import com.example.KMALegend.service.SemesterService;
import com.example.KMALegend.service.SubjectService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SemesterServiceImpl implements SemesterService {
    private final SemesterRepository semesterRepository;
    private final EntityManager entityManager;
    private final SubjectService subjectService;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final ScoreRepository scoreRepository;

    @Override
    public Semester createSemester(Semester semester) {
        List<Semester> data = semesterRepository.findByStudentCode(semester.getStudent().getStudentCode());
        for (Semester entry : data){
            if (entry.getSubject().getSubjectName().equals(semester.getSubject().getSubjectName())
                    || Normalizer.normalize(entry.getSubject().getSubjectName(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").equalsIgnoreCase(
                    Normalizer.normalize(semester.getSubject().getSubjectName(), Normalizer.Form.NFD).replaceAll("\\p{M}", ""))){
                semester.setId(entry.getId());
                semester.setScoreFirst(semester.getScoreFirst());
                semester.setScoreOverall(semester.getScoreOverall());
                semester.setScoreFinal(semester.getScoreFinal());
                semester.setScoreText(semester.getScoreText());
                semester.setScoreSecond(semester.getScoreSecond());
                return semesterRepository.save(semester);
            }
        }
        return semesterRepository.save(semester);
    }

    @Override
    public List<Semester> findAll() {
        return null;
    }

    @Override
    public void updateRanking() {

    }

    @Override
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM Semester").executeUpdate();
        entityManager.createQuery("ALTER TABLE Semester AUTO_INCREMENT = 1").executeUpdate();
    }

    @Override
    public List<RankingDTO> getScholarship(String studentCode) {
        return null;
    }

    @Override
    public List<Semester> createNewScore(CreateScoreDTO scoreDTO) {
        List<Semester> scores = new ArrayList<>();
        for (String index: scoreDTO.getListStudent()){
            Student student = this.studentRepository.findByStudentCode(index.toUpperCase());
            scores.add(
                    Semester.builder()
                            .subject(this.subjectRepository.findFirstBySubjectName(scoreDTO.getSubjectName()))
                            .student(student)
                            .scoreFirst(scoreDTO.getScoreFirst())
                            .scoreSecond(scoreDTO.getScoreSecond())
                            .scoreText(scoreDTO.getScoreText())
                            .scoreOverall(scoreDTO.getScoreOverall())
                            .scoreFinal(scoreDTO.getScoreFinal())
                            .build()
            );
        }
//        System.out.println(scores);
        return this.semesterRepository.saveAll(scores);
    }


    @Override
    public List<SubjectDTO> subjectResponse() {
        List<Long> distinctSubjectIds = semesterRepository.findDistinctSubjectIds();
        List<SubjectDTO> responses = new ArrayList<>();
        for (Long clone: distinctSubjectIds){
            Subject found = this.subjectService.findBySubjectId(clone);
            responses.add(
                    SubjectDTO.builder()
                            .subjectCredits(found.getSubjectCredits())
                            .subjectName(found.getSubjectName())
                            .build());

        }
        return responses;
    }
}
