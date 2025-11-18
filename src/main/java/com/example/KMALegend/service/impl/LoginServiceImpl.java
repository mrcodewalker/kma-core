package com.example.KMALegend.service.impl;

import com.example.KMALegend.common.responses.TimelineResponse;
import com.example.KMALegend.common.responses.VirtualCalendarResponse;
import com.example.KMALegend.entity.StudentSessions;
import com.example.KMALegend.repository.StudentSessionsRepository;
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

@Service
@RequiredArgsConstructor
public class LoginServiceImpl{
    private CookieStore cookieStore;
    private HttpClientContext context;
    private CloseableHttpClient httpClient;
    private final StudentSessionServiceImpl sessionService;
    private static final String LOGIN_URL = "http://qldt.actvn.edu.vn/CMCSoft.IU.Web.Info/Login.aspx";
    private static final String STUDENT_PROFILE_URL = "http://qldt.actvn.edu.vn/CMCSoft.IU.Web.Info/StudentProfileNew/HoSoSinhVien.aspx";
    private static final String STUDENT_SCHEDULE_URL = "http://qldt.actvn.edu.vn/CMCSoft.IU.Web.Info/Reports/Form/StudentTimeTable.aspx";
    private static final String STUDENT_VIRTUAL_CALENDAR = "http://qldt.actvn.edu.vn/cmcsoft.iu.web.info/StudyRegister/StudyRegister.aspx";
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
        int endIndex = targetSplit.indexOf(";", startIndex);

        String cookieData = (startIndex!=-1) ? targetSplit.substring(startIndex,endIndex) : "NULL COOKIE DATA";
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
            HttpEntity loginGetEntity = loginGetResponse.getEntity();
            String loginGetHtml = EntityUtils.toString(loginGetEntity);

            Document loginDoc = Jsoup.parse(loginGetHtml);
            Map<String, String> formData = parseInitialFormData(loginDoc);
            formData.put("txtUserName", username.toUpperCase());
            formData.put("txtPassword", this.md5(password));
            formData.put("btnSubmit", "Đăng nhập");

            List<NameValuePair> urlParameters = new ArrayList<>();
            for (Map.Entry<String, String> entry : formData.entrySet()) {
                urlParameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }

            HttpPost loginPost = new HttpPost(LOGIN_URL);
            HttpEntity formEntity = new UrlEncodedFormEntity(urlParameters);
            loginPost.setEntity(formEntity);

            HttpResponse loginPostResponse = httpClient.execute(loginPost);
            HttpEntity loginPostEntity = loginPostResponse.getEntity();
            String loginPostHtml = EntityUtils.toString(loginPostEntity);

            Document postLoginDoc = Jsoup.parse(loginPostHtml);
            String wrongPass = postLoginDoc.select("#lblErrorInfo").text();

