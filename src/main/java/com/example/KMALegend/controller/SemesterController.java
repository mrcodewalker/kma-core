package com.example.KMALegend.controller;

import com.example.KMALegend.common.responses.SemesterResponse;
import com.example.KMALegend.dto.CreateScoreDTO;
import com.example.KMALegend.dto.StudentFailedDTO;
import com.example.KMALegend.encode.DecryptedRequestWrapper;
import com.example.KMALegend.entity.Semester;
import com.example.KMALegend.entity.Student;
import com.example.KMALegend.entity.Subject;
import com.example.KMALegend.exception.ResourceNotFoundException;
import com.example.KMALegend.repository.SubjectRepository;
import com.example.KMALegend.service.impl.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.modelmapper.internal.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/semester")
@CrossOrigin(origins = "https://kma-legend.onrender.com")
public class SemesterController {
    private final SemesterServiceImpl semesterService;
    private final ScoreServiceImpl scoreService;
    private final SubjectServiceImpl subjectService;
    private final StudentFailedServiceImpl studentFailedService;
    private final StudentServiceImpl studentService;
    private final SubjectRepository subjectRepository;
    private final SemesterRankingServiceImpl semesterRankingService;
    public List<String> errors = new ArrayList<>();
    private List<String> listSubjectsName = new ArrayList<>();
    private List<Pair<String,Integer>> specialCase = new ArrayList<>();
    private final ObjectMapper objectMapper;

