package com.example.KMALegend.controller;

import com.example.KMALegend.common.responses.StatusResponse;
import com.example.KMALegend.dto.CreateScoreDTO;
import com.example.KMALegend.dto.ScoreUpdateContainerDTO;
import com.example.KMALegend.dto.StudentDTO;
import com.example.KMALegend.entity.Score;
import com.example.KMALegend.entity.Student;
import com.example.KMALegend.entity.Subject;
import com.example.KMALegend.exception.ResourceNotFoundException;
import com.example.KMALegend.repository.SubjectRepository;
import com.example.KMALegend.service.impl.ScoreServiceImpl;
import com.example.KMALegend.service.impl.StudentServiceImpl;
import com.example.KMALegend.service.impl.SubjectServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.modelmapper.internal.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.example.KMALegend.service.ScoreService;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/scores")
@CrossOrigin(origins = "https://kma-legend.onrender.com")
@Tag(name = "Scores", description = "Score management APIs")
public class ScoreController {
    private final SubjectServiceImpl subjectService;
    private final StudentServiceImpl studentService;
    private final SubjectRepository subjectRepository;
    private final ScoreServiceImpl scoreService;
    public List<String> errors = new ArrayList<>();
    private List<String> listSubjectsName = new ArrayList<>();
    private List<Pair<String,Integer>> specialCase = new ArrayList<>();
    private Integer totalSubjects = 0;
    @PostMapping("/score")
    @Transactional
    public ResponseEntity<?> ReadPDFFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("semester") String semester
    ) throws Exception {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File không hợp lệ.");
        }

        Map<String, Integer> allSubjects = new LinkedHashMap<>();
        Map<String, String> subjects = new LinkedHashMap<>();
        Map<String, Student> mapStudent = new HashMap<>();
        Map<String, List<Score>> mapScore = new HashMap<>();
        Map<String, Subject> mapSubject = new HashMap<>();
        List<Student> failedStudents = new ArrayList<>();
        Set<String> errorIndicators = new HashSet<>(Arrays.asList(
                "DC", "đình chỉ", "chỉ", "n100", "n25", "tkd", "thi", "v", "k", "nghỉ", "quá", "25%", "kld"
        ));
        // Error codes to skip
        Set<String> errorCodes = new HashSet<>(Arrays.asList("N25", "N100", "TKD", "DC", "V", "K", "KLD"));

        PDDocument pdfDocument = PDDocument.load(file.getInputStream());
        PDFTextStripper pdfTextStripper = new PDFTextStripper();
        pdfTextStripper.setStartPage(2); // Start from page 2
        String docText = pdfTextStripper.getText(pdfDocument);
        String[] lines = docText.split("\\r?\\n");

        System.out.println("Start collectAllSubjects " + new Date().getTime());
        collectAllSubjects(file);
        System.out.println("Finish collectAllSubjects " + new Date().getTime());

        Set<String> idSubjects = new HashSet<>();
        String currentSubject = null;
        boolean inScoreSection = false;

        for (String line : lines) {
            // Check if line contains "Môn thi:" to identify subject sections
            if (line.contains("Môn thi:")) {
                String subjectLine = line.substring(line.indexOf("Môn thi:") + 9).trim();
                if (subjectLine.contains(" - ")) {
                    currentSubject = subjectLine.substring(0, subjectLine.lastIndexOf(" - ")).trim();
                } else {
                    currentSubject = subjectLine;
                }
                inScoreSection = true;
                continue;
            }

            if (!inScoreSection) {
                continue;
            }

            if (line.contains("TT SBD Mã HVSV Họ đệm Tên Lớp TP1 TP2 THI TKHP Chữ")) {
                continue;
            }

            // Process student score rows
            String[] parts = line.trim().split("\\s+");
            if (parts.length < 10) {
                continue; // Skip lines that don't have enough data
            }

            // Check if the first part is a number (row number)
            if (!parts[0].matches("\\d+")) {
                continue;
            }

            // Extract student code (now including CT and DT prefixes)
            String studentCode = null;
            for (String part : parts) {
                if (part.matches("(AT|CT|DT)\\d+")) {
                    studentCode = part;
                    break;
                }
            }

            if (studentCode == null) {
                continue; // Skip if no valid student code found
            }

            // Find the index of the student class (now including CT and DT prefixes)
            int classIndex = -1;
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].matches("(AT|CT|DT)\\d+[A-Z]")) {
                    classIndex = i;
                    break;
                }
            }

            if (classIndex == -1 || classIndex + 5 >= parts.length) {
                continue; // Skip if class not found or not enough data for scores
            }

            // Extract student name (between student code and class)
            int studentCodeIndex = -1;
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals(studentCode)) {
                    studentCodeIndex = i;
                    break;
                }
            }

            if (studentCodeIndex == -1 || studentCodeIndex >= classIndex) {
                continue;
            }

            StringBuilder nameBuilder = new StringBuilder();
            for (int i = studentCodeIndex + 1; i < classIndex; i++) {
                nameBuilder.append(parts[i]).append(" ");
            }
            String studentName = nameBuilder.toString().trim();

            String studentClass = parts[classIndex];

            boolean hasError = false;
            for (int i = classIndex + 1; i < Math.min(classIndex + 6, parts.length); i++) {
                if (errorCodes.contains(parts[i].toUpperCase())) {
                    hasError = true;
                    break;
                }
            }

            if (hasError || classIndex + 5 >= parts.length) {
                continue;
            }

            // Parse scores
            try {
                String tp1Str = parts[classIndex + 1].replace(',', '.');
                String tp2Str = parts[classIndex + 2].replace(',', '.');
                String thiStr = parts[classIndex + 3].replace(',', '.');
                String tkhpStr = parts[classIndex + 4].replace(',', '.');
                String letterGrade = parts[classIndex + 5];

                // Check if scores are valid numbers
                if (!tp1Str.matches("\\d+(\\.\\d+)?") ||
                        !tp2Str.matches("\\d+(\\.\\d+)?") ||
                        !thiStr.matches("\\d+(\\.\\d+)?") ||
                        !tkhpStr.matches("\\d+(\\.\\d+)?")) {
                    continue;
                }


                float scoreFirst = Float.parseFloat(tp1Str);
                float scoreSecond = Float.parseFloat(tp2Str);
                float scoreFinal = Float.parseFloat(thiStr);
                float scoreOverall = Float.parseFloat(tkhpStr);

                // Validate letter grade
                Set<String> validGrades = new HashSet<>(Arrays.asList(
                        "A", "A+", "B+", "B", "C+", "C", "D+", "D", "F"
                ));

                if (!validGrades.contains(letterGrade.toUpperCase())) {
                    continue;
                }

                // Create or get student
                Student student = Student.builder()
                        .studentClass(studentClass)
                        .studentCode(studentCode)
                        .studentName(studentName)
                        .build();

                mapStudent.put(studentCode, student);

                if (!mapScore.containsKey(studentCode)) {
                    mapScore.put(studentCode, new ArrayList<>());
                }

                if (currentSubject != null) {
                    mapSubject.computeIfAbsent(currentSubject, key -> {
                        return (subjectService.findBySubjectName(key) == null) ?
                                subjectService.createSubject(
                                        Subject.builder()
                                                .subjectName(key)
                                                .subjectCredits(2L)
                                                .build()
                                ) : subjectService.findBySubjectName(key);
                    });

                    Subject subject = mapSubject.get(currentSubject);

                    Score score = Score.builder()
                            .scoreFirst(scoreFirst)
                            .scoreFinal(scoreFinal)
                            .scoreText(letterGrade)
                            .scoreSecond(scoreSecond)
                            .scoreOverall(scoreOverall)
                            .student(student)
                            .subject(subject)
                            .semester(semester)
                            .build();

                    mapScore.get(studentCode).add(score);
                    System.out.println("Added score: " + score);
                }
            } catch (NumberFormatException e) {
                // Skip this line if score parsing fails
                System.out.println("Failed to parse scores for line: " + line);
                continue;
            }
        }

        this.scoreService.saveData(mapScore, mapStudent);

        pdfDocument.close();

        return ResponseEntity.ok(
                StatusResponse.builder()
                        .status("200")
                        .build()
        );
    }
    @PostMapping("/score-new-format")
    @Transactional
    public ResponseEntity<?> ReadPDFFileNewFormat(
            @RequestParam("file") MultipartFile file,
            @RequestParam("semester") String semester,
            @RequestParam("user_id") String userId
    ) throws Exception {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File không hợp lệ.");
        }

        Map<String, Integer> allSubjects = new LinkedHashMap<>();
        errors.add("N25");
        errors.add("N100");
        errors.add("TKD");
        errors.add("K");

        PDDocument pdfDocument = PDDocument.load(file.getInputStream());
        PDFTextStripper pdfTextStripper = new PDFTextStripper();
        pdfTextStripper.setStartPage(2);
        String docText = pdfTextStripper.getText(pdfDocument);

        // Tách văn bản thành các dòng
        String[] lines = docText.split("\\r?\\n");

        // Collect all subject names first (preserving the existing method)
        System.out.println("Start collectAllSubjects " + new Date().getTime());
        collectAllSubjects(file);
        System.out.println("Finish collectAllSubjects " + new Date().getTime());

        boolean passedSubjects = false;
        int currentSubjectIndex = -1;
        String currentSubject = "";

        Map<String, Student> mapStudent = new HashMap<>();
        Map<String, List<Score>> mapScore = new HashMap<>();
        Map<String, Subject> mapSubject = new HashMap<>();

        System.out.println("Begin parsing new format " + new Date().getTime());

        for (String line : lines) {
            // Bỏ qua các dòng trống hoặc tiêu đề
            if (line.trim().isEmpty() || line.contains("TT SBD Mã HVSV")) {
                continue;
            }

            // Kiểm tra xem dòng có phải là tên môn học không
            if (line.trim().startsWith("Danh sách") || line.contains("AT20") || line.contains("CT20")) {
                // Đây có thể là tên môn học
                for (int i = 0; i < listSubjectsName.size(); i++) {
                    if (line.contains(listSubjectsName.get(i))) {
                        currentSubjectIndex = i;
                        currentSubject = listSubjectsName.get(i);
                        passedSubjects = true;
                        break;
                    }
                }
                continue;
            }

            if (!passedSubjects || currentSubjectIndex < 0) {
                continue;
            }

            // Xử lý dòng thông tin sinh viên
            String[] parts = line.trim().split("\\s+");

            // Kiểm tra xem dòng có đủ thông tin không (TT, SBD, Mã SV, Họ đệm, Tên, Lớp, TP1, TP2, THI, TKHP, Chữ, [Ghi chú])
            if (parts.length < 11) {
                continue;
            }

            try {
                // Xác định vị trí các trường dữ liệu
                int index = 0;
                String stt = parts[index++];
                String sbd = parts[index++];
                String studentCode = parts[index++]; // Mã sinh viên

                // Xác định vị trí của Lớp (dựa vào mẫu chuỗi bắt đầu bằng AT hoặc CT)
                StringBuilder studentName = new StringBuilder();
                int classIndex = -1;

                for (int i = index; i < parts.length; i++) {
                    if (parts[i].startsWith("AT") || parts[i].startsWith("CT")) {
                        classIndex = i;
                        break;
                    }
                }

                if (classIndex == -1) {
                    continue; // Không tìm thấy thông tin lớp
                }

                // Ghép họ đệm và tên
                for (int i = index; i < classIndex; i++) {
                    studentName.append(parts[i]).append(" ");
                }

                String fullName = studentName.toString().trim();
                String studentClass = parts[classIndex];

                // Xác định vị trí các điểm
                int scoresStartIndex = classIndex + 1;

                // Kiểm tra đủ thông tin điểm không
                if (parts.length < scoresStartIndex + 4) {
                    continue;
                }

                // Xử lý các điểm
                Float scoreFirst = parseFloatScore(parts[scoresStartIndex]);
                Float scoreSecond = parseFloatScore(parts[scoresStartIndex + 1]);
                Float scoreFinal = parseFloatScore(parts[scoresStartIndex + 2]);
                Float scoreOverRall = parseFloatScore(parts[scoresStartIndex + 3]);
                String scoreText = parts[scoresStartIndex + 4];

                if (isValidScore(scoreFirst, scoreSecond, scoreFinal, scoreOverRall, scoreText)) {
                    Student student = Student.builder()
                            .studentClass(studentClass)
                            .studentCode(studentCode)
                            .studentName(fullName)
                            .build();

                    mapStudent.put(studentCode, student);
                    if (!mapScore.containsKey(studentCode)) {
                        mapScore.put(studentCode, new ArrayList<>());
                    }

                    // Lấy hoặc tạo Subject
                    mapSubject.computeIfAbsent(currentSubject, key -> {
                        return (subjectService.findBySubjectName(key) == null) ?
                                subjectService.createSubject(
                                        Subject.builder()
                                                .subjectName(key)
                                                .subjectCredits(2L)
                                                .build()
                                ) : subjectService.findBySubjectName(key);
                    });

                    Subject subject = mapSubject.get(currentSubject);

                    // Tạo đối tượng Score
                    Score score = Score.builder()
                            .scoreFirst(scoreFirst)
                            .scoreFinal(scoreFinal)
                            .scoreText(scoreText)
                            .scoreSecond(scoreSecond)
                            .scoreOverall(scoreOverRall)
                            .student(student)
                            .subject(subject)
                            .semester(semester)
                            .build();

                    System.out.println(score);
                    mapScore.get(studentCode).add(score);
                }
            } catch (Exception e) {
                System.out.println("Error parsing line: " + line);
                e.printStackTrace();
            }
        }

        System.out.println("Finish parsing new format " + new Date().getTime());
        this.scoreService.saveData(mapScore, mapStudent);
        System.out.println("Finish saveData " + new Date().getTime());
        pdfDocument.close();

        return ResponseEntity.ok(
                StatusResponse.builder()
                        .status("200")
                        .build()
        );
    }

    private Float parseFloatScore(String scoreStr) {
        if (scoreStr == null || scoreStr.isEmpty() ||
                errors.contains(scoreStr.toUpperCase()) ||
                !scoreStr.matches("^[-+]?[0-9]*[,.]?[0-9]+$")) {
            return 0F;
        }

        String normalizedScore = scoreStr.replace(',', '.');
        try {
            return Float.parseFloat(normalizedScore);
        } catch (NumberFormatException e) {
            return 0F;
        }
    }

    private boolean isValidScore(Float scoreFirst, Float scoreSecond, Float scoreFinal,
                                 Float scoreOverRall, String scoreText) {
        // Check if all scores are valid
        if (scoreFirst < 0 || scoreSecond < 0 || scoreFinal < 0 || scoreOverRall < 0) {
            return false;
        }

        String[] validScores = {"A", "A+", "B+", "C+", "D+", "D", "B", "C", "F"};
        for (String validScore : validScores) {
            if (validScore.equalsIgnoreCase(scoreText)) {
                return true;
            }
        }

        return false;
    }
    public static String extractSemester(String fileName) throws Exception {
        // Kiểm tra định dạng file
        if (!fileName.endsWith(".pdf")) {
            throw new Exception("File name must end with .pdf");
        }

        // Tách phần chuỗi trước .pdf
        String baseName = fileName.substring(0, fileName.lastIndexOf("."));

        // Kiểm tra định dạng "namYYYY_YYYY_kiX"
        if (!baseName.contains("nam") || !baseName.contains("_")) {
            throw new Exception("File name does not have the required format");
        }

        String line[] = fileName.split("_");
        if (line.length<4) {
            throw new ResourceNotFoundException("Please check your name content file again!");
        }
        // Ghép lại thành "ki2-2023-2024"
        return line[2] + "-" + line[0].replace("nam","") + "-" + line[1];
    }
    public void collectAllSubjects(MultipartFile file) throws Exception {
        // Clear the list before processing
        this.listSubjectsName.clear();

        // Load the PDF document
        PDDocument pdfDocument = PDDocument.load(file.getInputStream());
        PDFTextStripper pdfTextStripper = new PDFTextStripper();

        // Start from page 2 where subject information typically begins
        pdfTextStripper.setStartPage(2);

        // Extract text from the document
        String docText = pdfTextStripper.getText(pdfDocument);

        // Split the text into lines
        String[] lines = docText.split("\\r?\\n");

        // Get all subjects from the database for comparison
        List<Subject> subjectList = subjectService.findAll();
        List<String> subjectsFromDB = new ArrayList<>();
        for (Subject subject : subjectList) {
            subjectsFromDB.add(subject.getSubjectName().trim());
        }

        // Set to track already added subjects to avoid duplicates
        Set<String> addedSubjects = new HashSet<>();

        // Flag to identify when we've reached the subject section
        boolean inSubjectSection = false;

        for (String line : lines) {
            // Check if we've reached the subject section
            if (line.contains("Môn thi:")) {
                inSubjectSection = true;

                // Extract the subject name from the "Môn thi:" line
                String subjectName = line.substring(line.indexOf("Môn thi:") + 9).trim();

                // Remove any trailing dash and course code (e.g., "- AT18")
                if (subjectName.contains(" - ")) {
                    subjectName = subjectName.substring(0, subjectName.lastIndexOf(" - ")).trim();
                }

                // Add the subject if not already added
                if (!addedSubjects.contains(subjectName)) {
                    addedSubjects.add(subjectName);
                    this.listSubjectsName.add(subjectName);
                }

                continue;
            }

            // Skip processing until we reach the subject section
            if (!inSubjectSection) {
                continue;
            }

            // Check for subject names in the database that match parts of the current line
            for (String dbSubjectName : subjectsFromDB) {
                // Normalize both strings for comparison to handle accents
                String normalizedLine = Normalizer.normalize(line, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
                String normalizedDBName = Normalizer.normalize(dbSubjectName, Normalizer.Form.NFD).replaceAll("\\p{M}", "");

                // Check if the line contains the subject name
                if (normalizedLine.contains(normalizedDBName) && !addedSubjects.contains(dbSubjectName)) {
                    addedSubjects.add(dbSubjectName);
                    this.listSubjectsName.add(dbSubjectName);
                    break;
                }
            }
        }

        // Close the PDF document
        pdfDocument.close();

        // Print the extracted subjects for verification
        System.out.println("Extracted subjects:");
        for (int i = 0; i < this.listSubjectsName.size(); i++) {
            System.out.println(i + " " + this.listSubjectsName.get(i));
        }
    }
    @PostMapping("/score/complement")
    @Transactional
    public ResponseEntity<?> ReadPDFFileComplement(
            @RequestParam("file") MultipartFile file,
            @RequestParam("semester") String semester,
            @RequestParam("user_id") String userId
    ) throws Exception {
        Map<String, Integer> allSubjects = new LinkedHashMap<>();
        errors.add("N25");
        errors.add("N100");
        errors.add("TKD");
        List<Score> list = new ArrayList<>();
        PDDocument pdfDocument = PDDocument.load(file.getInputStream());

        PDFTextStripper pdfTextStripper = new PDFTextStripper();

        pdfTextStripper.setStartPage(0);

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
//        for (Map.Entry<String, String> entry: subjects.entrySet()){ // All Subjects
//            if (!this.checkContainsSubject(entry.getValue())){
//                this.subjectService.createSubject(Subject.builder()
//                        .subjectName(entry.getValue())
//                        .build());
////                System.out.println(entry.getValue());
//            }
//        }
//        for (Map.Entry<String, Integer> entry: allSubjects.entrySet()){
//            if (entry.getValue()>1){
//                this.specialCase.add(Pair.of(entry.getKey(), entry.getValue()));
//            }
//        }
//        System.out.println(this.specialCase);
        boolean passedSubjects = false;
        collectSubjectsNew(file);
        Map<String,Student> mapStudent = new HashMap<>();
        Map<String,List<Score>> mapScore = new HashMap<>();
        Map<String,Subject> mapSubject = new HashMap<>();
        String previousLine = "";
        String previousSubject = "";

        for (String line : lines) {
            int spaceIndex = line.indexOf(" ");

            if (spaceIndex != -1) {
                String firstWord = line.substring(0, spaceIndex);

                String secondWord = line.substring(spaceIndex + 1);

                if (firstWord.length()<=4&&!firstWord.isEmpty()&&firstWord.matches("[1-9]\\d{0,3}")){
//                    if (firstWord.equals(String.valueOf(idSubjects.size()))){
//                        passedSubjects = true;
//                        continue;
//                    }
                    if (!passedSubjects){
                        if (!previousLine.contains("AT") && !previousLine.contains("CT") && !previousLine.contains("DT")
                                &&
                                firstWord.equals("1")&&secondWord.split(" ").length>7){
//                            System.out.println(firstWord+ " "+secondWord);
                            rows++;
                        }
                        String data[] = secondWord.split(" ");
//                        if (data[0].equals("0")) continue;
//                        if (data.length<8) continue;
                        String studentCode = "";
                        int markCode = 0;
                        if (data[0].contains("DT")||data[0].contains("CT")||data[0].contains("AT")){
                            studentCode = data[0];
                            markCode = 1;
                        } else {
                            studentCode = data[1];
                            markCode=2;
                        }
                        String studentName = "";
                        int mark = 4;
                        for (int i=markCode;i<data.length;i++){
                            if (data[i].contains("CT")||data[i].contains("AT")||data[i].contains("DT")){
                                mark = i ;
                                for (int j=2;j<i;j++){
                                    studentName += data[j] + " ";
                                }
                                studentName = studentName.trim();
                                break;
                            }
                        }
                        boolean checkFailedStudent = false;
                        System.out.println(line);
                        if (data.length>=mark+6){
                            for (int i=mark+1;i<data.length;i++){
                                if(data[i].contains("DC")
                                        || data[i].equalsIgnoreCase("đình")
                                        || data[i].equalsIgnoreCase("chỉ")
                                        || data[i].equalsIgnoreCase("n100")
                                        || data[i].equalsIgnoreCase("n25")
                                        || data[i].equalsIgnoreCase("tkd")
                                        || data[i].equalsIgnoreCase("thi")
                                        || data[i].equalsIgnoreCase("v")
                                        || data[i].equalsIgnoreCase("k")
                                        || data[i].equalsIgnoreCase("nghỉ")
                                        || data[i].equalsIgnoreCase("quá")
                                        || data[i].equalsIgnoreCase("25%")
                                        || data[i].equalsIgnoreCase("kld")
                                )
                                {
                                    checkFailedStudent = true;
                                    break;
                                }
                            }
                        }
                        if (data.length<mark+6||checkFailedStudent){
                            String[] newData = new String[mark+10];

                            System.arraycopy(data, 0, newData, 0, data.length);
                            data = newData;
                            for (int i=mark+1;i<data.length;i++){
                                data[i] = "0";
                            }
                            data[mark+5]="F";
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
//                                checkError=true;
                                cnt = 4;
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
                            for (int i = mark+1; i < data.length; i++) {
                                if (data[i].contains(",")) {
                                    data[i] = data[i].replace(',', '.');
                                }
                            }
                            scoreFirst = Float.parseFloat(data[mark+1]);
                            scoreSecond = Float.parseFloat(data[mark+2]);
                            scoreFinal = Float.parseFloat(data[mark+3]);
                            scoreOverRall = Float.parseFloat(data[mark+4]);
                            scoreText = data[mark+5];
                            String[] invalidScores = {"A", "A+", "B+", "C+", "D+", "D", "B", "C", "F",
                                    "a", "a+", "b+", "c+", "d+", "d", "b", "c", "f"};
                            if (!Arrays.asList(invalidScores).contains(scoreText.toUpperCase())) continue;
                        } else continue;
                        if (scoreFirst>=0&&scoreSecond>=0&&scoreFinal>=0&&scoreOverRall>=0) {

                            Student student = Student.builder()
                                    .studentClass(studentClass)
                                    .studentCode(studentCode)
                                    .studentName(studentName)
                                    .build();

                            mapStudent.put(studentCode, student);
                            if(!mapScore.containsKey(studentCode)){
                                mapScore.put(studentCode,new ArrayList<>());
                            }

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
                            String subjectName = this.listSubjectsName.get(rows);
                            if (!mapSubject.containsKey(subjectName)){
                                mapSubject.put(subjectName, subjectService.findBySubjectName(subjectName));
                            }

                            Subject subject = mapSubject.get(subjectName);

                            Score score = Score.builder()
                                    .scoreFirst(scoreFirst)
                                    .scoreFinal(scoreFinal)
                                    .scoreText(scoreText)
                                    .scoreSecond(scoreSecond)
                                    .scoreOverall(scoreOverRall)
                                    .student(student)
                                    .subject(subject)
                                    .semester(semester)
                                    .build();
                            mapScore.get(studentCode).add(score);

                        }
                    }
                }
            }
            previousLine = line;
        }
        System.out.println("Finish startFor 2 "+new Date().getTime());
        this.scoreService.saveData(mapScore, mapStudent);
        System.out.println("Finish saveData 2 "+new Date().getTime());
        pdfDocument.close();
        return ResponseEntity.ok(
                StatusResponse.builder()
                        .status("200")
                        .build()
        );
    }
    @PostMapping("/create/score")
    public ResponseEntity<?> createNewScore(
            @RequestBody CreateScoreDTO scoreDTO
    ){
        return ResponseEntity.ok(
                this.scoreService.createNewScore(
                        scoreDTO
                ));
    }
    public void collectAllSubjectsFake(MultipartFile file) throws Exception {
        Map<String, String> list = new LinkedHashMap<>();
        errors.add("N25");
        errors.add("N100");
        errors.add("TKD");

        PDDocument pdfDocument = PDDocument.load(file.getInputStream());
        System.out.println(pdfDocument.getPages().getCount());

        PDFTextStripper pdfTextStripper = new PDFTextStripper();

        pdfTextStripper.setStartPage(0);

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

//                if (firstWord.length() <= 4 && !firstWord.isEmpty() && firstWord.matches("[1-9]\\d{0,3}")) {
//                    if (firstWord.equals(String.valueOf(idSubjects.size()))) {
//                        passedSubjects = true;
//                        continue;
//                    }
//                }
            }
//            System.out.println(subjectsName.contains("Lập trình nhân Linux"));
            if (!passedSubjects) {
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
    public void collectSubjectsNew(MultipartFile file) throws Exception{
        Map<String, String> list = new LinkedHashMap<>();

        PDDocument pdfDocument = PDDocument.load(file.getInputStream());
        System.out.println(pdfDocument.getPages().getCount());

        PDFTextStripper pdfTextStripper = new PDFTextStripper();

        pdfTextStripper.setStartPage(0);

        PDPage firstPage = pdfDocument.getPage(0);

        String docText = pdfTextStripper.getText(pdfDocument);

        Set<String> idSubjects = new HashSet<>();
        String[] lines = docText.split("\\r?\\n");
        int rows = 0;
        int count = 0;
        List<Subject> subjectList = subjectService.findAll();
        List<String> subjectsName = new ArrayList<>();
        for (Subject subject : subjectList) {
            String subjectName = subject.getSubjectName();
            subjectsName.add(subjectName.trim());
        }
        Set<String> collectData = new LinkedHashSet<>();
        boolean passedSubjects = false;
        for (String line : lines) {
            int spaceIndex = line.indexOf("Môn thi");
            int checkValid = 0;
            String subjectName = "";
            if (spaceIndex!=-1){
                subjectName = line.substring(spaceIndex+8).trim();
                for (String subject: subjectsName){
                    if (Normalizer.normalize(subject.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").equalsIgnoreCase(
                            Normalizer.normalize(subjectName.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", ""))){
                        collectData.add(subject);
                        checkValid++;
                    }
                }
                if(checkValid==0){
                    Subject subject = Subject.builder()
                            .subjectCredits(2L)
                            .subjectName(subjectName)
                            .build();
                    Subject newSubject = this.subjectService.createSubject(
                            subject
                    );
                    collectData.add(newSubject.getSubjectName());
                    subjectsName.add(newSubject.getSubjectName());
                }
            } else continue;
        }
        this.listSubjectsName = collectData.stream().toList();
        System.out.println(this.listSubjectsName);
    }
    @PostMapping("/score/semester")
    public ResponseEntity<?> SaveSemester(
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        Map<String, Integer> allSubjects = new LinkedHashMap<>();
        errors.add("N25");
        errors.add("N100");
        errors.add("TKD");
        PDDocument pdfDocument = PDDocument.load(file.getInputStream());
        System.out.println(pdfDocument.getPages().getCount());

        PDFTextStripper pdfTextStripper = new PDFTextStripper();

        pdfTextStripper.setStartPage(2);

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
                        if (data[0].equals("0")) continue;
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
                            Subject subject = Subject.builder()
                                    .subjectName(this.listSubjectsName.get(rows))
                                    .subjectId(subjectService.findSubjectByName(this.listSubjectsName.get(rows)).getSubjectId())
                                    .build();
//
////
                            Score score = Score.builder()
                                    .scoreFirst(scoreFirst)
                                    .scoreFinal(scoreFinal)
                                    .scoreText(scoreText)
                                    .scoreSecond(scoreSecond)
                                    .scoreOverall(scoreOverRall)
                                    .student(student)
                                    .subject(subject)
                                    .build();
                            scoreService.createScore(score);

                        }
                    }
                }
            }
        }

        pdfDocument.close();
        return null;
    }
    private boolean checkContainsSubject(String subjectName){
        List<Subject> subjectsData = subjectRepository.findAll();
        List<String> subjectsName = new ArrayList<>();
        for (Subject subject : subjectsData){
            if (subject.getSubjectName().equals(subjectName)){
                return true;
            }
        }
        return false;
    }
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getScoresByStudentCode(@PathVariable("id") String studentCode){
        return ResponseEntity.ok(scoreService.getScoreByStudentCode(studentCode));
    }

    @Operation(summary = "Create new score", description = "Creates a new score entry for a student")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Score created successfully",
                content = @Content(schema = @Schema(implementation = CreateScoreDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Student or subject not found")
    })
    @PostMapping
    public ResponseEntity<?> createScore(@RequestBody CreateScoreDTO createScoreDTO) {
        return ResponseEntity.ok(scoreService.createScore(createScoreDTO));
    }
}
