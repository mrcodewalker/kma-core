# KMALegend API Changes — Frontend Integration Guide

Base URL: `http://localhost:8765`

Tất cả request body được **mã hóa** (RSA + AES). Response trả về plain JSON.

> **Masking rule chung:**
> - `birth_place`, `id_card`, `bank_account`, `enroll_semester`, `phone`: giữ 2 ký tự đầu, còn lại `*`
> - `email`: giữ 2 ký tự đầu phần local, domain giữ nguyên. VD: `example@gmail.com` → `ex******@gmail.com`
> - Áp dụng cho cả `/login` lẫn `/virtual-calendar`

---

## 1. POST `/api/v1/auth/login`

Đăng nhập, trả về thông tin sinh viên (đã mask) + thời khóa biểu hiện tại.

### Request Body (encrypted)
```json
{
  "username": "CT070218",
  "password": "yourpassword"
}
```

### Response `200`
```json
{
  "code": "200",
  "message": "OK",
  "data": {
    "student_info": {
      "display_name": "Nguyễn Văn An",
      "student_code": "CT070218",
      "gender": "Nam",
      "birthday": "01/01/2000",
      "birth_place": "Hà**",
      "id_card": "01**********",
      "bank_account": "12********",
      "enroll_semester": "20**",
      "phone": "09********",
      "email": "ex******@gmail.com"
    },
    "student_schedule": [
      {
        "course_name": "Lập trình Java",
        "course_code": "INT1234",
        "study_days": "14/04/2026 21/04/2026",
        "lessons": "1,2,3 1,2,3",
        "study_location": "P.101",
        "teacher": "Nguyễn Văn B"
      }
    ]
  }
}
```

### Response `401`
```json
{ "code": "401", "message": "Wrong Password" }
```

### Response `400`
```json
{ "code": "400", "message": "Missing Item" }
```

---

## 2. POST `/api/v1/auth/virtual-calendar`

Đăng nhập và lấy lịch đăng ký học phần (bảng ảo). Trả về thông tin sinh viên **đã mask** + danh sách lịch học.

### Request Body (encrypted)
```json
{
  "username": "CT070218",
  "password": "yourpassword"
}
```

### Response `200`
```json
{
  "code": "200",
  "message": "OK",
  "data": {
    "student_info": {
      "display_name": "Nguyễn Văn An",
      "student_code": "CT070218",
      "gender": "Nam",
      "birthday": "01/01/2000",
      "birth_place": "Hà**",
      "id_card": "01**********",
      "bank_account": "12********",
      "enroll_semester": "20**",
      "phone": "09********",
      "email": "ex******@gmail.com"
    },
    "virtual_calendar": [
      {
        "course": "2024-2025",
        "courseName": "Lập trình Java",
        "baseTime": "Từ 14/04/2026 đến 30/06/2026: Thứ 2 tiết 1,2,3",
        "timelineResponse": {
          "courseCode": "INT1234",
          "courseName": "LT Java - CT7B",
          "studyLocation": "P.101",
          "teacher": "Nguyễn Văn B",
          "studyDays": "14/04/2026 21/04/2026",
          "lessons": "1,2,3 1,2,3"
        }
      }
    ]
  }
}
```

---

## 3. GET `/api/v1/scores/users/{studentCode}`

Lấy bảng điểm theo mã sinh viên. Tên sinh viên và tên môn học được **mask**.

### Path Param
| Param | Type | Mô tả |
|-------|------|--------|
| `studentCode` | string | Mã sinh viên, VD: `CT070218` |

### Response `200`
```json
{
  "studentDTO": {
    "studentCode": "CT070218",
    "studentName": "N* V* A*",
    "studentClass": "CT7B"
  },
  "scoreDTOS": [
    {
      "scoreText": "A",
      "scoreFirst": 8.5,
      "scoreSecond": 9.0,
      "scoreFinal": 8.0,
      "scoreOverall": 8.5,
      "subjectName": "Lập***********"
    }
  ]
}
```

> - `studentName`: mỗi từ giữ ký tự đầu + `*`. VD: `Nguyễn Văn An` → `N* V* A*`
> - `subjectName`: giữ 3 ký tự đầu, còn lại `*`

---

## 4. POST `/api/v1/score-batch/create-or-update`

Tạo hoặc cập nhật bảng điểm batch.

