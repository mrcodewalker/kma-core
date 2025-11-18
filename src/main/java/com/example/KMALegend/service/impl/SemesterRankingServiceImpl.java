package com.example.KMALegend.service.impl;

import com.example.KMALegend.dto.RankingDTO;
import com.example.KMALegend.dto.SubjectDTO;
import com.example.KMALegend.entity.*;
import com.example.KMALegend.exception.ResourceNotFoundException;
import com.example.KMALegend.repository.ScholarshipRepository;
import com.example.KMALegend.repository.SemesterRankingRepository;
import com.example.KMALegend.repository.SemesterRepository;
import com.example.KMALegend.repository.StudentRepository;
import com.example.KMALegend.service.SemesterRankingService;
import com.example.KMALegend.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SemesterRankingServiceImpl implements SemesterRankingService {
    private final SemesterRankingRepository semesterRankingRepository;
    private final StudentRepository studentRepository;
    private final SemesterRepository semesterRepository;
    private final ScholarshipRepository scholarshipRepository;
    private final StudentFailedServiceImpl studentFailedService;
    private final SubjectService subjectService;

    @Override
    public void updateRanking() {
        List<SemesterRanking> rankingList = this.semesterRankingRepository.findAll();

        rankingList.sort(Comparator.comparing(SemesterRanking::getGpa).reversed());
        int cnt = 0;
        for (SemesterRanking semesterRanking : rankingList) {
            cnt++;
            semesterRanking.setRanking(Long.parseLong(cnt + ""));
        }
        this.semesterRankingRepository.saveAll(rankingList);
    }
    @Transactional
    public void scholarshipUpdate() {
        this.scholarshipRepository.deleteAllRecords();
        this.scholarshipRepository.resetAutoIncrement();
        List<Student> studentList = new ArrayList<>();
        studentList = this.studentRepository.findAll();
        for (int i=0;i<studentList.size();i++){
            if (this.scholarshipRepository.findByStudentCode(
                    studentList.get(i).getStudentCode()
            )!=null
                    || this.scholarshipRepository.findListFilter(studentList.get(i).getStudentCode().substring(0,4)).size()>0){
                continue;
            }
            List<SemesterRanking> rankings = this.semesterRankingRepository
                    .findListFilter(studentList.get(i).getStudentCode().substring(0,4));
            List<Scholarship> scholarships = new ArrayList<>();
            for (int j=0;j<rankings.size();j++){
                rankings.get(j).setRanking(j+1L);
                scholarships.add(SemesterRanking.formData(rankings.get(j)));
            }
            if (scholarships.size()>0) {
                this.scholarshipRepository.saveAll(scholarships);
            }
        }
    }
    @Override
    @Transactional
    public void updateGPA() {
        List<Student> studentList = studentRepository.findAll();

        // Fetch all scores and subjects in one query
        List<Semester> allScores = semesterRepository.findAll();
        Map<String, List<Semester>> scoresByStudent = allScores.stream()
                .collect(Collectors.groupingBy(score -> score.getStudent().getStudentCode()));

        List<Subject> allSubjects = subjectService.findAll();
        Map<String, Subject> subjectMap = allSubjects.stream()
                .collect(Collectors.toMap(Subject::getSubjectName, subject -> subject));

        List<SemesterRanking> newRankings = new ArrayList<>();

        for (Student student : studentList) {
            float gpa = 0f;
            int count = 0;
            int subjects = 0;

            List<Semester> scoreList = scoresByStudent.get(student.getStudentCode());
            if (scoreList != null) {
                for (Semester score : scoreList) {
                    if (score.getSubject().getSubjectName().contains("Giáo dục thể chất")) {
                        continue;
                    }

                    Subject subject = subjectMap.get(score.getSubject().getSubjectName());
                    if (subject == null) continue;

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
                        default:
                            continue;
                    }
                    gpa += scoreValue * subject.getSubjectCredits();
                    count += subject.getSubjectCredits();
                    subjects++;
                }
            }
            if (!student.getStudentCode().contains("CT08")
                    && !student.getStudentCode().contains("CT07")
                    && !student.getStudentCode().contains("CT06")
                    && !student.getStudentCode().contains("CT05")
                    && !student.getStudentCode().contains("DT07")
                    && !student.getStudentCode().contains("DT06")
                    && !student.getStudentCode().contains("DT05")
                    && !student.getStudentCode().contains("DT04")
                    && !student.getStudentCode().contains("AT20")
                    && !student.getStudentCode().contains("AT19")
                    && !student.getStudentCode().contains("AT18")
                    && !student.getStudentCode().contains("AT17")
            ){
                continue;
            }
            if (subjects<=3){
                continue;
            }
            if (count != 0) {
                float roundedGPA = Math.round((gpa / count) * 100) / 100f;
                float roundedAsiaGPA = Math.round((gpa / count) * 2.5f * 100) / 100f;
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
            newRankings.get(i).setRanking((long) (i + 1));
        }
        semesterRankingRepository.saveAll(newRankings);
    }

    @Override
    public List<RankingDTO> findRanking(String studentCode) throws ResourceNotFoundException {
        studentCode = studentCode.toUpperCase();
        List<RankingDTO> responses = new ArrayList<>();
        List<Object[]> scholarships = this.semesterRankingRepository.findTopStudents(
                studentCode.substring(0, 4) + "%", studentCode
        );

        if (scholarships.size() < 3) {
            throw new ResourceNotFoundException("Can not be filtered right now!");
        }

        for (int count = 0; count < scholarships.size(); count++) {
            Object[] scholarship = scholarships.get(count);
            Long ranking = (Long) scholarship[0];
            String studentCodeFromDb = (String) scholarship[1];
            String studentName = (String) scholarship[2];
            String studentClass = (String) scholarship[3];
            Float gpa = (Float) scholarship[4];
            Float asiaGpa = (Float) scholarship[5];

            RankingDTO response = RankingDTO.builder()
                    .ranking(ranking)
                    .studentCode(studentCodeFromDb)
                    .studentName(studentName)
                    .studentClass(studentClass)
                    .gpa(gpa)
                    .asiaGpa(asiaGpa)
                    .build();

            responses.add(response);  // Thêm vào cuối danh sách
        }

        boolean found = false;

        // Kiểm tra xem studentCode có nằm trong responses không
        int cnt =0;
        for (RankingDTO resp : responses) {
            cnt++;
            if (resp.getStudentCode().equals(studentCode)) {
                if (cnt==4)
                    // Di chuyển sinh viên có studentCode trùng lên đầu danh sách
                    responses.add(0, responses.remove(responses.indexOf(resp))); // Di chuyển sinh viên vào đầu danh sách
                else
                    responses.add(0, resp);
                found = true;  // Đánh dấu là đã tìm thấy
                break;  // Thoát khỏi vòng lặp nếu đã tìm thấy
            }
        }

        // Nếu không tìm thấy studentCode, di chuyển phần tử cuối lên đầu
        if (!found && !responses.isEmpty()) {
            RankingDTO lastElement = responses.remove(responses.size() - 1); // Lấy phần tử cuối
            responses.add(0, lastElement); // Di chuyển phần tử cuối lên đầu danh sách
        }

        RankingDTO clone = responses.get(1);
        responses.set(1, responses.get(2));
        responses.set(2, clone);

        return responses;
    }
    public List<RankingDTO> findRanking(String studentCode, Long count) throws ResourceNotFoundException {
        studentCode = studentCode.toUpperCase();
        String filterCode = studentCode.substring(0, 4) + "%";

        List<Object[]> scholarships = this.semesterRankingRepository.findTopStudents(filterCode, studentCode, count);

        if (scholarships.isEmpty()) {
            throw new ResourceNotFoundException("No ranking data found!");
        }

        List<RankingDTO> responses = new ArrayList<>();
        RankingDTO studentResponse = null;

        for (Object[] scholarship : scholarships) {
            Long ranking = (Long) scholarship[0];
            String studentCodeFromDb = (String) scholarship[1];
            String studentName = (String) scholarship[2];
            String studentClass = (String) scholarship[3];
            Float gpa = (Float) scholarship[4];
            Float asiaGpa = (Float) scholarship[5];

            RankingDTO response = RankingDTO.builder()
                    .ranking(ranking)
                    .studentCode(studentCodeFromDb)
                    .studentName(studentName)
                    .studentClass(studentClass)
                    .gpa(gpa)
                    .asiaGpa(asiaGpa)
                    .build();

            if (studentCodeFromDb.equals(studentCode)) {
                studentResponse = response;
            } else {
                responses.add(response);
            }
        }

        // Rearrange the list based on the student's position
        if (studentResponse != null) {
            if (studentResponse.getRanking() > count) {
                responses.add(0, studentResponse);
                if (responses.size() > count) {
                    responses.remove(responses.size() - 1);
                }
            } else {
                int insertIndex = Math.min(studentResponse.getRanking().intValue() - 1, responses.size());
                responses.add(insertIndex, studentResponse);
            }
        }

        // Trim or pad the list to match the count
        while (responses.size() > count) {
            responses.remove(responses.size() - 1);
        }
        while (responses.size() < count) {
            responses.add(RankingDTO.builder().ranking((long) responses.size() + 1).build());
        }

        return responses;
    }


    @Override
    public List<SubjectDTO> findSubjects(String studentCode) {
        List<Semester> semesters = this.semesterRepository.findByStudentCode(studentCode);
        List<SubjectDTO> list = new ArrayList<>();
        for (Semester auto : semesters){
            list.add(SubjectDTO.builder()
                    .subjectName(auto.getSubject().getSubjectName())
                    .build());
        }
        return list;
    }

    @Override
    public List<RankingDTO> getList100Students() {
        List<RankingDTO> result = new ArrayList<>();
        Pageable pageable = PageRequest.of(0, 100, Sort.by("ranking").ascending());
        List<Scholarship> list = this.scholarshipRepository.findTop100(pageable);
        for (Scholarship clone : list){
            List<SubjectDTO> subjectResponses = this.findSubjects(clone.getStudent().getStudentCode());
            if (subjectResponses.size()<=3||clone.getGpa()<3){
                continue;
            }
            result.add(RankingDTO.convert(clone));
        }
        return result;
    }

    @Override
    public List<RankingDTO> filterListStudents(String filterCode) {
        Pageable pageable = PageRequest.of(0, 30);
        List<SemesterRanking> response = this.semesterRankingRepository.findTopWithMatchingFilterCode(filterCode, pageable);
        List<RankingDTO> result = new ArrayList<>();
        for (SemesterRanking clone : response){
            Student student = clone.getStudent();
            int count = 0;
            if (this.studentFailedService.findByStudentCode(clone.getStudent().getStudentCode()).size()>0) continue;
            List<Semester> list = this.semesterRepository.findByStudentCode(student.getStudentCode());
            for (Semester auto : list){
                if (auto.getScoreFinal()<4){
                    count++;
                    break;
                }
            }
            if(count==0){
                result.add(RankingDTO.builder()
                        .studentClass(student.getStudentClass())
                        .studentCode(student.getStudentCode())
                        .studentName(student.getStudentName())
                        .ranking(clone.getRanking())
                        .asiaGpa(clone.getAsiaGpa())
                        .gpa(clone.getGpa())
                        .build());
            }
        }
        for (int i=0;i<result.size();i++){
            result.set(i, RankingDTO.builder()
                    .gpa(result.get(i).getGpa())
                    .asiaGpa(result.get(i).getAsiaGpa())
                    .ranking(i+1L)
                    .studentName(result.get(i).getStudentName())
                    .studentClass(result.get(i).getStudentClass())
                    .studentCode(result.get(i).getStudentCode())
                    .build());
        }
        return result;
    }
    public List<RankingDTO> formData(List<SemesterRanking> semesterRankings) {
        List<RankingDTO> responses = new ArrayList<>();
        for (int i = 0; i < semesterRankings.size(); i++) {
            SemesterRanking semesterRanking = semesterRankings.get(i);
            Student student = semesterRanking.getStudent();

            responses.add(RankingDTO.builder()
                    .gpa(semesterRanking.getGpa())
                    .asiaGpa(semesterRanking.getAsiaGpa())
                    .ranking(i + 1L)  // Đặt thứ hạng từ 1
                    .studentName(student.getStudentName())
                    .studentClass(student.getStudentClass())
                    .studentCode(student.getStudentCode())
                    .build());
        }
        return responses;
    }
}