            if ("Bạn đã nhập sai tên hoặc mật khẩu!".equals(wrongPass) || "Tên đăng nhập không đúng!".equals(wrongPass)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("code", "401", "message", "Wrong Password"));
            }

            String cookieData = cookieStore.getCookies().toString();

            HttpGet profileGet = new HttpGet(STUDENT_PROFILE_URL);
            HttpResponse profileGetResponse = httpClient.execute(profileGet);
            HttpEntity profileGetEntity = profileGetResponse.getEntity();
            String profileGetHtml = EntityUtils.toString(profileGetEntity);

            Document profileDoc = Jsoup.parse(profileGetHtml);
            String displayName = profileDoc.select("input[name=txtHoDem]").val() + " " + profileDoc.select("input[name=txtTen]").val();
            String studentCode = profileDoc.select("input[name=txtMaSV]").val();
            String gender = profileDoc.select("select[name=drpGioiTinh] option[selected]").text();
            String birthday = profileDoc.select("input[name=txtNgaySinh]").val();

            Map<String, String> studentInfo = new HashMap<>();
            studentInfo.put("display_name", displayName);
            studentInfo.put("student_code", studentCode);
            studentInfo.put("gender", gender);
            studentInfo.put("birthday", birthday);

            HttpGet scheduleGet = new HttpGet(STUDENT_SCHEDULE_URL);
            HttpResponse scheduleGetResponse = httpClient.execute(scheduleGet);
            HttpEntity scheduleGetEntity = scheduleGetResponse.getEntity();
            String scheduleGetHtml = EntityUtils.toString(scheduleGetEntity);

            Document scheduleDoc = Jsoup.parse(scheduleGetHtml);
            Elements scheduleRows = scheduleDoc.select("tr.cssListItem, tr.cssListAlternativeItem");

            List<Map<String, String>> scheduleData = new ArrayList<>();
            for (Element row : scheduleRows) {
                Map<String, String> rowData = new HashMap<>();
                rowData.put("course_name", row.select("td").get(1).text());
                rowData.put("course_code", row.select("td").get(2).text());
//                rowData.put("studySchedule", row.select("td").get(3).html().replace("<br>", "\n"));
                Map<String, String> parsedSchedule = parseSchedule(row.select("td").get(3).html().replace("<br>", "\n"));
                rowData.put("study_days", parsedSchedule.get("days"));
                rowData.put("lessons", parsedSchedule.get("lessons"));
                rowData.put("study_location", row.select("td").get(4).text());
                rowData.put("teacher", row.select("td").get(5).text());
                scheduleData.add(rowData);
            }

