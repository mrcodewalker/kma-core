package com.example.KMALegend.service.impl;

import com.example.KMALegend.common.responses.TimelineResponse;
import com.example.KMALegend.common.responses.VirtualCalendarResponse;
import com.example.KMALegend.entity.Student;
import com.example.KMALegend.entity.StudentSessions;
import com.example.KMALegend.repository.StudentSessionsRepository;
import com.example.KMALegend.service.StudentScheduleService;
import com.example.KMALegend.service.StudentService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class LoginServiceImpl {

    private CookieStore cookieStore;
    private HttpClientContext context;
    private CloseableHttpClient httpClient;
    private final StudentSessionServiceImpl sessionService;
    private final StudentService studentService;
    private final StudentScheduleService studentScheduleService;
    private static final String LOGIN_URL = "https://qldt.actvn.edu.vn/CMCSoft.IU.Web.Info/Login.aspx";
    private static final String STUDENT_PROFILE_URL = "https://qldt.actvn.edu.vn/CMCSoft.IU.Web.Info/StudentProfileNew/HoSoSinhVien.aspx";
    private static final String STUDENT_SCHEDULE_URL = "https://qldt.actvn.edu.vn/CMCSoft.IU.Web.Info/Reports/Form/StudentTimeTable.aspx";
    private static final String STUDENT_VIRTUAL_CALENDAR = "https://qldt.actvn.edu.vn/cmcsoft.iu.web.info/StudyRegister/StudyRegister.aspx";
    private static final String DATE_FORMAT = "dd/MM/yyyy";

    @PostConstruct
    public void init() {
        cookieStore = new BasicCookieStore();
        RequestConfig globalConfig = RequestConfig.custom().setRedirectsEnabled(true).build();
        context = HttpClientContext.create();
        context.setCookieStore(cookieStore);
        httpClient = HttpClients.custom()
                .setDefaultRequestConfig(globalConfig)
                .setDefaultCookieStore(cookieStore)
                .build();
    }
    private static final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

    private StudentSessions parseStudentSession(String targetSplit, String studentCode) {
        int startIndex = targetSplit.indexOf("Set-Cookie:");
        int endIndex = (startIndex != -1) ? targetSplit.indexOf(";", startIndex) : -1;

        String cookieData = (startIndex != -1 && endIndex != -1)
                ? targetSplit.substring(startIndex, endIndex)
                : "NULL COOKIE DATA";
        return StudentSessions.builder()
                .studentCode(studentCode)
                .cookieData(cookieData)
                .build();
    }

    public ResponseEntity<?> login(String username, String password, HttpServletRequest request) throws IOException {
        if (username == null || password == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "400", "message", "Missing Item"));
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet loginGet = new HttpGet(LOGIN_URL);
            HttpResponse loginGetResponse = httpClient.execute(loginGet);
            System.out.println("Login GET status: " + loginGetResponse.getStatusLine());
            HttpEntity loginGetEntity = loginGetResponse.getEntity();
            String loginGetHtml = EntityUtils.toString(loginGetEntity, "UTF-8");

            Document loginDoc = Jsoup.parse(loginGetHtml);
            Map<String, String> formData = parseInitialFormData(loginDoc);
            formData.put("txtUserName", username.toUpperCase());
            formData.put("txtPassword", password);
            formData.put("btnSubmit", "Đăng nhập");

            List<NameValuePair> urlParameters = new ArrayList<>();
            for (Map.Entry<String, String> entry : formData.entrySet()) {
                urlParameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }

            HttpPost loginPost = new HttpPost(LOGIN_URL);
            HttpEntity formEntity = new UrlEncodedFormEntity(urlParameters, "UTF-8");
            loginPost.setEntity(formEntity);

            HttpResponse loginPostResponse = httpClient.execute(loginPost);
            System.out.println("Login POST status: " + loginPostResponse.getStatusLine()
                    + ", Location: " + loginPostResponse.getFirstHeader("Location"));
            HttpEntity loginPostEntity = loginPostResponse.getEntity();
            String loginPostHtml = EntityUtils.toString(loginPostEntity, "UTF-8");

            Document postLoginDoc = Jsoup.parse(loginPostHtml);
            String wrongPass = postLoginDoc.select("#lblErrorInfo").text();
            System.out.println("Login POST #lblErrorInfo: '" + wrongPass + "'");

            if ("Bạn đã nhập sai tên hoặc mật khẩu!".equals(wrongPass) || "Tên đăng nhập không đúng!".equals(wrongPass)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("code", "401", "message", "Wrong Password"));
            }
            if (wrongPass != null && !wrongPass.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("code", "401", "message", wrongPass.trim()));
            }

            String cookieData = cookieStore.getCookies().toString();

            HttpGet profileGet = new HttpGet(STUDENT_PROFILE_URL);
            HttpResponse profileGetResponse = httpClient.execute(profileGet);
            HttpEntity profileGetEntity = profileGetResponse.getEntity();
            String profileGetHtml = EntityUtils.toString(profileGetEntity, "UTF-8");

            HttpGet scheduleGet = new HttpGet(STUDENT_SCHEDULE_URL);
            HttpResponse scheduleGetResponse = httpClient.execute(scheduleGet);
            HttpEntity scheduleGetEntity = scheduleGetResponse.getEntity();
            String scheduleGetHtml = EntityUtils.toString(scheduleGetEntity, "UTF-8");

            Document profileDoc = Jsoup.parse(profileGetHtml);
            Document scheduleDoc = Jsoup.parse(scheduleGetHtml);

            System.out.println("Profile GET status: " + profileGetResponse.getStatusLine()
                    + ", isLoginPage=" + isLoginPage(profileDoc)
                    + ", txtMaSV='" + profileDoc.select("input[name=txtMaSV]").val() + "'");
            System.out.println("Schedule GET status: " + scheduleGetResponse.getStatusLine()
                    + ", isLoginPage=" + isLoginPage(scheduleDoc)
                    + ", rows=" + scheduleDoc.select("tr.cssListItem, tr.cssListAlternativeItem").size());

            if (isLoginPage(profileDoc) && isLoginPage(scheduleDoc)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("code", "401", "message", "Không thể xác thực tài khoản trên cổng đào tạo. Vui lòng kiểm tra lại tên đăng nhập và mật khẩu."));
            }

            String displayName = (profileDoc.select("input[name=txtHoDem]").val() + " " + profileDoc.select("input[name=txtTen]").val()).trim();
            String studentCode = profileDoc.select("input[name=txtMaSV]").val();
            if (studentCode == null || studentCode.isBlank()) {
                studentCode = extractStudentCodeFallback(profileDoc, scheduleDoc, username);
            } else {
                studentCode = studentCode.trim().toUpperCase();
            }

            if (displayName.isBlank()) {
                displayName = extractDisplayNameFallback(profileDoc, scheduleDoc, studentCode);
            }

            String gender = profileDoc.select("select[name=drpGioiTinh] option[selected]").text();
            String birthday = profileDoc.select("input[name=txtNgaySinh]").val();
            String birthPlace = profileDoc.select("input[name=txtNoiSinh]").val();
            String idCard = profileDoc.select("input[name=txtCMTND]").val();
            String bankAccount = profileDoc.select("input[name=txtSoTaiKhoanNganHang]").val();
            String enrollSemester = profileDoc.select("input[name=txtHocKyNhapHoc]").val();
            String phone = profileDoc.select("input[name=txtDienThoaiCaNHAN]").val();
            String email = profileDoc.select("input[name=txtEmail]").val();

            // Parse student class from student code and create student record if not exists
            String studentClass = parseStudentClassFromCode(studentCode);
            Student student = Student.builder()
                    .studentCode(studentCode)
                    .studentName(displayName.isBlank() ? studentCode : displayName)
                    .studentClass(studentClass)
                    .gender(gender)
                    .birthday(birthday)
                    .birthPlace(birthPlace)
                    .idCard(idCard)
                    .bankAccount(bankAccount)
                    .enrollSemester(enrollSemester)
                    .phone(phone)
                    .email(email)
                    .build();
            if (!studentService.existByStudentCode(studentCode)) {
                studentService.createStudent(student);
            } else {
                studentService.updateStudentInfo(student);
            }

            Student persistedStudent = studentService.findByStudentCode(studentCode);
            if (persistedStudent != null) {
                if (displayName.isBlank() && persistedStudent.getStudentName() != null) {
                    displayName = persistedStudent.getStudentName();
                }
                if ((gender == null || gender.isBlank()) && persistedStudent.getGender() != null) {
                    gender = persistedStudent.getGender();
                }
                if ((birthday == null || birthday.isBlank()) && persistedStudent.getBirthday() != null) {
                    birthday = persistedStudent.getBirthday();
                }
            }

            Map<String, String> studentInfo = new HashMap<>();
            studentInfo.put("display_name", displayName.isBlank() ? studentCode : displayName);
            studentInfo.put("student_code", studentCode);
            studentInfo.put("gender", gender);
            studentInfo.put("birthday", birthday);
            studentInfo.put("birth_place", maskField(birthPlace));
            studentInfo.put("id_card", maskField(idCard));
            studentInfo.put("bank_account", maskField(bankAccount));
            studentInfo.put("enroll_semester", maskField(enrollSemester));
            studentInfo.put("phone", maskField(phone));
            studentInfo.put("email", maskEmail(email));

            Elements scheduleRows = scheduleDoc.select("tr.cssListItem, tr.cssListAlternativeItem");

            List<Map<String, String>> scheduleData = new ArrayList<>();
            for (Element row : scheduleRows) {
                Elements cells = row.select("td");
                if (cells.size() < 6) {
                    continue;
                }
                Map<String, String> rowData = new HashMap<>();
                rowData.put("course_name", cells.get(1).text());
                rowData.put("course_code", cells.get(2).text());
                String location = cells.get(4).text();
                Map<String, String> parsedSchedule = parseSchedule(cells.get(3).html().replace("<br>", "\n"), location);
                rowData.put("study_days", parsedSchedule.getOrDefault("days", ""));
                rowData.put("lessons", parsedSchedule.getOrDefault("lessons", ""));
                rowData.put("study_location", parsedSchedule.get("locations") != null ? parsedSchedule.get("locations") : location);
                rowData.put("teacher", cells.get(5).text());
                scheduleData.add(rowData);
            }

            String semesterHint = extractSemesterHint(scheduleDoc);
            String liveHash = null;
            if (!scheduleData.isEmpty()) {
                String label = studentScheduleService.deriveLabel(semesterHint, scheduleData);
                studentScheduleService.saveScheduleIfNotEmpty(studentCode, label, scheduleData);
                liveHash = studentScheduleService.computeScheduleHash(scheduleData);
            }
            List<Map<String, Object>> savedSchedules = studentScheduleService.getSavedSchedules(studentCode, liveHash);
            boolean scheduleIsCached = scheduleData.isEmpty() && !savedSchedules.isEmpty();
            Object effectiveSchedule = !scheduleData.isEmpty()
                    ? scheduleData
                    : (!savedSchedules.isEmpty() ? savedSchedules.get(0).get("schedule") : scheduleData);

            sessionService.saveUserSession(parseStudentSession(loginGetResponse.toString(),
                    username.toUpperCase()), request);

            Map<String, Object> responseData = new LinkedHashMap<>();
            responseData.put("student_info", studentInfo);
            responseData.put("student_schedule", effectiveSchedule);
            responseData.put("saved_schedules", savedSchedules);
            responseData.put("schedule_is_cached", scheduleIsCached);

            return ResponseEntity.ok(Map.of(
                    "code", "200",
                    "message", "OK",
                    "data", responseData
            ));

        } catch (IOException | ParseException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", "500", "message", "Error: " + e.getMessage()));
        }
    }

    private boolean isLoginPage(Document doc) {
        if (doc == null) {
            return true;
        }
        return doc.selectFirst("input[name=txtUserName]") != null
                && doc.selectFirst("input[name=txtPassword]") != null;
    }

    private String extractStudentCodeFallback(Document profileDoc, Document scheduleDoc, String username) {
        String[] selectors = {
                "#lblMaSV",
                "span[id*=MaSV]",
                "input[id*=MaSV]",
                "#PageHeader1_lblUserFullName",
                "span[id*=lblUserFullName]"
        };
        for (Document doc : new Document[]{profileDoc, scheduleDoc}) {
            if (doc == null) continue;
            for (String sel : selectors) {
                Element el = doc.selectFirst(sel);
                if (el != null) {
                    String val = el.tagName().equalsIgnoreCase("input") ? el.val() : el.text();
                    Matcher m = Pattern.compile("((?:AT|CT|DT)\\d+)", Pattern.CASE_INSENSITIVE).matcher(val);
                    if (m.find()) {
                        return m.group(1).toUpperCase();
                    }
                }
            }
        }
        return username != null ? username.trim().toUpperCase() : "";
    }

    private String extractDisplayNameFallback(Document profileDoc, Document scheduleDoc, String studentCode) {
        String[] selectors = {
                "#lblHoTen",
                "span[id*=HoTen]",
                "#PageHeader1_lblUserFullName",
                "span[id*=lblUserFullName]"
        };
        for (Document doc : new Document[]{profileDoc, scheduleDoc}) {
            if (doc == null) continue;
            for (String sel : selectors) {
                Element el = doc.selectFirst(sel);
                if (el != null) {
                    String text = el.text().trim();
                    if (!text.isBlank()) {
                        return text.replaceAll("\\s*\\([^)]*\\)\\s*$", "").trim();
                    }
                }
            }
        }
        return studentCode != null ? studentCode : "";
    }

    private String extractSemesterHint(Document scheduleDoc) {
        if (scheduleDoc == null) {
            return null;
        }
        String[] selectors = {
                "select[name*=Term] option[selected]",
                "select[name*=Semester] option[selected]",
                "select[name*=HocKy] option[selected]",
                "select[id*=Term] option[selected]",
                "select[id*=Semester] option[selected]"
        };
        for (String selector : selectors) {
            Element selected = scheduleDoc.selectFirst(selector);
            if (selected != null) {
                String text = selected.text().trim();
                if (!text.isBlank() && !text.toLowerCase().contains("chọn")) {
                    return text;
                }
            }
        }
        return null;
    }

    public String getLoginPayload(String username, String password) {
        StringBuilder payload = new StringBuilder();
        payload.append("txtUserName=").append(username)
                .append("&txtPassword=").append(password)
                .append("&btnSubmit=Đăng+nhập");
        return payload.toString();
    }

    public ResponseEntity<?> loginVirtualCalendar(String username, String password, HttpSession session, HttpServletRequest request) throws IOException {
        List<VirtualCalendarResponse> result = new ArrayList<>();
        if (username == null || password == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "400", "message", "Missing Item"));
        }

        try {
            // Step 1: Login
            HttpGet loginGet = new HttpGet(LOGIN_URL);
            HttpResponse loginGetResponse = httpClient.execute(loginGet, context);
            HttpEntity loginGetEntity = loginGetResponse.getEntity();
            String loginGetHtml = EntityUtils.toString(loginGetEntity);

            Document loginDoc = Jsoup.parse(loginGetHtml);
            Map<String, String> formData = parseInitialFormData(loginDoc);
            formData.put("txtUserName", username.toUpperCase());
            formData.put("txtPassword", password);
            formData.put("btnSubmit", "Đăng nhập");

            List<NameValuePair> urlParameters = new ArrayList<>();
            for (Map.Entry<String, String> entry : formData.entrySet()) {
                urlParameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }

            HttpPost loginPost = new HttpPost(LOGIN_URL);
            HttpEntity formEntity = new UrlEncodedFormEntity(urlParameters);
            loginPost.setEntity(formEntity);

            HttpResponse loginPostResponse = httpClient.execute(loginPost, context);
            HttpEntity loginPostEntity = loginPostResponse.getEntity();
            String loginPostHtml = EntityUtils.toString(loginPostEntity);

            Document postLoginDoc = Jsoup.parse(loginPostHtml);
            String wrongPass = postLoginDoc.select("#lblErrorInfo").text();

            if ("Bạn đã nhập sai tên hoặc mật khẩu!".equals(wrongPass) || "Tên đăng nhập không đúng!".equals(wrongPass)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("code", "401", "message", "Wrong Password"));
            }

            // Save student session after successful login
            String cookieData = cookieStore.getCookies().toString();

            session.setAttribute("username", username);
            session.setAttribute("cookieStore", cookieStore);
            session.setAttribute("context", context);
            session.setAttribute("httpClient", httpClient);

            // Fetch student profile info
            HttpGet profileGet = new HttpGet(STUDENT_PROFILE_URL);
            HttpResponse profileGetResponse = httpClient.execute(profileGet, context);
            String profileGetHtml = EntityUtils.toString(profileGetResponse.getEntity());
            Document profileDoc = Jsoup.parse(profileGetHtml);
            String displayName = profileDoc.select("input[name=txtHoDem]").val() + " " + profileDoc.select("input[name=txtTen]").val();
            String studentCode = profileDoc.select("input[name=txtMaSV]").val();
            String gender = profileDoc.select("select[name=drpGioiTinh] option[selected]").text();
            String birthday = profileDoc.select("input[name=txtNgaySinh]").val();
            String birthPlace = profileDoc.select("input[name=txtNoiSinh]").val();
            String idCard = profileDoc.select("input[name=txtCMTND]").val();
            String bankAccount = profileDoc.select("input[name=txtSoTaiKhoanNganHang]").val();
            String enrollSemester = profileDoc.select("input[name=txtHocKyNhapHoc]").val();
            String phone = profileDoc.select("input[name=txtDienThoaiCaNHAN]").val();
            String email = profileDoc.select("input[name=txtEmail]").val();

            Map<String, String> studentInfo = new HashMap<>();
            studentInfo.put("display_name", displayName);
            studentInfo.put("student_code", studentCode);
            studentInfo.put("gender", gender);
            studentInfo.put("birthday", birthday);
            studentInfo.put("birth_place", maskField(birthPlace));
            studentInfo.put("id_card", maskField(idCard));
            studentInfo.put("bank_account", maskField(bankAccount));
            studentInfo.put("enroll_semester", maskField(enrollSemester));
            studentInfo.put("phone", maskField(phone));
            studentInfo.put("email", maskEmail(email));

            // Step 2: Get Academic Years
            HttpGet calendarGet = new HttpGet(STUDENT_VIRTUAL_CALENDAR);
            HttpResponse calendarResponse = httpClient.execute(calendarGet, context);
            HttpEntity calendarEntity = calendarResponse.getEntity();
            String calendarHtml = EntityUtils.toString(calendarEntity);

            Document calendarDoc = Jsoup.parse(calendarHtml);
            List<String> validCourse = new ArrayList<>();
            // Extract academic year options
            Elements academicYearOptions = calendarDoc.select("select[name=drpAcademicYear] option");
            List<String> academicYears = new ArrayList<>();
            for (Element option : academicYearOptions) {
                String text = option.text();
                if (!text.isEmpty()) {
                    validCourse.add(text);
                    academicYears.add(option.attr("value"));
                }
            }

            Elements drpFields = calendarDoc.select("select[name=drpField] option");
            String drpFieldValue = "";
            for (Element option : drpFields) {
                String text = option.text();
                if (!text.isEmpty()) {
                    drpFieldValue = text;
                    break;
                }
            }

            // Extract hidden form fields
            Map<String, String> hiddenFields = new HashMap<>();
            Elements hiddenInputs = calendarDoc.select("input[type=hidden]");
            for (Element input : hiddenInputs) {
                hiddenFields.put(input.attr("name"), input.attr("value"));
            }

            // Extract courses and their classes for each academic year
            List<Map<String, Object>> courseClassDetails = new ArrayList<>();
            int count = 0;
            for (String academicYear : academicYears) {
                // Prepare the parameters to submit to get courses
                List<NameValuePair> courseParams = new ArrayList<>();
                courseParams.add(new BasicNameValuePair("__EVENTTARGET", "drpAcademicYear"));
                courseParams.add(new BasicNameValuePair("__EVENTARGUMENT", ""));
                courseParams.add(new BasicNameValuePair("drpAcademicYear", academicYear));
//                courseParams.add(new BasicNameValuePair("drpField", drpFieldValue));
                for (Map.Entry<String, String> entry : hiddenFields.entrySet()) {
                    courseParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                }
                HttpPost coursePost = new HttpPost(STUDENT_VIRTUAL_CALENDAR);
                HttpEntity courseFormEntity = new UrlEncodedFormEntity(courseParams, "UTF-8");
                coursePost.setEntity(courseFormEntity);

                HttpResponse courseResponse = httpClient.execute(coursePost, context);
                HttpEntity courseEntity = courseResponse.getEntity();
                String courseHtml = EntityUtils.toString(courseEntity);

                Document courseDoc = Jsoup.parse(courseHtml);
                Elements courseOptions = courseDoc.select("select[name=drpCourse] option");

                for (Element courseOption : courseOptions) {
                    String courseName = courseOption.text();
                    String courseValue = courseOption.attr("value");
                    if (!courseName.isEmpty() && !courseName.equals("Chọn học phần để hiển thị các lớp học")) {
                        // Prepare the parameters to submit to view course classes
                        List<NameValuePair> classParams = new ArrayList<>();
                        classParams.add(new BasicNameValuePair("drpCourse", courseValue));
                        classParams.add(new BasicNameValuePair("drpWeekDay", "0")); // Example value
                        classParams.add(new BasicNameValuePair("btnViewCourseClass", "Hiển thị lớp")); // Add button value here

                        Elements classHiddenInputs = courseDoc.select("input[type=hidden]");
                        for (Element input : classHiddenInputs) {
                            classParams.add(new BasicNameValuePair(input.attr("name"), input.attr("value")));
                        }

                        HttpPost classPost = new HttpPost(STUDENT_VIRTUAL_CALENDAR);
                        HttpEntity classFormEntity = new UrlEncodedFormEntity(classParams);
                        classPost.setEntity(classFormEntity);

                        HttpResponse classResponse = httpClient.execute(classPost, context);
                        HttpEntity classEntity = classResponse.getEntity();
                        String classHtml = EntityUtils.toString(classEntity);

                        Document classDoc = Jsoup.parse(classHtml);
                        Elements classRows = classDoc.select("#gridRegistration tr.cssListItem, #gridRegistration tr.cssListAlternativeItem");

                        for (Element row : classRows) {
                            // Extract class details
                            String className = row.select("td").get(2).text(); // Class name
                            String coursePart = row.select("td").get(3).text(); // Course part
                            String baseTime = row.select("td").get(4).text(); // Time
                            String time = row.select("td").get(4).text(); // Time
                            String location = row.select("td").get(5).text(); // Location
                            String lecturer = row.select("td").get(6).text(); // Lecturer
                            Map<String, String> afterParse = this.parseVirtualCalendar(time, location);

                            TimelineResponse timelineResponse = TimelineResponse.builder()
                                    .courseCode(coursePart)
                                    .courseName(className)
                                    .studyLocation(afterParse.get("locations") != null ? afterParse.get("locations") : location)
                                    .teacher(lecturer)
                                    .lessons(afterParse.get("lessons"))
                                    .studyDays(afterParse.get("days"))
                                    .build();

                            VirtualCalendarResponse virtualCalendarResponse = VirtualCalendarResponse.builder()
                                    .course(validCourse.get(count))
                                    .timelineResponse(timelineResponse)
                                    .baseTime(baseTime)
                                    .courseName(courseName)
                                    .build();

                            result.add(virtualCalendarResponse);
                        }
                    }
                }
                count++;
            }
            sessionService.saveUserSession(parseStudentSession(loginGetResponse.toString(),
                    username.toUpperCase()), request);
            return ResponseEntity.ok(Map.of(
                    "code", "200",
                    "message", "OK",
                    "data", Map.of(
                            "student_info", studentInfo,
                            "virtual_calendar", result
                    )
            ));

        } catch (IOException | ParseException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", "500", "message", "Error: " + e.getMessage()));
        }
    }

    private Map<Integer, String> parseRoomPhases(String locationStr) {
        Map<Integer, String> phaseToRoom = new HashMap<>();
        if (locationStr == null || locationStr.isBlank()) {
            return phaseToRoom;
        }

        Pattern pattern = Pattern.compile("\\((\\d+(?:,\\d+)*)\\)");
        Matcher matcher = pattern.matcher(locationStr);

        List<String> phasesStr = new ArrayList<>();
        List<Integer> matchStarts = new ArrayList<>();
        List<Integer> matchEnds = new ArrayList<>();

        while (matcher.find()) {
            phasesStr.add(matcher.group(1));
            matchStarts.add(matcher.start());
            matchEnds.add(matcher.end());
        }

        if (phasesStr.isEmpty()) {
            phaseToRoom.put(-1, locationStr.trim());
            return phaseToRoom;
        }

        for (int i = 0; i < phasesStr.size(); i++) {
            int startOfNext = (i + 1 < phasesStr.size()) ? matchStarts.get(i + 1) : locationStr.length();
            String room = locationStr.substring(matchEnds.get(i), startOfNext).trim();

            if (room.startsWith(":") || room.startsWith("-")) {
                room = room.substring(1).trim();
            }

            String[] parts = phasesStr.get(i).split(",");
            for (String part : parts) {
                try {
                    phaseToRoom.put(Integer.parseInt(part.trim()), room);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }

        return phaseToRoom;
    }

    private Map<String, String> parseVirtualCalendar(String studySchedule, String locationStr) throws ParseException {
        // More tolerant parser for virtual calendar blocks.
        // Handles multiple ranges like:
        // "Từ 19/01/2026 đến 08/02/2026: (1) Thứ 2 tiết 10,11,12 (LT) Thứ 4 tiết 10,11,12 (LT)
        //  Từ 02/03/2026 đến 12/04/2026: (2) Thứ 2 tiết 10,11,12 (LT) Thứ 4 tiết 10,11,12 (LT)"
        // Also handles ranges that have no weekday info (will be skipped):
        // "Từ 04/05/2026 đến 31/05/2026: (LT) (Lý thuyết)"
        Map<String, String> result = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        Map<Integer, String> roomPhases = parseRoomPhases(locationStr);

        // Regex to capture each range and its content until the next "Từ ... đến" or end of string
        Pattern rangePattern = Pattern.compile(
                "Từ\\s*(\\d{2}/\\d{2}/\\d{4})\\s*đến\\s*(\\d{2}/\\d{2}/\\d{4})\\s*:?(.*?)(?=Từ\\s*\\d{2}/\\d{2}/\\d{4}\\s*đến|$)",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        // Regex to capture weekday + lesson list inside a range
        Pattern weekdayPattern = Pattern.compile("(Thứ\\s*(\\d)|Chủ\\s*nhật)\\s*tiết\\s*([\\d,]+)", Pattern.CASE_INSENSITIVE);

        Matcher rangeMatcher = rangePattern.matcher(studySchedule);
        while (rangeMatcher.find()) {
            String startStr = rangeMatcher.group(1);
            String endStr = rangeMatcher.group(2);
            String body = rangeMatcher.group(3);

            Date startDate = sdf.parse(startStr);
            Date endDate = sdf.parse(endStr);

            // Parse phase index
            Pattern phasePattern = Pattern.compile("\\((\\d+)\\)");
            Matcher phaseMatcher = phasePattern.matcher(body);
            int phaseNum = -1;
            if (phaseMatcher.find()) {
                phaseNum = Integer.parseInt(phaseMatcher.group(1));
            }

            // Determine location for this phase
            String room = locationStr;
            if (roomPhases.containsKey(phaseNum)) {
                room = roomPhases.get(phaseNum);
            } else if (roomPhases.containsKey(-1)) {
                room = roomPhases.get(-1);
            }

            Matcher weekdayMatcher = weekdayPattern.matcher(body);
            boolean foundWeekday = false;
            while (weekdayMatcher.find()) {
                foundWeekday = true;
                String dayStr = weekdayMatcher.group(2);          // e.g. "2"
                String lessonsStr = weekdayMatcher.group(3);      // e.g. "10,11,12"
                if (dayStr == null) {
                    dayStr = "CN"; // Chủ nhật
                }

                int targetDow = dayOfWeekToCalendarDay(dayStr);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(startDate);
                while (!calendar.getTime().after(endDate)) {
                    int dow = calendar.get(Calendar.DAY_OF_WEEK);
                    if (dow == targetDow) {
                        String dateStr = sdf.format(calendar.getTime());
                        if (!result.containsKey("days")) {
                            result.put("days", dateStr);
                            result.put("lessons", lessonsStr);
                            result.put("locations", room);
                        } else {
                            result.put("days", result.get("days") + " " + dateStr);
                            result.put("lessons", result.get("lessons") + " " + lessonsStr);
                            result.put("locations", result.get("locations") + ";" + room);
                        }
                    }
                    calendar.add(Calendar.DATE, 1);
                }
            }

            // If no weekday info is found in this block, just ignore that block (prevents crashes)
            if (!foundWeekday) {
                continue;
            }
        }
        return result;
    }

    private Map<String, String> parseScheduleFromRegistration(String time) throws ParseException {

        return null;
    }

    private Map<String, String> parseInitialFormData(Document doc) {
        Map<String, String> formData = new HashMap<>();
        for (Element input : doc.select("input:not([type=submit]):not([type=button]):not([type=reset]):not([type=image])")) {
            String name = input.attr("name");
            if (!name.isBlank()) {
                formData.put(name, input.attr("value"));
            }
        }
        return formData;
    }

    private String md5(String input) {
        return org.apache.commons.codec.digest.DigestUtils.md5Hex(input);
    }

    /**
     * Parse student class from student code Example: CT070218 -> CT7B (02 is
     * the 2nd class, so B) DT050103 -> DT5A (01 is the 1st class, so A)
     * Pattern: First 2 chars (major) + chars 2-3 (year, remove leading zero) +
     * letter from chars 4-5 (class number) Class number: 01 -> A, 02 -> B, 03
     * -> C, ..., 26 -> Z
     */
    private String parseStudentClassFromCode(String studentCode) {
        if (studentCode == null || studentCode.length() < 6) {
            return null;
        }

        try {
            // Extract components
            String major = studentCode.substring(0, 2); // CT, DT, etc.
            String yearStr = studentCode.substring(2, 4); // 07, 05, etc.
            String classNumStr = studentCode.substring(4, 6); // 02, 01, etc.

            // Remove leading zero from year
            int year = Integer.parseInt(yearStr);
            String yearWithoutZero = String.valueOf(year);

            // Convert class number to letter (01 -> A, 02 -> B, etc.)
            int classNum = Integer.parseInt(classNumStr);
            char classLetter = (char) ('A' + classNum - 1);

            // Combine: major + year + letter
            return major + yearWithoutZero + classLetter;
        } catch (Exception e) {
            // If parsing fails, return null
            return null;
        }
    }

    private Map<String, String> parseSchedule(String studySchedule, String locationStr) throws ParseException {
        String cleanText = Jsoup.parse(studySchedule).text();
        return parseVirtualCalendar(cleanText, locationStr);
    }

    private int dayOfWeekToCalendarDay(String dayOfWeek) {
        switch (dayOfWeek) {
            case "2":
                return Calendar.MONDAY;
            case "3":
                return Calendar.TUESDAY;
            case "4":
                return Calendar.WEDNESDAY;
            case "5":
                return Calendar.THURSDAY;
            case "6":
                return Calendar.FRIDAY;
            case "7":
                return Calendar.SATURDAY;
            default:
                return Calendar.SUNDAY;
        }
    }

    private static int getDayOfWeek(String line) {
        if (line.contains("Thứ 2")) {
            return Calendar.MONDAY;
        }
        if (line.contains("Thứ 3")) {
            return Calendar.TUESDAY;
        }
        if (line.contains("Thứ 4")) {
            return Calendar.WEDNESDAY;
        }
        if (line.contains("Thứ 5")) {
            return Calendar.THURSDAY;
        }
        if (line.contains("Thứ 6")) {
            return Calendar.FRIDAY;
        }
        if (line.contains("Thứ 7")) {
            return Calendar.SATURDAY;
        }
        if (line.contains("Chủ nhật")) {
            return Calendar.SUNDAY;
        }
        return -1;
    }

    public static List<Map<String, String>> extractTableData(String html) {
        List<Map<String, String>> rowsData = new ArrayList<>();

        // Phân tích HTML
        Document doc = Jsoup.parse(html);
        Element table = doc.getElementById("gridRegistration");

        if (table != null) {
            // Chọn tất cả các hàng trong tbody
            Elements rows = table.select("tbody tr.cssRangeItem2");

            for (Element row : rows) {
                Map<String, String> rowData = new HashMap<>();

                // Lấy dữ liệu từ các cột
                rowData.put("STT", row.select("td").get(0).text().trim());
                rowData.put("Chọn", row.select("td input[type=radio]").attr("value").trim());
                rowData.put("Lớp học phần", row.select("td span[id^=gridRegistration_lblCourseClass_]").text().trim());
                rowData.put("Học phần", row.select("td span[id^=gridRegistration_lblCourseName_]").text().trim());
                rowData.put("Thời gian", row.select("td").get(4).text().trim());
                rowData.put("Địa điểm", row.select("td").get(5).text().trim());
                rowData.put("Giảng viên", row.select("td").get(6).text().trim());
                rowData.put("Sĩ số", row.select("td span[id^=gridRegistration_lblExpectationStudent_]").text().trim());
                rowData.put("Đã ĐK", row.select("td span[id^=gridRegistration_lblCurrentStudent_]").text().trim());

                rowsData.add(rowData);
            }
        }

        return rowsData;
    }

    private String maskField(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        int keep = Math.min(2, value.length());
        return value.substring(0, keep) + "*".repeat(Math.max(0, value.length() - keep));
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return maskField(email);
        }
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        int keep = Math.min(2, local.length());
        return local.substring(0, keep) + "*".repeat(local.length() - keep) + domain;
    }

}
