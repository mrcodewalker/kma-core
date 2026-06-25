# KMA Legend — Mobile API Integration Spec

> Base URL: `https://kma-legend.click/api`
>
> Tài liệu này dành cho mobile developer tích hợp API vào ứng dụng KMA Legend.

---

## Mục lục

1. [Cơ chế mã hóa (Bắt buộc đọc trước)](#1-cơ-chế-mã-hóa)
2. [Authentication](#2-authentication)
3. [Điểm thật](#3-điểm-thật)
4. [Bảng điểm ảo](#4-bảng-điểm-ảo)
5. [Xếp hạng](#5-xếp-hạng)
6. [Sinh viên & Môn học](#6-sinh-viên--môn-học)
7. [Error Responses](#7-error-responses)

---

## 1. Cơ chế mã hóa

**Tất cả POST request** đều phải mã hóa body trước khi gửi. GET request gửi bình thường.

### Thuật toán: RSA + AES-CBC hybrid

```
Client                              Server
  |                                   |
  |-- GET /v1/encryption/public-key ->|
  |<-- RSA Public Key (Base64) -------|
  |                                   |
  |  1. Tạo AES key ngẫu nhiên (256-bit)
  |  2. Mã hóa payload JSON bằng AES/CBC/PKCS5
  |  3. Mã hóa AES key bằng RSA Public Key
  |                                   |
  |-- POST /v1/... (encrypted body) ->|
  |  {                                |
  |    encryptedKey: "...",           |  <- AES key đã mã hóa bằng RSA, Base64
  |    encryptedData: "...",          |  <- JSON payload đã mã hóa bằng AES, Base64
  |    iv: "..."                      |  <- IV hex string
  |  }                                |
  |<-- Response JSON (plain) ---------|
```

### Cấu trúc encrypted request body

```json
{
  "encryptedKey": "<Base64(RSA_encrypt(hex(AES_key)))>",
  "encryptedData": "<Base64(AES_CBC_encrypt(JSON_payload))>",
  "iv": "<hex string của IV 16 bytes>"
}
```

### Ví dụ mã hóa (JavaScript / React Native)

```javascript
import CryptoJS from 'crypto-js';
import JSEncrypt from 'jsencrypt';

// Bước 1: Lấy public key (chỉ cần lấy 1 lần khi app khởi động)
const publicKeyBase64 = await fetch('https://kma-legend.click/api/v1/encryption/public-key')
  .then(r => r.text());

// Bước 2: Hàm mã hóa payload
function encryptPayload(payload, publicKeyBase64) {
  // Tạo AES key ngẫu nhiên
  const aesKey = CryptoJS.lib.WordArray.random(32); // 256-bit
  const iv = CryptoJS.lib.WordArray.random(16);     // 128-bit IV

  // Mã hóa JSON bằng AES/CBC
  const jsonStr = JSON.stringify(payload);
  const encrypted = CryptoJS.AES.encrypt(jsonStr, aesKey, {
    iv: iv,
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7
  });

  // Mã hóa AES key bằng RSA
  const rsaEncrypt = new JSEncrypt();
  const publicKeyPem = `-----BEGIN PUBLIC KEY-----\n${publicKeyBase64}\n-----END PUBLIC KEY-----`;
  rsaEncrypt.setPublicKey(publicKeyPem);
  const encryptedKey = rsaEncrypt.encrypt(CryptoJS.enc.Hex.stringify(aesKey));

  return {
    encryptedKey: encryptedKey,           // string
    encryptedData: encrypted.toString(),  // Base64 string
    iv: CryptoJS.enc.Hex.stringify(iv)    // hex string
  };
}

// Bước 3: Gửi request
async function postEncrypted(endpoint, payload) {
  const body = encryptPayload(payload, publicKeyBase64);
  const response = await fetch(`https://kma-legend.click/api${endpoint}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  return response.json();
}
```

> **Lưu ý quan trọng:**
> - Public key được tạo mới mỗi lần server restart → nên cache key theo session, không hardcode
> - Response từ server **không** mã hóa, trả về JSON bình thường
> - Chỉ POST request mới cần mã hóa, GET request gửi bình thường

---

## 2. Authentication

### 2.1 Lấy Public Key

```
GET /v1/encryption/public-key
```

**Response:** `200 OK`
```
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
```
*(Plain text, Base64 encoded RSA public key — không phải JSON)*

---

### 2.2 Đăng nhập

```
POST /v1/auth/login
Content-Type: application/json
```

**Payload gốc (trước khi mã hóa):**
```json
{
  "username": "CT070218",
  "password": "matkhau123"
}
```

**Body gửi lên (sau khi mã hóa):**
```json
{
  "encryptedKey": "abc123...",
  "encryptedData": "xyz789...",
  "iv": "a1b2c3d4..."
}
```

**Response: `200 OK`**
```json
{
  "code": "200",
  "message": "OK",
  "data": {
    "student_info": {
      "display_name": "Nguyễn Văn A",
      "student_code": "CT070218",
      "gender": "Nam",
      "birthday": "01/01/2002"
    },
    "student_schedule": [
      {
        "course_name": "Lập trình Java",
        "course_code": "IT3040",
        "study_days": "15/01/2026 17/01/2026 22/01/2026",
        "lessons": "1,2,3 1,2,3 1,2,3",
        "study_location": "A101",
        "teacher": "Nguyễn Thị B"
      }
    ]
  }
}
```

**Response: `401 Unauthorized`**
```json
{
  "code": "401",
  "message": "Wrong Password"
}
```

**Response: `400 Bad Request`**
```json
{
  "code": "400",
  "message": "Missing Item"
}
```

**Response: `500 Internal Server Error`**
```json
{
  "code": "500",
  "message": "Error: <chi tiết lỗi>"
}
```

> **Lưu ý:** `student_schedule` là lịch học thật từ hệ thống KMA (qldt.actvn.edu.vn). `study_days` là chuỗi các ngày học cách nhau bởi dấu cách, `lessons` là chuỗi tiết học tương ứng.

---

### 2.3 Lấy lịch học ảo (Virtual Calendar)

```
POST /v1/auth/virtual-calendar
Content-Type: application/json
```

**Payload gốc (trước khi mã hóa):**
```json
{
  "username": "CT070218",
  "password": "matkhau123"
}
```

**Response: `200 OK`**
```json
{
  "code": "200",
  "message": "OK",
  "virtual_calendar": [
    {
      "course": "2024-2025",
      "course_name": "Lập trình Java",
      "base_time": "Từ 19/01/2026 đến 08/02/2026: Thứ 2 tiết 10,11,12",
      "details": {
        "study_days": "19/01/2026 21/01/2026 26/01/2026",
        "teacher": "Nguyễn Thị B",
        "course_code": "IT3040",
        "course_name": "Lớp CT7B",
        "study_location": "A101",
        "lessons": "10,11,12 10,11,12 10,11,12"
      }
    }
  ]
}
```

> **Lưu ý:** `virtual_calendar` là danh sách các môn học đăng ký từ hệ thống KMA. Mỗi item có `details` chứa thông tin chi tiết lịch học. `study_days` và `lessons` là chuỗi cách nhau bởi dấu cách, tương ứng theo từng ngày.

---

## 3. Điểm thật

### 3.1 Lấy điểm + xếp hạng của sinh viên

Đây là API chính để lấy toàn bộ điểm thật và thông tin xếp hạng của sinh viên.

```
POST /v1/ranking/scores
Content-Type: application/json
```

**Payload gốc (trước khi mã hóa):**
```json
{
  "studentCode": "CT070218"
}
```

**Response: `200 OK`**
```json
{
  "listScoreDTO": {
    "studentDTO": {
      "studentId": 123,
      "studentCode": "CT070218",
      "studentName": "Nguyễn Văn A",
      "studentClass": "CT7B"
    },
    "scoreDTOS": [
      {
        "id": 1,
        "studentId": 123,
        "subjectId": 45,
        "scoreText": "A",
        "scoreFirst": 8.5,
        "scoreSecond": 9.0,
        "scoreFinal": 8.0,
        "scoreOverall": 8.5,
        "semester": "ki1-2023-2024",
        "subjectName": "Lập trình Java",
        "subjectCredit": 3
      },
      {
        "id": 2,
        "studentId": 123,
        "subjectId": 46,
        "scoreText": "B+",
        "scoreFirst": 7.0,
        "scoreSecond": 7.5,
        "scoreFinal": 7.0,
        "scoreOverall": 7.2,
        "semester": "ki1-2023-2024",
        "subjectName": "Cơ sở dữ liệu",
        "subjectCredit": 3
      }
    ]
  },
  "subjectDTOS": [
    {
      "subjectId": null,
      "subjectName": "Lập trình Java",
      "subjectCredits": null
    },
    {
      "subjectId": null,
      "subjectName": "Cơ sở dữ liệu",
      "subjectCredits": null
    }
  ]
}
```

> **Lưu ý:**
> - `subjectDTOS` là danh sách môn học trong học kỳ gần nhất (từ bảng `semester`), dùng để hiển thị tab "Học kỳ hiện tại"
> - `scoreDTOS` là toàn bộ điểm tích lũy của sinh viên
> - `semester` format: `"ki1-2023-2024"`, `"ki2-2023-2024"`, `"ki3-2023-2024"`
> - Grade mapping: `A+=4.0`, `A=3.8`, `B+=3.5`, `B=3.0`, `C+=2.4`, `C=2.0`, `D+=1.5`, `D=1.0`, `F=0.0`

---

### 3.2 Lấy thông tin sinh viên theo mã

```
GET /v1/students/{studentCode}
```

**Ví dụ:** `GET /v1/students/CT070218`

**Response: `200 OK`**
```json
{
  "studentId": 123,
  "studentCode": "CT070218",
  "studentName": "Nguyễn Văn A",
  "studentClass": "CT7B"
}
```

**Response: `404 Not Found`**
```json
{
  "timestamp": "2026-03-29T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Student not found"
}
```

---

## 4. Bảng điểm ảo

### 4.1 Lấy bảng điểm ảo

```
GET /v1/score-batch/student/{studentCode}
```

**Ví dụ:** `GET /v1/score-batch/student/CT070218`

**Response: `200 OK`**
```json
{
  "batchId": 1,
  "studentCode": "CT070218",
  "studentName": "Nguyễn Văn A",
  "studentClass": "CT7B",
  "lastUpdated": "2026-03-29T09:12:05.876",
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
    },
    {
      "itemId": 2,
      "scoreText": "D",
      "scoreFirst": 6.0,
      "scoreSecond": 6.0,
      "scoreFinal": 4.0,
      "scoreOverall": 4.6,
      "subjectName": "Giáo dục thể chất 1",
      "subjectCredit": 1,
      "isSelected": false
    }
  ]
}
```

**Response: `404 Not Found`** — Sinh viên chưa có bảng điểm ảo (body rỗng)

---

### 4.2 Tạo / Cập nhật bảng điểm ảo

```
POST /v1/score-batch/create-or-update
Content-Type: application/json
```

**Payload gốc (trước khi mã hóa):**
```json
{
  "studentInfo": {
    "studentId": null,
    "studentCode": "CT070218",
    "studentName": "Nguyễn Văn A",
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
    },
    {
      "scoreText": "D",
      "scoreFirst": 6.0,
      "scoreSecond": 6.0,
      "scoreFinal": 4.0,
      "scoreOverall": 4.6,
      "subjectName": "Giáo dục thể chất 1",
      "subjectCredit": 1,
      "isSelected": false
    }
  ],
  "lastUpdated": "2026-03-29T09:12:05.876Z"
}
```

**Response: `200 OK`** — Trả về ScoreBatch đã lưu (cùng cấu trúc với GET ở trên)
```json
{
  "batchId": 1,
  "studentCode": "CT070218",
  "studentName": "Nguyễn Văn A",
  "studentClass": "CT7B",
  "lastUpdated": "2026-03-29T09:12:05.876",
  "scoreItems": [...]
}
```

**Response: `400 Bad Request`**
```
"Error processing score batch: <chi tiết lỗi>"
```

> **Lưu ý:**
> - Nếu sinh viên đã có bảng điểm ảo → **xóa toàn bộ và tạo lại** (không merge)
> - `isSelected: false` dùng để đánh dấu môn không tính vào GPA ảo (ví dụ: Giáo dục thể chất)
> - `lastUpdated` nếu không truyền → server tự set thời gian hiện tại

---

## 5. Xếp hạng

### 5.1 Lấy xếp hạng toàn trường của sinh viên

```
GET /v1/ranking/school/{studentCode}
```

**Ví dụ:** `GET /v1/ranking/school/CT070218`

**Response: `200 OK`** — Mảng 4 phần tử: [sinh viên hiện tại, hạng 1, hạng 2, hạng 3]
```json
[
  {
    "studentName": "Nguyễn Văn A",
    "studentCode": "CT070218",
    "studentClass": "CT7B",
    "ranking": 47,
    "gpa": 3.45,
    "asiaGpa": 8.12
  },
  {
    "studentName": "Trần Thị B",
    "studentCode": "CT070101",
    "studentClass": "CT7A",
    "ranking": 1,
    "gpa": 3.98,
    "asiaGpa": 9.95
  },
  {
    "studentName": "Lê Văn C",
    "studentCode": "CT070205",
    "studentClass": "CT7B",
    "ranking": 2,
    "gpa": 3.95,
    "asiaGpa": 9.87
  },
  {
    "studentName": "Phạm Thị D",
    "studentCode": "AT170312",
    "studentClass": "AT17C",
    "ranking": 3,
    "gpa": 3.92,
    "asiaGpa": 9.80
  }
]
```

> **Lưu ý:** Index 0 luôn là sinh viên đang tra cứu. Index 1, 2, 3 là top 3 toàn trường.

---

### 5.2 Lấy top 3 toàn trường

```
GET /v1/ranking/top
```

**Response: `200 OK`** — Mảng 3 phần tử: [hạng 2, hạng 1, hạng 3] *(thứ tự podium)*
```json
[
  {
    "studentName": "Lê Văn C",
    "studentCode": "CT070205",
    "studentClass": "CT7B",
    "ranking": 2,
    "gpa": 3.95,
    "asiaGpa": 9.87
  },
  {
    "studentName": "Trần Thị B",
    "studentCode": "CT070101",
    "studentClass": "CT7A",
    "ranking": 1,
    "gpa": 3.98,
    "asiaGpa": 9.95
  },
  {
    "studentName": "Phạm Thị D",
    "studentCode": "AT170312",
    "studentClass": "AT17C",
    "ranking": 3,
    "gpa": 3.92,
    "asiaGpa": 9.80
  }
]
```

> **Lưu ý:** Server trả về theo thứ tự [2, 1, 3] để hiển thị podium. Index 0 = hạng 2 (trái), Index 1 = hạng 1 (giữa), Index 2 = hạng 3 (phải).

---

### 5.3 Lấy xếp hạng theo query param

```
GET /v1/ranking/school?student_code={studentCode}
```

**Ví dụ:** `GET /v1/ranking/school?student_code=CT070218`

**Response:** Giống hệt 5.1

---

## 6. Sinh viên & Môn học

### 6.1 Lấy danh sách tất cả sinh viên

```
GET /v1/students
```

**Response: `200 OK`**
```json
[
  {
    "studentId": 1,
    "studentCode": "CT070218",
    "studentName": "Nguyễn Văn A",
    "studentClass": "CT7B"
  },
  {
    "studentId": 2,
    "studentCode": "CT070101",
    "studentName": "Trần Thị B",
    "studentClass": "CT7A"
  }
]
```

---

### 6.2 Lấy danh sách tất cả môn học

```
GET /v1/subjects
```

**Response: `200 OK`**
```json
[
  {
    "subjectId": 1,
    "subjectName": "Lập trình Java",
    "subjectCredits": 3
  },
  {
    "subjectId": 2,
    "subjectName": "Cơ sở dữ liệu",
    "subjectCredits": 3
  },
  {
    "subjectId": 3,
    "subjectName": "Giáo dục thể chất 1",
    "subjectCredits": 1
  }
]
```

---

### 6.3 Lấy môn học theo ID

```
GET /v1/subjects/{id}
```

**Response: `200 OK`**
```json
{
  "subjectId": 1,
  "subjectName": "Lập trình Java",
  "subjectCredits": 3
}
```

---

## 7. Error Responses

### Cấu trúc lỗi chuẩn (từ GlobalExceptionHandler)

```json
{
  "timestamp": "2026-03-29T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Student not found"
}
```

### HTTP Status Codes

| Code | Ý nghĩa | Khi nào xảy ra |
|---|---|---|
| `200` | OK | Request thành công |
| `400` | Bad Request | Thiếu field, sai format, decrypt thất bại |
| `401` | Unauthorized | Sai mật khẩu KMA |
| `404` | Not Found | Không tìm thấy sinh viên / dữ liệu |
| `500` | Internal Server Error | Lỗi server, lỗi kết nối KMA portal |

---

## 8. Tóm tắt nhanh cho từng màn hình

### Màn hình Đăng nhập
1. `GET /v1/encryption/public-key` → lưu public key vào memory
2. `POST /v1/auth/login` (encrypted) → lấy `student_info` + `student_schedule`
3. Lưu `studentCode` vào local storage để dùng cho các API sau

### Màn hình Home
1. `GET /v1/students/{studentCode}` → thông tin sinh viên
2. `GET /v1/ranking/school/{studentCode}` → GPA + hạng (lấy index 0)
3. `POST /v1/ranking/scores` (encrypted) → điểm gần nhất (lọc theo `semester`)

### Tab Điểm thật
1. `POST /v1/ranking/scores` (encrypted) → toàn bộ `scoreDTOS`
2. Group by `semester` để hiển thị theo học kỳ
3. `subjectDTOS` trong response = môn học kỳ hiện tại

### Tab Điểm ảo
1. `GET /v1/score-batch/student/{studentCode}` → load bảng điểm ảo
2. Khi user chỉnh sửa → `POST /v1/score-batch/create-or-update` (encrypted)
3. Tính GPA ảo client-side từ `scoreItems` (lọc `isSelected: true`, bỏ môn GDTC)

### Tab Lịch học ảo
1. `POST /v1/auth/virtual-calendar` (encrypted) → `virtual_calendar` array
2. Parse `details.study_days` (split by space) + `details.lessons` (split by space) để render lịch

### Tab Xếp hạng
1. `GET /v1/ranking/top` → top 3 podium
2. `GET /v1/ranking/school/{studentCode}` → hạng của sinh viên + top 3

---

## 9. Công thức tính GPA (client-side)

### GPA hệ 4

```
GPA = Σ(điểm_hệ4 × tín_chỉ) / Σ(tín_chỉ)
```

### Bảng quy đổi điểm chữ → hệ 4

| scoreText | Hệ 4 | Hệ 10 (asiaGpa = gpa × 2.5) |
|---|---|---|
| A+ | 4.0 | 10.0 |
| A | 3.8 | 9.5 |
| B+ | 3.5 | 8.75 |
| B | 3.0 | 7.5 |
| C+ | 2.4 | 6.0 |
| C | 2.0 | 5.0 |
| D+ | 1.5 | 3.75 |
| D | 1.0 | 2.5 |
| F | 0.0 | 0.0 |

### Môn không tính GPA
- Môn có tên chứa `"Giáo dục thể chất"` → bỏ qua khi tính GPA
- Môn có `isSelected: false` trong bảng điểm ảo → bỏ qua

---

## 10. Lưu ý triển khai

- **Public key cache:** Lấy 1 lần khi app start, lưu vào memory. Nếu gặp lỗi decrypt (server restart) → lấy lại key mới.
- **studentCode format:** Luôn uppercase, ví dụ `CT070218` không phải `ct070218`. Server tự uppercase nhưng nên chuẩn hóa ở client.
- **semester format:** `"ki1-2023-2024"` — dùng để group điểm theo học kỳ.
- **CORS:** Server chỉ cho phép `https://kma-legend.click` và `http://localhost:4200`. Mobile app gọi trực tiếp không bị ảnh hưởng CORS.
- **Timeout:** Các API login (`/auth/login`, `/auth/virtual-calendar`) gọi đến server KMA bên ngoài, có thể chậm 3-10 giây. Nên set timeout 15s và hiển thị loading.
- **Không có JWT:** Hệ thống hiện tại không dùng JWT token. Mỗi request cần mã hóa lại, không có session token.