//            String token = Jwts.builder()
//                    .setSubject(username)
//                    .setIssuedAt(new Date())
//                    .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000L))
//                    .signWith(SignatureAlgorithm.HS256, getSigninKey())
//                    .compact();
            sessionService.saveUserSession(parseStudentSession(loginGetResponse.toString(),
                    username.toUpperCase()), request);

            return ResponseEntity.ok(Map.of(
                    "code", "200",
                    "message", "OK",
                    "data", Map.of(
                            "student_info", studentInfo,
                            "student_schedule", scheduleData
                    )
            ));

        } catch (IOException | ParseException e) {
//            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", "500", "message", "Error: " + e.getMessage()));
        }
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
            formData.put("txtPassword", md5(password));
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
                            Map<String, String> afterParse = this.parseVirtualCalendar(time);


                            TimelineResponse timelineResponse = TimelineResponse.builder()
                                    .courseCode(coursePart)
                                    .courseName(className)
                                    .studyLocation(location)
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
                    "virtual_calendar", result
            ));

        } catch (IOException | ParseException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", "500", "message", "Error: " + e.getMessage()));
        }
    }


    private Map<String, String> parseVirtualCalendar(String studySchedule) throws ParseException {
        Map<String, String> result = new HashMap<>();
        List<String> startDay = new ArrayList<>();
        List<String> endDay = new ArrayList<>();
        List<String> lessons = new ArrayList<>();
        List<String> dayInWeek = new ArrayList<>();
        String[] line = studySchedule.split("Từ");
        for (String clone : line){
            if (clone.length()<2) continue;
            String[] splitOver = clone.split(":");
            String start = "";
            String end = "";
            String day = "";
            String lesson = "";
            start = splitOver[0].substring(0, splitOver[0].indexOf("đến")).trim();
            end = splitOver[0].substring(splitOver[0].lastIndexOf("đến")+3).trim();

            String[] findLessons = splitOver[1].split("Thứ");
            Calendar calendar = Calendar.getInstance();

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            for (String each: findLessons) {
                if (each.length() > 5) {
                    day = each.substring(0, each.indexOf("tiết")).trim();
                    lesson = each.substring(each.indexOf("tiết") + 4, each.indexOf("(")).trim();
                    dayInWeek.add(day);
                    lessons.add(lesson);
                    Date starts = sdf.parse(start);
                    Date ends = sdf.parse(end);
                    calendar.setTime(starts);
                    while (!calendar.getTime().after(ends)) {
                        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                        if (dayOfWeek == dayOfWeekToCalendarDay(day+"")) {
                            String dateStr = sdf.format(calendar.getTime());
                            if (result.get("days")==null){
                                result.put("days", dateStr);
                                result.put("lessons", lesson);
                            } else {
                                result.put("days", result.get("days")+" "+dateStr);
                                result.put("lessons", result.get("lessons")+" "+lesson);
                            }
                        }
                        calendar.add(Calendar.DATE, 1);
                    }
                }
            }

        }
        return result;
    }
    private Map<String, String> parseScheduleFromRegistration(String time) throws ParseException{

        return null;
    }
    private Map<String, String> parseInitialFormData(Document doc) {
        Map<String, String> formData = new HashMap<>();
        for (Element input : doc.select("input")) {
            formData.put(input.attr("name"), input.attr("value"));
        }
        return formData;
    }

    private String md5(String input) {
        return org.apache.commons.codec.digest.DigestUtils.md5Hex(input);
    }
    private Map<String, String> parseSchedule(String studySchedule) throws ParseException {
        // Loại bỏ các thẻ HTML bằng cách sử dụng Jsoup để lấy văn bản thuần túy
        Document doc = Jsoup.parse(studySchedule);
        String cleanText = doc.body().text();
        String[] lines = studySchedule.split("\n");
        // Tách các đoạn văn bản bằng cách phân tách theo các ký tự xuống dòng
        List<String> startDay = new ArrayList<>();
        List<String> endDay = new ArrayList<>();
        List<String> lesson = new ArrayList<>();
        List<String> dayInWeek = new ArrayList<>();
        Map<String, String> result = new HashMap<>();
        int check = 0;
        String start = "";
        String end = "";
        for (String line : lines){
            Calendar calendar = Calendar.getInstance();

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            if (line.contains("Từ")||
                    line.contains("đến")) {
                line = line.substring(0, line.indexOf(":"));
                start = "";
                end = "";
                check=1;
            } else {
                int i = 1 + line.indexOf(">");
                int j = line.lastIndexOf("<");
                line = line.substring(i, j);
                check=0;
            }
            String[] parts = line.split(" ");
            int day = 2;
            if (check==1){
                start=parts[1];
                end=parts[3];
            } else {
                day = Integer.parseInt(parts[1]);
                dayInWeek.add(parts[1]);
                Date starts = sdf.parse(start);
                Date ends = sdf.parse(end);
                calendar.setTime(starts);
                while (!calendar.getTime().after(ends)) {
                    int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                    if (dayOfWeek == dayOfWeekToCalendarDay(day+"")) {
                        String dateStr = sdf.format(calendar.getTime());
                        if (result.get("days")==null){
                            result.put("days", dateStr);
                            result.put("lessons", parts[3]);

                        } else {
                            result.put("days", result.get("days")+" "+dateStr);
                            result.put("lessons", result.get("lessons")+" "+parts[3]);
                        }
                    }
                    calendar.add(Calendar.DATE, 1);
                }
            }
        }
        return result;
    }

    private int dayOfWeekToCalendarDay(String dayOfWeek) {
        switch (dayOfWeek) {
            case "2": return Calendar.MONDAY;
            case "3": return Calendar.TUESDAY;
            case "4": return Calendar.WEDNESDAY;
            case "5": return Calendar.THURSDAY;
            case "6": return Calendar.FRIDAY;
            case "7": return Calendar.SATURDAY;
            default: return Calendar.SUNDAY;
        }
    }
    private static int getDayOfWeek(String line) {
        if (line.contains("Thứ 2")) return Calendar.MONDAY;
        if (line.contains("Thứ 3")) return Calendar.TUESDAY;
        if (line.contains("Thứ 4")) return Calendar.WEDNESDAY;
        if (line.contains("Thứ 5")) return Calendar.THURSDAY;
        if (line.contains("Thứ 6")) return Calendar.FRIDAY;
        if (line.contains("Thứ 7")) return Calendar.SATURDAY;
        if (line.contains("Chủ nhật")) return Calendar.SUNDAY;
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

}