### Request Body (encrypted)
```json
{
  "studentInfo": {
    "studentCode": "CT070218",
    "studentName": "Nguyễn Văn An",
    "studentClass": "CT7B"
  },
  "scores": [
    {
      "scoreText": "A",
      "scoreFirst": 8.5,
      "scoreSecond": 9.0,
      "scoreFinal": 8.0,
      "scoreOverall": 8.5,
      "subjectName": "Lập trình Java",
      "subjectCredit": 3,
      "isSelected": true
    }
  ],
  "lastUpdated": "2026-04-13T10:00:00"
}
```

### Response `200`
```json
{
  "batchId": 1,
  "studentCode": "CT070218",
  "studentName": "Nguyễn Văn An",
  "studentClass": "CT7B",
  "lastUpdated": "2026-04-13T10:00:00",
  "scoreItems": [
    {
      "itemId": 1,
      "scoreText": "A",
      "scoreFirst": 8.5,
      "scoreSecond": 9.0,
      "scoreFinal": 8.0,
      "scoreOverall": 8.5,
      "subjectName": "Lập trình Java",
      "subjectCredit": 3,
      "isSelected": true
    }
  ]
}
```

---

## 5. GET `/api/v1/score-batch/student/{studentCode}`

Lấy score batch theo mã sinh viên (plain text).

### Path Param
| Param | Type | Mô tả |
|-------|------|--------|
| `studentCode` | string | Mã sinh viên |

### Response `200` — cấu trúc giống mục 4 response
### Response `404` — không tìm thấy

---

## 6. GET `/api/v1/score-batch/student/encrypted`

Lấy score batch theo mã sinh viên **đã mã hóa**.

### Query Params
| Param | Type | Mô tả |
|-------|------|--------|
| `encryptedKey` | string | RSA-encrypted AES key (base64) |
| `encryptedData` | string | AES-encrypted student code (base64) |
| `iv` | string | IV hex string |

### Response `200` — cấu trúc giống mục 4 response
### Response `404` — không tìm thấy
### Response `400`
```json
{ "error": "Bad Request", "message": "..." }
```

---

## TypeScript Interfaces (Angular)

```typescript
// student-info.model.ts
export interface StudentInfo {
  display_name: string;
  student_code: string;
  gender: string;
  birthday: string;
  birth_place: string;   // masked
  id_card: string;       // masked
  bank_account: string;  // masked
  enroll_semester: string; // masked
  phone: string;         // masked
  email: string;         // masked
}

// schedule.model.ts
export interface StudentSchedule {
  course_name: string;
  course_code: string;
  study_days: string;
  lessons: string;
  study_location: string;
  teacher: string;
}

// login-response.model.ts
export interface LoginResponse {
  code: string;
  message: string;
  data: {
    student_info: StudentInfo;
    student_schedule: StudentSchedule[];
  };
}

// timeline.model.ts
export interface TimelineResponse {
  courseCode: string;
  courseName: string;
  studyLocation: string;
  teacher: string;
  studyDays: string;
  lessons: string;
}

export interface VirtualCalendarItem {
  course: string;
  courseName: string;
  baseTime: string;
  timelineResponse: TimelineResponse;
}

// virtual-calendar-response.model.ts
export interface VirtualCalendarResponse {
  code: string;
  message: string;
  data: {
    student_info: StudentInfo;
    virtual_calendar: VirtualCalendarItem[];
  };
}

// score.model.ts
export interface ScoreDTO {
  scoreText: string;
  scoreFirst: number;
  scoreSecond: number;
  scoreFinal: number;
  scoreOverall: number;
  subjectName: string; // masked
}

export interface StudentDTO {
  studentCode: string;
  studentName: string; // masked
  studentClass: string;
}

export interface ListScoreResponse {
  studentDTO: StudentDTO;
  scoreDTOS: ScoreDTO[];
}

// score-batch.model.ts
export interface ScoreItemDTO {
  scoreText: string;
  scoreFirst: number;
  scoreSecond: number;
  scoreFinal: number;
  scoreOverall: number;
  subjectName: string;
  subjectCredit: number;
  isSelected: boolean;
}

export interface ScoreBatchRequest {
  studentInfo: {
    studentCode: string;
    studentName: string;
    studentClass: string;
  };
  scores: ScoreItemDTO[];
  lastUpdated?: string; // ISO 8601
}

export interface ScoreBatch {
  batchId: number;
  studentCode: string;
  studentName: string;
  studentClass: string;
  lastUpdated: string;
  scoreItems: (ScoreItemDTO & { itemId: number })[];
}
```
