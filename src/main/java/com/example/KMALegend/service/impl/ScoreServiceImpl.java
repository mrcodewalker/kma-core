package com.example.KMALegend.service.impl;

import com.example.KMALegend.dto.*;
import com.example.KMALegend.entity.Score;
import com.example.KMALegend.entity.Student;
import com.example.KMALegend.repository.ScoreRepository;
import com.example.KMALegend.repository.StudentRepository;
import com.example.KMALegend.repository.SubjectRepository;
import com.example.KMALegend.service.ScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ScoreServiceImpl implements ScoreService {
    private final ScoreRepository scoreRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    @Override
    public List<ScoreDTO> getAllScores() {
        return null;
    }

    @Override
    public List<ScoreDTO> getScoresByStudentId(Integer studentId) {
        return null;
    }
    @Transactional
    public Score createScore(Score score) {
        System.out.println("Begin createScore before sql"+new Date().getTime());
        List<Score> data = scoreRepository.findByStudentCode(score.getStudent().getStudentCode());
        System.out.println("after sql "+data.size()+" "+new Date().getTime());
        for (Score entry : data){
            if (entry.getSubject().getSubjectName().equals(score.getSubject().getSubjectName())
                    || Normalizer.normalize(entry.getSubject().getSubjectName(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").equalsIgnoreCase(
                    Normalizer.normalize(score.getSubject().getSubjectName(), Normalizer.Form.NFD).replaceAll("\\p{M}", ""))){
                score.setId(entry.getId());
                score.setScoreFirst(score.getScoreFirst());
                score.setScoreOverall(score.getScoreOverall());
                score.setScoreFinal(score.getScoreFinal());
                score.setScoreText(score.getScoreText());
                score.setScoreSecond(score.getScoreSecond());
                System.out.println("Finish createScore 1"+new Date().getTime());
                return scoreRepository.save(score);
            }
        }
        System.out.println("Finish createScore 2"+new Date().getTime());
        return scoreRepository.save(score);
    }
    @Override
    public List<ScoreDTO> getScoresByStudentAndSemester(Integer studentId, String semester) {
        return null;
    }
    public List<Score> createNewScore(CreateScoreDTO scoreDTO) {
        List<Score> scores = new ArrayList<>();
        for (String index: scoreDTO.getListStudent()){
            Student student = this.studentRepository.findByStudentCode(index.toUpperCase());
            scores.add(
                    Score.builder()
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
        System.out.println(scores);
        return this.scoreRepository.saveAll(scores);
    }
    @Override
    public ScoreDTO getScoreById(Long id) {
        return null;
    }

    @Override
    public List<Score> createScore(CreateScoreDTO scoreDTO) {
        List<Score> scores = new ArrayList<>();
        for (String index: scoreDTO.getListStudent()){
            Student student = this.studentRepository.findByStudentCode(index.toUpperCase());
            scores.add(
                    Score.builder()
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
        System.out.println(scores);
        return this.scoreRepository.saveAll(scores);
    }

    @Override
    public ScoreDTO updateScore(Long id, ScoreDTO scoreDTO) {
        return null;
    }
    public ScoreUpdateContainerDTO getScoreFlag(Score score, List<Score> data) {
        for (Score entry : data){
            if (entry.getSubject().getSubjectName().equals(score.getSubject().getSubjectName())
                    || Normalizer.normalize(entry.getSubject().getSubjectName(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").equalsIgnoreCase(
                    Normalizer.normalize(score.getSubject().getSubjectName(), Normalizer.Form.NFD).replaceAll("\\p{M}", ""))){
                score.setId(entry.getId());
                score.setScoreFirst(score.getScoreFirst());
                score.setScoreOverall(score.getScoreOverall());
                score.setScoreFinal(score.getScoreFinal());
                score.setScoreText(score.getScoreText());
                score.setScoreSecond(score.getScoreSecond());
                //System.out.println("Finish createScore 1"+new Date().getTime());
                return ScoreUpdateContainerDTO.builder()
                        .score(score)
                        .isUpdate(true)
                        .build();
            }
        }
        return ScoreUpdateContainerDTO.builder()
                .score(score)
                .isUpdate(false)
                .build();
    }

    @Override
    public void deleteScore(Long id) {

    }
    public ListScoreDTO getScoreByStudentCode(String studentCode) {
        Student student = this.studentRepository.findByStudentCode(studentCode);
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
                    .build();
            data.add(scoreResponse);
        }
        return ListScoreDTO.builder()
                .studentDTO(StudentDTO.builder()
                        .studentClass(student.getStudentClass())
                        .studentName(student.getStudentName())
                        .studentCode(student.getStudentCode())
                        .build())
                .scoreDTOS(data)
                .build();
    }
    public void saveData(Map<String, List<Score>> mapScore, Map<String, Student> mapStudent){
        int stdCount = 0;
        int scoreCount = 0;
        System.out.println("13579 "+stdCount+" | "+scoreCount+" "+new Date());
        List<List<String>> studentCodeBulk = this.chunkList(mapStudent.keySet().stream().toList(), 1000);
        List<Student> newStudentList = new ArrayList<>();
        for(List<String> codes : studentCodeBulk){
            // select toan bo student co ma trong code
            List<Student> existStudentList = this.studentRepository.findListStudentByStudentCode(codes);
            List<String> existStudentCodeList = new ArrayList<>();
            for (Student student: existStudentList){
                existStudentCodeList.add(student.getStudentCode());
                mapStudent.put(student.getStudentCode(), student);
            }
            for (String studentCode: codes){
                if (!existStudentCodeList.contains(studentCode)){
                    newStudentList.add(mapStudent.get(studentCode));
                }
            }
            List<Student> addNewStudentList = this.studentRepository.saveAll(newStudentList);
            for (Student student: addNewStudentList){
                mapStudent.put(student.getStudentCode(), student);
            }
        }
        System.out.println("TEST 1: ");
        List<List<String>> studentCodeInsert = this.chunkList(mapStudent.keySet().stream().toList(), 200);
        Map<String,List<Score>> dataScoreByStudentMap = new HashMap<>();
        for (List<String> studentCodes: studentCodeInsert){
            List<Score> dataBulk = scoreRepository.findListScoreByStudentCode(studentCodes);
            for(Score score : dataBulk){
                String code = score.getStudent().getStudentCode();
                if(!dataScoreByStudentMap.containsKey(code)){
                    dataScoreByStudentMap.put(code,new ArrayList<>());
                }
                dataScoreByStudentMap.get(code).add(score);
            }
        }
        System.out.println("TEST 2: ");

        List<Score> updateList = new ArrayList<>();
        List<Score> insertList = new ArrayList<>();

        for(String studentCode : mapStudent.keySet()){
            if(!dataScoreByStudentMap.containsKey(studentCode)){
                dataScoreByStudentMap.put(studentCode,new ArrayList<>());
            }
        }

        for (String studentCode : mapStudent.keySet()){
            stdCount++;
            Student student = mapStudent.get(studentCode);
            for(Score score:mapScore.get(studentCode)){
                //System.out.println("TEST 2-1: ");
                score.setStudent(student);
                ScoreUpdateContainerDTO dataBinding =  this.getScoreFlag(score, dataScoreByStudentMap.get(studentCode));
                //System.out.println("TEST 2-1-2: ");
                if (dataBinding.isUpdate()){
                    updateList.add(dataBinding.getScore());
                } else {
                    insertList.add(dataBinding.getScore());
                }
                //System.out.println("TEST 2-2: ");
                scoreCount++;
            }
        }
        System.out.println("TEST 3: ");
        List<List<Score>> updateListBulk = this.chunkList(updateList, 100);
        List<List<Score>> insertListBulk = this.chunkList(insertList, 1000);
        for (List<Score> update: updateListBulk){
            this.scoreRepository.saveAll(update);
        }
        for (List<Score> insert: insertListBulk){
            this.scoreRepository.saveAll(insert);
        }
        System.out.println("TEST 4: ");
        System.out.println("13579 "+stdCount+" | "+scoreCount+" "+new Date());
    }
    public <T> List<List<T>> chunkList(List<T> list, int chunkSize) {
        List<List<T>> chunkedList = new ArrayList<>();

        for (int i = 0; i < list.size(); i += chunkSize) {
            int end = Math.min(list.size(), i + chunkSize);
            chunkedList.add(new ArrayList<>(list.subList(i, end)));
        }

        return chunkedList;
    }
}