    private Integer totalSubjects = 0;
    @PostMapping("/update")
    public ResponseEntity<?> SemesterUpdate(
            @RequestParam("file")MultipartFile file
            ) throws Exception{
        Map<String, Integer> allSubjects = new LinkedHashMap<>();
        PDDocument pdfDocument = PDDocument.load(file.getInputStream());
        System.out.println(pdfDocument.getPages().getCount());

        PDFTextStripper pdfTextStripper = new PDFTextStripper();

        pdfTextStripper.setStartPage(2);
        String previousSubject = "";

        PDPage firstPage = pdfDocument.getPage(0);

        String docText = pdfTextStripper.getText(pdfDocument);

        Map<String, String> subjects = new LinkedHashMap<>();
        Set<String> idSubjects = new HashSet<>();
// Tách văn bản thành các dòng
        String[] lines = docText.split("\\r?\\n");
        int rows = -1;
        int count = 0;
        for (String line : lines) {
            int spaceIndex = line.indexOf(" ");

            if (spaceIndex != -1) {
                String firstWord = line.substring(0, spaceIndex);

                String secondWord = line.substring(spaceIndex + 1);

                if (firstWord.length()<=2&&!firstWord.isEmpty()&&firstWord.matches("[1-9][0-9]?")){
                    if (!idSubjects.contains(firstWord)) {
                        idSubjects.add(firstWord);
                        if (secondWord.contains("Học lại")){
                            int index = secondWord.indexOf("Học lại");
                            if (index >= 0) {
                                secondWord = secondWord.substring(0, index).trim();
                            }
                        }
                        if (secondWord.contains("CT")||secondWord.contains("DT")||secondWord.contains("AT")
                                || secondWord.contains("CNTT") || secondWord.contains("ĐTVT") || secondWord.contains("ATTT")
                                || secondWord.contains("(")){
                            int indexCT = secondWord.indexOf("CT");
                            int indexDT = secondWord.indexOf("DT");
                            int indexAT = secondWord.indexOf("AT");
                            int indexCNTT = secondWord.indexOf("CNTT");
                            int indexATTT = secondWord.indexOf("ATTT");
                            int indexDTVT = secondWord.indexOf("ĐTVT");
                            int indexCharacter = secondWord.indexOf("(");

                            int minIndex = -1;
                            if (indexCT >= 0 || indexDT >= 0 || indexAT >= 0 || indexCNTT >=0 || indexATTT >=0 || indexDTVT>=0 || indexCharacter >=0) {
                                minIndex = Math.min(indexCT >= 0 ? indexCT : Integer.MAX_VALUE,
                                        Math.min(indexDT >= 0 ? indexDT : Integer.MAX_VALUE,
                                                Math.min(indexAT >= 0 ? indexAT : Integer.MAX_VALUE,
                                                        Math.min(indexCNTT >= 0 ? indexCNTT-1 : Integer.MAX_VALUE,
                                                                Math.min(indexATTT >=0 ? indexATTT-1 : Integer.MAX_VALUE,
                                                                        Math.min(indexDTVT >=0 ? indexDTVT-1 : Integer.MAX_VALUE,
                                                                                indexCharacter >=0 ? indexCharacter : Integer.MAX_VALUE))))));
                            }

                            if (minIndex >= 0) {
                                secondWord = secondWord.substring(0, minIndex).trim();
                            }
                        }
                        if (allSubjects.get(secondWord.trim())!=null&&allSubjects.get(secondWord.trim())>=1) {
                            allSubjects.put(secondWord.trim(), allSubjects.get(secondWord.trim()) + 1);
                            totalSubjects++;
                        } else {
                            if (allSubjects.get(secondWord.trim())==null||allSubjects.get(secondWord.trim())==0){
                                allSubjects.put(secondWord.trim(),1);
                                totalSubjects++;
                            }
                        }
                        if (secondWord.contains("HTTT")){
                            secondWord = secondWord.substring(0,secondWord.indexOf("HTTT")).trim()+" hệ thống thông tin";
                            secondWord.trim();
                        }
                        if (secondWord.contains("&")) {
                            secondWord = secondWord.replace("&", "và");
                        }
                        subjects.put(firstWord, secondWord.trim());
                    } else {
                        break;
                    };
                }
            }

        }
        for (Map.Entry<String, String> entry: subjects.entrySet()){ // All Subjects
            if (!this.checkContainsSubject(entry.getValue())){
                this.subjectService.createSubject(Subject.builder()
                        .subjectName(entry.getValue())
                        .build());
//                System.out.println(entry.getValue());
            }
        }
        for (Map.Entry<String, Integer> entry: allSubjects.entrySet()){
            if (entry.getValue()>1){
                this.specialCase.add(Pair.of(entry.getKey(), entry.getValue()));
            }
        }
        System.out.println(this.specialCase);
        boolean passedSubjects = false;
        collectAllSubjects(file);

        for (String line : lines) {
            int spaceIndex = line.indexOf(" ");

            if (spaceIndex != -1) {
                String firstWord = line.substring(0, spaceIndex);

                String secondWord = line.substring(spaceIndex + 1);

                if (firstWord.length()<=4&&!firstWord.isEmpty()&&firstWord.matches("[1-9]\\d{0,3}")){
                    if (firstWord.equals(String.valueOf(idSubjects.size()))){
                        passedSubjects = true;
                        continue;
                    }
                    if (passedSubjects){
                        if (firstWord.equals("1")&&secondWord.split(" ").length>7){
//                            System.out.println(firstWord+ " "+secondWord);
                            rows++;
                        }
                        String data[] = secondWord.split(" ");
//                        if (data[0].equals("0")) continue;
                        if (data.length<8) continue;
                        String studentCode = data[1];
                        String studentName = "";
                        int mark = 4;
                        for (int i=2;i<data.length;i++){
                            if (data[i].contains("CT")||data[i].contains("AT")||data[i].contains("DT")){
                                mark = i ;
                                for (int j=2;j<i;j++){
                                    studentName += data[j] + " ";
                                }
                                studentName = studentName.trim();
                                break;
                            }
                        }
                        int cnt = 0;
                        boolean checkError = false;
                        String studentClass = data[mark];
                        for (int i=mark+1;i<data.length;i++){
                            String entry = data[i];
                            if (entry.matches("^[-+]?[0-9]*\\.?[0-9]+$")){
                                cnt++;
                                if (cnt==4) break;
                            } else {
                                checkError=true;
                                break;
                            }
                        }
                        if (checkError){
                            checkError=false;
                            continue;
                        }
                        Float scoreFirst = 0F;
                        Float scoreSecond = 0F;
                        Float scoreFinal = 0F;
                        Float scoreOverRall = 0F;
                        String scoreText = "";
                        if (cnt == 4) {
                            scoreFirst = Float.parseFloat(data[mark+1]);
                            scoreSecond = Float.parseFloat(data[mark+2]);
                            scoreFinal = Float.parseFloat(data[mark+3]);
                            scoreOverRall = Float.parseFloat(data[mark+4]);
                            scoreText = data[data.length-1];
                            String[] invalidScores = {"A", "A+", "B+", "C+", "D+", "D", "B", "C", "F"};
                            if (!Arrays.asList(invalidScores).contains(scoreText.toUpperCase())) continue;
                        } else continue;
                        if (scoreFirst>=0&&scoreSecond>=0&&scoreFinal>=0&&scoreOverRall>=0) {
//
                            Student student = Student.builder()
                                    .studentClass(studentClass)
                                    .studentCode(studentCode)
//                                        .studentId(studentService.findByStudentCode(studentCode).getStudentId())
                                    .studentName(studentName)
                                    .build();

                            if (studentService.existByStudentCode(studentCode)){
                                student.setStudentId(studentService.findByStudentCode(studentCode).getStudentId());
                            }

                            studentService.createStudent(student);

                            if (rows<0||this.listSubjectsName.size()==0) continue;
                            if (rows>this.listSubjectsName.size()) continue;
                            if (rows==this.listSubjectsName.size()) rows--;
                            String cloneSubjectName = this.listSubjectsName.get(rows);

                            if (firstWord.equals("1")){
                                if (previousSubject.length()==0) {
                                    previousSubject = cloneSubjectName;
                                } else {
                                    if (cloneSubjectName.equalsIgnoreCase(previousSubject)){
                                        rows--;
                                    }
                                }
                            }
                            Subject subject = Subject.builder()
                                    .subjectName(this.listSubjectsName.get(rows))
                                    .subjectId(subjectService.findSubjectByName(this.listSubjectsName.get(rows)).getSubjectId())
                                    .build();
//
////
                            Semester semester = Semester.builder()
                                    .scoreFirst(scoreFirst)
                                    .scoreFinal(scoreFinal)
                                    .scoreText(scoreText)
                                    .scoreSecond(scoreSecond)
                                    .scoreOverall(scoreOverRall)
                                    .student(student)
                                    .subject(subject)
                                    .build();
//                            scoreService.createScore(score);
////                                System.out.println(score);
                            semesterService.createSemester(semester);
                        }
                    }
                }
            }
        }

        pdfDocument.close();
        return null;
    }
    public void collectAllSubjects(
            MultipartFile file
    ) throws Exception {
        Map<String, String> list = new LinkedHashMap<>();

        PDDocument pdfDocument = PDDocument.load(file.getInputStream());
        System.out.println(pdfDocument.getPages().getCount());

        PDFTextStripper pdfTextStripper = new PDFTextStripper();

        pdfTextStripper.setStartPage(2);

        PDPage firstPage = pdfDocument.getPage(0);

        String docText = pdfTextStripper.getText(pdfDocument);

        Set<String> idSubjects = new HashSet<>();
// Tách văn bản thành các dòng
        String[] lines = docText.split("\\r?\\n");
        int rows = 0;
        int count = 0;
        for (String line : lines) {
            int spaceIndex = line.indexOf(" ");

            if (spaceIndex != -1) {
                String firstWord = line.substring(0, spaceIndex);

                String secondWord = line.substring(spaceIndex + 1);

                if (firstWord.length() <= 2 && !firstWord.isEmpty() && firstWord.matches("[1-9][0-9]?")) {
                    if (!idSubjects.contains(firstWord)) {
                        idSubjects.add(firstWord);
                    } else break;
                }
            }

        }
        List<Subject> subjectList = subjectService.findAll();
        List<String> subjectsName = new ArrayList<>();
        for (Subject subject : subjectList) {
            String subjectName = subject.getSubjectName();
            subjectsName.add(subjectName.trim());
        }
//        for (Map.Entry<String, String> entry: subjects.entrySet()){ // All Subjects
//            System.out.println(entry.getKey()+" "+entry.getValue());
//        }
//        for (int i=0 ;i < subjectsName.size();i++){
//            System.out.println(i+ " "+subjectsName.get(i));
//        }
        // Lập trình nhân Linux
//        String str1 = "Lập trình nhân Linux"; database
//        String str2 = "Lập trình nhân Linux"; pdf file
//        str1 = Normalizer.normalize(str1, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
//        str2 = Normalizer.normalize(str2, Normalizer.Form.NFD).replaceAll("\\p{M}", "");

//        if (str1.equals(str2)) {
//            System.out.println("Hai chuỗi giống nhau.");
//        } else {
//            System.out.println("Hai chuỗi không giống nhau.");
//        }
        // Tín hiệu và hệ thống database
        // Tín hiệu và hệ thống file

        boolean passedSubjects = false;
        for (String line : lines) {
            int spaceIndex = line.indexOf(" ");

            if (spaceIndex != -1) {
                String firstWord = line.substring(0, spaceIndex);

                String secondWord = line.substring(spaceIndex + 1);

                if (firstWord.length() <= 4 && !firstWord.isEmpty() && firstWord.matches("[1-9]\\d{0,3}")) {
                    if (firstWord.equals(String.valueOf(idSubjects.size()))) {
                        passedSubjects = true;
                        continue;
                    }
                }
            }
//            System.out.println(subjectsName.contains("Lập trình nhân Linux"));
            if (passedSubjects) {
//              && subjectsName.stream().anyMatch(line::contains)
                String each[] = line.split(" ");
//                if (line.contains("Lập trình nhân Linux")){
//                    System.out.println(line);
//                }
                if (each.length > 0 && each[0].matches(".*\\d.*")) continue;
                for (String subjectName : subjectsName) {
                    int indexDash = line.lastIndexOf("-");
                    int indexCT = line.lastIndexOf("CT");
                    int indexDT = line.lastIndexOf("DT");
                    int indexAT = line.lastIndexOf("AT");

                    int[] indices = {indexDash, indexCT, indexDT, indexAT};

                    int minIndex = Integer.MAX_VALUE;
                    int checkDash = 0;
                    for (int index : indices) {
                        if (index != -1 && index < minIndex) {
                            minIndex = index;
                        } else checkDash++;
                    }
                    String subjectNameLine = "";
                    if(checkDash==4){
                        subjectNameLine = line.trim();
                    }
//                    System.out.println("OK "+subjectNameLine);
                    if (minIndex != Integer.MAX_VALUE || checkDash==4) {
                        if (checkDash!=4) {
                            subjectNameLine = line.substring(0, minIndex).trim();
                        } else {
                            subjectNameLine = subjectNameLine.trim();
                        }

                        if (subjectNameLine.contains("(")) {
                            subjectNameLine = subjectNameLine.substring(0, subjectNameLine.indexOf("(")).trim();
                        }
//                        System.out.println(subjectNameLine);
                        // Lập trình nhân Linux
//                        subjectNameLine= Normalizer.normalize(subjectNameLine, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
//                        subjectName = Normalizer.normalize(subjectName, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
                        if (subjectNameLine.equals(subjectName.trim())
                                || Normalizer.normalize(subjectNameLine.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").equalsIgnoreCase(
                                Normalizer.normalize(subjectName.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                        )) {
//                            System.out.println(subjectNameLine);
                            int counting = 0;
//                            if (!this.listSubjectsName.contains(subjectName)) this.listSubjectsName.add(subjectName);
                            for (int i = 0; i < this.listSubjectsName.size(); i++) {
                                if (this.listSubjectsName.get(i).equals(subjectName)
                                        || Normalizer.normalize(this.listSubjectsName.get(i), Normalizer.Form.NFD).replaceAll("\\p{M}", "").equals(
                                        Normalizer.normalize(subjectName, Normalizer.Form.NFD).replaceAll("\\p{M}", "")))
                                    break;
                                else {
                                    counting++;
                                }
                            }
                            if (counting == this.listSubjectsName.size()) {
                                this.listSubjectsName.add(subjectName);
                            } else {
                                if (this.listSubjectsName.get(this.listSubjectsName.size() - 1).equals(subjectName)
                                        || Normalizer.normalize(this.listSubjectsName.get(this.listSubjectsName.size() - 1), Normalizer.Form.NFD)
                                        .replaceAll("\\p{M}", "").equals(
                                                Normalizer.normalize(subjectName, Normalizer.Form.NFD).replaceAll("\\p{M}", ""))) {
                                    continue;
                                } else {
                                    for (int i = 0; i < this.specialCase.size(); i++) {
                                        Pair<String, Integer> clone = this.specialCase.get(i);
                                        if (clone.getLeft().equals(subjectName)
                                                || Normalizer.normalize(clone.getLeft(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").equals(
                                                Normalizer.normalize(subjectName, Normalizer.Form.NFD).replaceAll("\\p{M}", "")) && clone.getRight() > 1) {
                                            Pair<String, Integer> suffix = Pair.of(clone.getLeft(), clone.getRight() - 1);
                                            this.listSubjectsName.add(subjectName);
                                            this.specialCase.set(i, suffix);
                                        }
                                    }
                                }
                            }
                        } else {

                        }
//                    }
                    }
//                    System.out.println(line);
//            System.out.println(listSubjectsName.size());
                }
            }
//            int cnt = 0;
//            for (String clone : listSubjectsName) {
//                System.out.println(cnt + " " + clone);
//                cnt++;
//            }
        }
    }
    @PostMapping("/failed/student")
    public ResponseEntity<?> createFailedStudents(
            @RequestParam("file") MultipartFile file
    ) throws Exception{
        Map<String, Integer> allSubjects = new LinkedHashMap<>();
        PDDocument pdfDocument = PDDocument.load(file.getInputStream());
        System.out.println(pdfDocument.getPages().getCount());

        PDFTextStripper pdfTextStripper = new PDFTextStripper();

        pdfTextStripper.setStartPage(2);
        String previousSubject = "";

        PDPage firstPage = pdfDocument.getPage(0);

        String docText = pdfTextStripper.getText(pdfDocument);

        Map<String, String> subjects = new LinkedHashMap<>();
        Set<String> idSubjects = new HashSet<>();
// Tách văn bản thành các dòng
        String[] lines = docText.split("\\r?\\n");
        int rows = -1;
        int count = 0;
        for (String line : lines) {
            int spaceIndex = line.indexOf(" ");

            if (spaceIndex != -1) {
                String firstWord = line.substring(0, spaceIndex);

                String secondWord = line.substring(spaceIndex + 1);

                if (firstWord.length()<=2&&!firstWord.isEmpty()&&firstWord.matches("[1-9][0-9]?")){
                    if (!idSubjects.contains(firstWord)) {
                        idSubjects.add(firstWord);
                        if (secondWord.contains("Học lại")){
                            int index = secondWord.indexOf("Học lại");
                            if (index >= 0) {
                                secondWord = secondWord.substring(0, index).trim();
                            }
                        }
                        if (secondWord.contains("CT")||secondWord.contains("DT")||secondWord.contains("AT")
                                || secondWord.contains("CNTT") || secondWord.contains("ĐTVT") || secondWord.contains("ATTT")
                                || secondWord.contains("(")){
                            int indexCT = secondWord.indexOf("CT");
                            int indexDT = secondWord.indexOf("DT");
                            int indexAT = secondWord.indexOf("AT");
                            int indexCNTT = secondWord.indexOf("CNTT");
                            int indexATTT = secondWord.indexOf("ATTT");
                            int indexDTVT = secondWord.indexOf("ĐTVT");
                            int indexCharacter = secondWord.indexOf("(");

                            int minIndex = -1;
                            if (indexCT >= 0 || indexDT >= 0 || indexAT >= 0 || indexCNTT >=0 || indexATTT >=0 || indexDTVT>=0 || indexCharacter >=0) {
                                minIndex = Math.min(indexCT >= 0 ? indexCT : Integer.MAX_VALUE,
                                        Math.min(indexDT >= 0 ? indexDT : Integer.MAX_VALUE,
                                                Math.min(indexAT >= 0 ? indexAT : Integer.MAX_VALUE,
                                                        Math.min(indexCNTT >= 0 ? indexCNTT-1 : Integer.MAX_VALUE,
                                                                Math.min(indexATTT >=0 ? indexATTT-1 : Integer.MAX_VALUE,
                                                                        Math.min(indexDTVT >=0 ? indexDTVT-1 : Integer.MAX_VALUE,
                                                                                indexCharacter >=0 ? indexCharacter : Integer.MAX_VALUE))))));
                            }

                            if (minIndex >= 0) {
                                secondWord = secondWord.substring(0, minIndex).trim();
                            }
                        }
                        if (allSubjects.get(secondWord.trim())!=null&&allSubjects.get(secondWord.trim())>=1) {
                            allSubjects.put(secondWord.trim(), allSubjects.get(secondWord.trim()) + 1);
                            totalSubjects++;
                        } else {
                            if (allSubjects.get(secondWord.trim())==null||allSubjects.get(secondWord.trim())==0){
                                allSubjects.put(secondWord.trim(),1);
                                totalSubjects++;
                            }
                        }
                        if (secondWord.contains("HTTT")){
                            secondWord = secondWord.substring(0,secondWord.indexOf("HTTT")).trim()+" hệ thống thông tin";
                            secondWord.trim();
                        }
                        if (secondWord.contains("&")) {
                            secondWord = secondWord.replace("&", "và");
                        }
                        subjects.put(firstWord, secondWord.trim());
                    } else {
                        break;
                    };
                }
            }

        }
        for (Map.Entry<String, String> entry: subjects.entrySet()){ // All Subjects
            if (!this.checkContainsSubject(entry.getValue())){
                this.subjectService.createSubject(Subject.builder()
                        .subjectName(entry.getValue())
                        .build());
//                System.out.println(entry.getValue());
            }
        }
        for (Map.Entry<String, Integer> entry: allSubjects.entrySet()){
            if (entry.getValue()>1){
                this.specialCase.add(Pair.of(entry.getKey(), entry.getValue()));
            }
        }
        System.out.println(this.specialCase);
        boolean passedSubjects = false;
        collectAllSubjects(file);

        for (String line : lines) {
            int spaceIndex = line.indexOf(" ");

            if (spaceIndex != -1) {
                String firstWord = line.substring(0, spaceIndex);

                String secondWord = line.substring(spaceIndex + 1);
                String data[] = secondWord.split(" ");
                if (secondWord.contains("N100")||secondWord.contains("N25")||secondWord.contains("TKD")){
                    this.studentFailedService.createStudent(StudentFailedDTO.builder()
                                    .studentCode(data[1])
                            .build());
                }
                }
        }

        pdfDocument.close();
        return null;
    }
    boolean checkContainsSubject(String subjectName){
        List<Subject> subjectsData = subjectRepository.findAll();
        List<String> subjectsName = new ArrayList<>();
        for (Subject subject : subjectsData){
            if (subject.getSubjectName().equals(subjectName)){
                return true;
            }
        }
        return false;
    }
    @GetMapping("/renew")
    public ResponseEntity<?> RenewData(){
        this.semesterService.deleteAll();
        return ResponseEntity.ok("Data has been renewed");
    }
    @PostMapping("/ranking")
    public ResponseEntity<?> RankingUpdate(){
        this.semesterRankingService.updateGPA();
        return ResponseEntity.ok("Ranking has been updated");
    }
    @PostMapping("/update_scholarship")
    public ResponseEntity<?> scholarshipUpdate(){
        this.semesterRankingService.scholarshipUpdate();
        return ResponseEntity.ok("Ranking has been updated");
    }
    @GetMapping("/scholarship")
    public ResponseEntity<?> ScholarshipRanking(@RequestParam("student_code") String studentCode) throws ResourceNotFoundException {
        return ResponseEntity.ok(
                SemesterResponse.builder()
                        .rankingDTOS(this.semesterRankingService.findRanking(studentCode))
                        .subjectDTOS(this.semesterRankingService.findSubjects(studentCode))
                        .build()
        );
    }
    @GetMapping("/scholarship/top/{count}")
    public ResponseEntity<?> ScholarshipRankingTop(@RequestParam("student_code") String studentCode,
                                                   @PathVariable("count") Long count) throws ResourceNotFoundException {
        return ResponseEntity.ok(
                SemesterResponse.builder()
                        .rankingDTOS(this.semesterRankingService.findRanking(studentCode, count))
                        .build()
        );
    }
    @GetMapping("/top100")
    public ResponseEntity<?> top100(){
        return ResponseEntity.ok(
                SemesterResponse.builder()
                        .rankingDTOS(this.semesterRankingService.getList100Students())
                        .build()
        );
    }
    @PostMapping("/filter/scholarship")
    public ResponseEntity<?> filterScholarship(
            HttpServletRequest request
    ) throws Exception {
        DecryptedRequestWrapper wrapper = new DecryptedRequestWrapper(request, objectMapper);
        Map<String, String> credentials = wrapper.getDecryptedBody(Map.class);
        return ResponseEntity.ok(
                this.semesterRankingService.filterListStudents(credentials.get("code"))
        );
    }
    @PostMapping("/create/score")
    public ResponseEntity<?> createNewScore(@RequestBody CreateScoreDTO createScoreDTO){
        return ResponseEntity.ok(
                this.semesterService.createNewScore(createScoreDTO)
        );
    }
    @GetMapping("/collect/subjects")
    public ResponseEntity<?> collectSubjects()  {
        return ResponseEntity.ok(
                this.semesterService.subjectResponse()
        );
    }
}
