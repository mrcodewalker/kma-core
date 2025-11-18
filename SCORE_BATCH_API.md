# Score Batch API Documentation

## Tổng quan
API này cho phép tạo hoặc cập nhật điểm của sinh viên theo batch (hàng loạt). API được thiết kế để xử lý dữ liệu điểm theo cấu trúc JSON như yêu cầu.

**⚠️ Lưu ý quan trọng:** Tất cả request POST đều được mã hóa và cần được giải mã trước khi xử lý. API sẽ tự động decrypt dữ liệu trước khi xử lý.

## Cấu trúc dữ liệu

### Request Body
```json
{
    "studentInfo": {
        "studentId": null,
        "studentCode": "CT070218",
        "studentName": "Huỳnh Ngọc Hải",
        "studentClass": "CT7B"
    },
    "scores": [
        {
            "scoreText": "D",
            "scoreFirst": 6,
            "scoreSecond": 6,
            "scoreFinal": 4,
            "scoreOverall": 4.6,
            "subjectName": "Giáo dục thể chất 1",
            "subjectCredit": 1,
            "isSelected": false
        }
    ],
    "lastUpdated": "2025-09-16T09:12:05.876Z"
}
```

## Encryption/Decryption

### Cách hoạt động:
1. **Client** gửi request với dữ liệu đã được mã hóa (RSA + AES)
2. **EncryptionInterceptor** tự động decrypt request trước khi gửi đến controller
3. **Controller** nhận dữ liệu đã được decrypt thông qua `DecryptedRequestWrapper`
4. **Service** xử lý dữ liệu bình thường

### Cấu trúc request được mã hóa:
```json
{
    "encryptedKey": "base64_encoded_rsa_encrypted_aes_key",
    "encryptedData": "base64_encoded_aes_encrypted_json_data", 
    "iv": "hex_encoded_initialization_vector"
}
```

## API Endpoints

### 1. Tạo hoặc cập nhật điểm batch
**POST** `/api/v1/score-batch/create-or-update`

Tạo mới hoặc cập nhật điểm batch cho một sinh viên. Nếu sinh viên đã có điểm batch, hệ thống sẽ xóa tất cả điểm cũ và thêm điểm mới.

**⚠️ Request phải được mã hóa theo format encryption của hệ thống**

**Dữ liệu gốc (trước khi mã hóa):**
- `studentInfo`: Thông tin sinh viên
  - `studentId`: ID sinh viên (có thể null)
  - `studentCode`: Mã sinh viên (bắt buộc)
  - `studentName`: Tên sinh viên (bắt buộc)
  - `studentClass`: Lớp sinh viên
- `scores`: Danh sách điểm
  - `scoreText`: Điểm chữ (A, B, C, D, F, etc.)
  - `scoreFirst`: Điểm thành phần 1
  - `scoreSecond`: Điểm thành phần 2
  - `scoreFinal`: Điểm thi
  - `scoreOverall`: Điểm tổng kết
  - `subjectName`: Tên môn học (bắt buộc)
  - `subjectCredit`: Số tín chỉ (bắt buộc)
  - `isSelected`: Đã chọn hay chưa
- `lastUpdated`: Thời gian cập nhật cuối

**Response:**
- `200 OK`: Trả về thông tin ScoreBatch đã được tạo/cập nhật
- `400 Bad Request`: Dữ liệu đầu vào không hợp lệ
- `500 Internal Server Error`: Lỗi server

### 2. Lấy điểm batch theo mã sinh viên
**GET** `/api/v1/score-batch/student/{studentCode}`

Lấy thông tin điểm batch của một sinh viên theo mã sinh viên.

**Path Parameters:**
- `studentCode`: Mã sinh viên

**Response:**
- `200 OK`: Trả về thông tin ScoreBatch
- `404 Not Found`: Không tìm thấy điểm batch
- `400 Bad Request`: Lỗi xử lý

## Cơ sở dữ liệu

### Bảng `score_batches`
- `batch_id`: ID batch (Primary Key)
- `student_code`: Mã sinh viên
- `student_name`: Tên sinh viên
- `student_class`: Lớp sinh viên
- `last_updated`: Thời gian cập nhật cuối

### Bảng `score_items`
- `item_id`: ID item (Primary Key)
- `batch_id`: ID batch (Foreign Key)
- `score_text`: Điểm chữ
- `score_first`: Điểm thành phần 1
- `score_second`: Điểm thành phần 2
- `score_final`: Điểm thi
- `score_overall`: Điểm tổng kết
- `subject_name`: Tên môn học
- `subject_credit`: Số tín chỉ
- `is_selected`: Đã chọn hay chưa

## Lưu ý
- **Encryption**: Tất cả POST request đều phải được mã hóa theo hệ thống RSA + AES
- **Auto-decryption**: API tự động decrypt dữ liệu trước khi xử lý
- **Transaction**: API sử dụng transaction để đảm bảo tính toàn vẹn dữ liệu
- **Update behavior**: Khi cập nhật, tất cả điểm cũ sẽ bị xóa và thay thế bằng điểm mới
- **Auto timestamp**: Thời gian `lastUpdated` sẽ được tự động set nếu không được cung cấp
- **CORS**: API hỗ trợ CORS cho domain `https://kma-legend.click` và `http://localhost:4200`

## Ví dụ sử dụng

### 1. Lấy public key để mã hóa
```bash
GET /api/v1/encryption/public-key
```

### 2. Mã hóa dữ liệu và gửi request
```javascript
// 1. Lấy public key
const publicKey = await fetch('/api/v1/encryption/public-key').then(r => r.text());

// 2. Mã hóa dữ liệu (sử dụng thư viện crypto-js hoặc tương tự)
const encryptedData = encryptData(scoreBatchData, publicKey);

// 3. Gửi request đã mã hóa
const response = await fetch('/api/v1/score-batch/create-or-update', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(encryptedData)
});
```

