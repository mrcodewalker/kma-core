# KMA Legend — Mobile UI Design Specification

> Tài liệu này dành cho designer thiết kế giao diện ứng dụng di động **KMA Legend** — nền tảng hỗ trợ học tập thông minh dành cho sinh viên Học viện Kỹ thuật Mật mã (KMA).

---

## 1. Tổng quan sản phẩm

**KMA Legend** là ứng dụng di động giúp sinh viên KMA:
- Tra cứu điểm thật từ hệ thống nhà trường (qua đăng nhập tài khoản KMA)
- Quản lý bảng điểm ảo (tự nhập, chỉnh sửa, mô phỏng kết quả)
- Xem lịch thi
- Xếp lịch học ảo (tự tạo thời khóa biểu cá nhân)
- Xem bảng xếp hạng GPA toàn trường / theo khối / theo lớp

**Đối tượng người dùng:** Sinh viên đại học, quen dùng smartphone, thích giao diện hiện đại, tối giản.

---

## 2. Design System

### 2.1 Màu sắc

| Token | Hex | Mô tả |
|---|---|---|
| `primary` | `#1A56DB` | Xanh dương chủ đạo — nút CTA, icon active |
| `primary-light` | `#EBF5FF` | Nền card nhẹ, badge |
| `secondary` | `#7E3AF2` | Tím — accent, gradient |
| `success` | `#0E9F6E` | Điểm đạt, trạng thái tốt |
| `warning` | `#FF8A4C` | Điểm cảnh báo, sắp thi |
| `danger` | `#F05252` | Điểm trượt, cảnh báo quan trọng |
| `neutral-900` | `#111928` | Text chính |
| `neutral-500` | `#6B7280` | Text phụ, placeholder |
| `neutral-100` | `#F3F4F6` | Nền trang, divider |
| `surface` | `#FFFFFF` | Nền card, bottom sheet |

**Gradient chủ đạo:** `linear-gradient(135deg, #1A56DB → #7E3AF2)`  
Dùng cho: Header, splash screen, avatar placeholder, card nổi bật.

### 2.2 Typography

| Style | Font | Size | Weight |
|---|---|---|---|
| Display | Inter / SF Pro | 28sp | 700 |
| Heading 1 | Inter / SF Pro | 22sp | 700 |
| Heading 2 | Inter / SF Pro | 18sp | 600 |
| Body | Inter / SF Pro | 15sp | 400 |
| Body Bold | Inter / SF Pro | 15sp | 600 |
| Caption | Inter / SF Pro | 12sp | 400 |
| Label | Inter / SF Pro | 11sp | 500 |

### 2.3 Spacing & Radius

- Base unit: `4dp`
- Padding màn hình: `16dp` (horizontal), `20dp` (top safe area)
- Card radius: `16dp`
- Button radius: `12dp`
- Chip/Badge radius: `999dp` (pill)
- Bottom sheet radius: `24dp` (top corners)

### 2.4 Elevation & Shadow

```
Card shadow: 0 2px 12px rgba(0,0,0,0.08)
Modal shadow: 0 8px 32px rgba(0,0,0,0.16)
Bottom nav shadow: 0 -1px 0 #E5E7EB
```

### 2.5 Icon Style

- Dùng bộ icon **Phosphor Icons** hoặc **Lucide** — stroke style, 24dp
- Icon active: filled variant + màu `primary`
- Icon inactive: stroke variant + màu `neutral-500`

---

## 3. Navigation Architecture

### Bottom Navigation Bar (5 tab)

```
┌──────────────────────────────────────────────┐
│  🏠 Home  │  📊 Điểm  │  📅 Lịch  │  🏆 BXH  │  👤 Tôi  │
└──────────────────────────────────────────────┘
```

| Tab | Icon | Label | Stack |
|---|---|---|---|
| Home | `house` | Trang chủ | HomeScreen |
| Điểm | `chart-bar` | Bảng điểm | ScoreStack |
| Lịch | `calendar` | Lịch học/thi | ScheduleStack |
| BXH | `trophy` | Xếp hạng | RankingStack |
| Tôi | `user` | Cá nhân | ProfileStack |

**Thiết kế Bottom Nav:**
- Nền trắng, shadow nhẹ phía trên
- Tab active: icon filled + label màu `primary` + dot indicator nhỏ bên dưới icon
- Tab inactive: icon stroke + label màu `neutral-500`
- Không dùng badge trừ khi có thông báo

---

## 4. Màn hình chi tiết

---

### 4.1 Splash Screen

**Layout:** Full screen, gradient `primary → secondary` (135deg)

**Elements:**
- Logo KMA Legend (icon + wordmark) — căn giữa màn hình
- Tagline: *"Học thông minh hơn mỗi ngày"* — caption, màu trắng 70%
- Loading indicator: 3 dot pulse animation, màu trắng — bottom 48dp

---

### 4.2 Màn hình Đăng nhập

**Layout:** Scroll view, padding 24dp

**Header:**
- Gradient background cao 220dp, bo góc dưới 32dp
- Logo + tên app căn giữa
- Subtitle: "Đăng nhập bằng tài khoản KMA"

**Form:**
- Input "Mã sinh viên" — icon `identification-card` prefix
- Input "Mật khẩu" — icon `lock` prefix, toggle show/hide
- Button "Đăng nhập" — full width, gradient, height 52dp, radius 12dp
- Text nhỏ: "Thông tin được mã hóa RSA+AES trước khi gửi" + icon `shield-check` màu `success`

**States:**
- Loading: button disabled + spinner
- Error: shake animation + error message dưới input
- Success: fade out → navigate to Home

> **API:** `POST /api/v1/auth/login` (encrypted)  
> **Lưu ý:** Hiển thị badge bảo mật để tăng trust

---

### 4.3 Home Screen

**Layout:** ScrollView, không có fixed header (dùng inline header)

#### Section 1 — Hero Card (Student Info)
```
┌─────────────────────────────────────────────┐
│  Gradient card, radius 20dp, padding 20dp   │
│                                             │
│  Xin chào, [Tên sinh viên] 👋               │
│  [Mã SV] · [Lớp]                           │
│                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │  GPA 4.0 │  │ Hạng #12 │  │ HK hiện  │  │
│  │  3.45    │  │  Lớp     │  │  tại     │  │
│  └──────────┘  └──────────┘  └──────────┘  │
└─────────────────────────────────────────────┘
```
- 3 stat chip: GPA thực, Hạng trong lớp, Học kỳ hiện tại
- Tap vào card → navigate đến tab Điểm

#### Section 2 — Quick Actions (2x2 grid)
```
┌─────────────────┐  ┌─────────────────┐
│  📄 Tra cứu     │  │  ✏️ Bảng điểm   │
│     điểm thật   │  │     ảo          │
└─────────────────┘  └─────────────────┘
┌─────────────────┐  ┌─────────────────┐
│  📅 Lịch thi    │  │  🗓️ Xếp lịch    │
│                 │  │     học ảo      │
└─────────────────┘  └─────────────────┘
```
- Mỗi card: icon lớn 32dp + label + màu nền pastel riêng biệt
- Tap → navigate đến màn hình tương ứng

#### Section 3 — Điểm gần đây
- Tiêu đề "Điểm học kỳ gần nhất" + link "Xem tất cả →"
- Horizontal scroll list, mỗi card hiển thị: tên môn, điểm tổng kết, grade letter (A/B/C/D/F), màu theo grade

#### Section 4 — Lịch thi sắp tới
- Tiêu đề "Lịch thi sắp tới" + link "Xem tất cả →"
- List 2-3 item gần nhất: tên môn, ngày thi, phòng thi, countdown chip

---

### 4.4 Stack: Bảng điểm (ScoreStack)

#### 4.4.1 Score Home Screen

**Header:**
- Tiêu đề "Bảng điểm"
- 2 tab pill: `Điểm thật` | `Điểm ảo`

**Tab "Điểm thật":**

*Semester Selector:*
- Horizontal scroll chip list: "HK1 2022-2023", "HK2 2022-2023", ... "Tất cả"
- Chip active: màu `primary`, filled

*Summary Card:*
```
┌──────────────────────────────────────────┐
│  GPA tích lũy: 3.45 / 4.0               │
│  GPA hệ 10: 8.12                        │
│  Tín chỉ tích lũy: 98 / 130             │
│  ████████████████░░░░  75%              │
└──────────────────────────────────────────┘
```
- Progress bar tín chỉ, màu gradient
- Tap → ScoreDetailScreen

*Score List:*
- Mỗi item: tên môn học, số tín chỉ, điểm thành phần (CC/GK/CK), điểm tổng kết, grade letter
- Grade letter badge: A=`success`, B=`primary`, C=`warning`, D=`warning` dim, F=`danger`
- Swipe left để xem chi tiết điểm thành phần

**Tab "Điểm ảo":**
- Hiển thị bảng điểm ảo đã lưu (từ ScoreBatch)
- Nút "Cập nhật từ KMA" — sync điểm thật vào bảng ảo
- Nút "Thêm môn học" — thêm môn mới vào bảng ảo
- Mỗi item có thể chỉnh sửa điểm trực tiếp (inline edit)
- Tính toán GPA ảo real-time khi chỉnh sửa

> **API điểm thật:** `POST /api/v1/ranking/scores` (encrypted)  
> **API điểm ảo:** `GET /api/v1/score-batch/student/{studentCode}`  
> **Cập nhật điểm ảo:** `POST /api/v1/score-batch/create-or-update` (encrypted)

#### 4.4.2 Score Detail Screen

**Header:** Tên môn học, số tín chỉ

**Content:**
- Breakdown điểm dạng visual:
```
  Chuyên cần (CC)    ████████░░  6.5 / 10
  Giữa kỳ (GK)      ██████████  8.0 / 10
  Cuối kỳ (CK)      ███████░░░  7.0 / 10
  ─────────────────────────────────────
  Tổng kết           ████████░░  7.4 / 10  →  B
```
- Công thức tính điểm hiển thị nhỏ bên dưới
- Học kỳ, năm học

#### 4.4.3 Virtual Score Editor Screen

**Header:** "Chỉnh sửa điểm ảo" + nút Save

**Content:**
- Tên môn học (editable)
- Số tín chỉ (stepper: 1-5)
- Điểm CC, GK, CK — number input với slider
- Preview điểm tổng kết real-time
- Preview grade letter real-time với màu tương ứng

**Bottom:**
- GPA ảo tổng kết cập nhật real-time
- Nút "Lưu thay đổi" — full width, gradient

---

### 4.5 Stack: Lịch học & Lịch thi (ScheduleStack)

#### 4.5.1 Schedule Home Screen

**Header:**
- Tiêu đề "Lịch"
- 2 tab pill: `Lịch thi` | `Lịch học ảo`

**Tab "Lịch thi":**

*Calendar View:*
- Month calendar ở trên, compact (6 hàng × 7 cột)
- Ngày có lịch thi: dot indicator màu `danger` bên dưới số ngày
- Ngày được chọn: circle filled màu `primary`

*Exam List (theo ngày được chọn):*
- Mỗi item:
  ```
  ┌────────────────────────────────────────┐
  │  🔴  Thứ 3, 15/01/2026                │
  │      Toán cao cấp A1                  │
  │      08:00 - 10:00  ·  Phòng A101     │
  │      Còn 3 ngày                       │
  └────────────────────────────────────────┘
  ```
- Countdown chip: đỏ nếu ≤ 3 ngày, cam nếu ≤ 7 ngày, xanh nếu > 7 ngày

**Tab "Lịch học ảo":**

*Week View:*
- Hiển thị lịch theo tuần, dạng grid (7 cột × các tiết học)
- Mỗi môn học: block màu riêng biệt, hiển thị tên môn + phòng học
- Swipe trái/phải để chuyển tuần

*Nút "Thêm môn học":*
- FAB (Floating Action Button) góc dưới phải
- Tap → AddSubjectScheduleSheet

#### 4.5.2 Add Subject Schedule Bottom Sheet

**Layout:** Bottom sheet, drag to dismiss

**Fields:**
- Tên môn học (text input hoặc chọn từ danh sách môn)
- Thứ trong tuần (multi-select chip: T2, T3, T4, T5, T6, T7, CN)
- Tiết bắt đầu / Tiết kết thúc (picker)
- Phòng học (text input)
- Màu sắc (color picker — 8 màu preset)

**Button:** "Thêm vào lịch" — full width

---

### 4.6 Stack: Bảng xếp hạng (RankingStack)

#### 4.6.1 Ranking Home Screen

**Header:**
- Tiêu đề "Bảng xếp hạng"
- Filter chips: `Toàn trường` | `Khối` | `Lớp tôi`

**My Ranking Card:**
```
┌──────────────────────────────────────────────┐
│  Hạng của bạn                                │
│                                              │
│  🏆  #47 / 1,200 sinh viên                  │
│  GPA: 3.45 · GPA hệ 10: 8.12               │
│  Top 4% toàn trường                         │
└──────────────────────────────────────────────┘
```
- Gradient card, nổi bật

**Top 3 Podium:**
```
        🥇
    🥈  [1]  🥉
    [2]      [3]
```
- Avatar placeholder với gradient
- Tên, mã SV, GPA

**Leaderboard List:**
- Mỗi item: số thứ hạng, avatar, tên SV, mã SV, lớp, GPA
- Item của người dùng hiện tại: highlight nền `primary-light`
- Infinite scroll hoặc "Xem thêm" button

> **API:** `GET /api/v1/ranking/school/{studentCode}`  
> **Top:** `GET /api/v1/ranking/top`

---

### 4.7 Stack: Cá nhân (ProfileStack)

#### 4.7.1 Profile Screen

**Header:**
- Avatar gradient (initials) + tên sinh viên + mã SV + lớp
- Nút "Chỉnh sửa" (icon pencil)

**Stats Row:**
```
┌──────────┐  ┌──────────┐  ┌──────────┐
│  GPA     │  │  Hạng    │  │  Tín chỉ │
│  3.45    │  │  #47     │  │  98/130  │
└──────────┘  └──────────┘  └──────────┘
```

**Menu List:**
- 📊 Bảng điểm của tôi
- 🏆 Xếp hạng của tôi
- 📅 Lịch học ảo
- 🔔 Thông báo
- ⚙️ Cài đặt
- 🔒 Bảo mật & Quyền riêng tư
- 🚪 Đăng xuất

---

## 5. Components Library

### 5.1 Score Card
```
┌──────────────────────────────────────────┐
│  Tên môn học                    3 TC     │
│  Giải tích 1                             │
│                                          │
│  CC: 7.0  GK: 8.0  CK: 6.5             │
│                                 7.2  B   │
└──────────────────────────────────────────┘
```
- Grade badge: pill, màu theo grade
- Tap → Score Detail

### 5.2 Exam Countdown Chip
- Format: "Còn X ngày" hoặc "Hôm nay" hoặc "Đã qua"
- Màu: đỏ / cam / xanh / xám

### 5.3 GPA Progress Ring
- Circular progress, 120dp
- Center: GPA value lớn + "/4.0" nhỏ
- Màu arc: gradient `primary → secondary`
- Background arc: `neutral-100`

### 5.4 Ranking Badge
- Hạng 1: 🥇 vàng
- Hạng 2: 🥈 bạc
- Hạng 3: 🥉 đồng
- Hạng 4+: số thứ hạng trong circle `neutral-100`

### 5.5 Empty State
- Illustration nhỏ (SVG)
- Tiêu đề + mô tả ngắn
- CTA button (nếu có action)

### 5.6 Loading Skeleton
- Dùng shimmer animation
- Áp dụng cho: score list, ranking list, calendar

### 5.7 Error State
- Icon cảnh báo + message
- Nút "Thử lại"

---

## 6. Micro-interactions & Animations

| Trigger | Animation | Duration |
|---|---|---|
| Tab switch | Slide + fade | 200ms |
| Card tap | Scale 0.97 → 1.0 | 100ms |
| Bottom sheet open | Slide up + backdrop fade | 300ms ease-out |
| Score update | Number count-up | 600ms |
| Grade badge appear | Scale + fade in | 200ms |
| Ranking change | Slide + color transition | 400ms |
| Pull to refresh | Custom spinner với logo | — |
| Login success | Checkmark animation → navigate | 800ms |

---

## 7. Data Mapping — API → UI

### Điểm thật
| UI Element | API Field | Source |
|---|---|---|
| Tên môn | `subjectName` | `ScoreDTO` |
| Số tín chỉ | `subjectCredit` | `ScoreDTO` |
| Điểm CC | `scoreFirst` | `ScoreDTO` |
| Điểm GK | `scoreSecond` | `ScoreDTO` |
| Điểm CK | `scoreFinal` | `ScoreDTO` |
| Điểm tổng kết | `scoreOverall` | `ScoreDTO` |
| Grade letter | `scoreText` | `ScoreDTO` |
| Học kỳ | `semester` | `ScoreDTO` |

### Bảng điểm ảo
| UI Element | API Field | Source |
|---|---|---|
| Tên môn | `subjectName` | `ScoreItem` |
| Số tín chỉ | `subjectCredit` | `ScoreItem` |
| Điểm tổng kết | `scoreOverall` | `ScoreItem` |
| Grade letter | `scoreText` | `ScoreItem` |
| Đã chọn | `isSelected` | `ScoreItem` |
| Cập nhật lần cuối | `lastUpdated` | `ScoreBatch` |

### Xếp hạng
| UI Element | API Field | Source |
|---|---|---|
| Hạng | `ranking` | `RankingDTO` |
| GPA hệ 4 | `gpa` | `RankingDTO` |
| GPA hệ 10 | `asiaGpa` | `RankingDTO` |
| Tên SV | `studentName` | `RankingDTO` |
| Mã SV | `studentCode` | `RankingDTO` |
| Lớp | `studentClass` | `RankingDTO` |

---

## 8. Màn hình trạng thái đặc biệt

### 8.1 Sinh viên chưa có điểm
- Empty state với illustration sách + bút
- Message: "Chưa có dữ liệu điểm. Hãy đăng nhập để đồng bộ điểm từ KMA."
- CTA: "Đồng bộ ngay"

### 8.2 Sinh viên có môn trượt (StudentFailed)
- Banner cảnh báo màu `danger` ở đầu trang điểm
- Icon cảnh báo + "Bạn có môn học chưa đạt. Vui lòng kiểm tra lại."
- Tap → highlight các môn F trong danh sách

### 8.3 Offline mode
- Banner nhỏ ở top: "Không có kết nối mạng — Đang hiển thị dữ liệu đã lưu"
- Màu nền banner: `neutral-900` với text trắng

---

## 9. Accessibility

- Minimum touch target: 44×44dp
- Color contrast ratio: ≥ 4.5:1 cho text thường, ≥ 3:1 cho text lớn
- Tất cả icon có `contentDescription` / `accessibilityLabel`
- Hỗ trợ Dynamic Type (iOS) / Font Scale (Android)
- Focus order hợp lý cho screen reader
- Không dùng màu sắc là cách duy nhất truyền tải thông tin (luôn kèm text/icon)

---

## 10. Platform Notes

### iOS
- Safe area insets: top (Dynamic Island / notch), bottom (home indicator)
- Navigation: native back gesture (swipe from left edge)
- Bottom sheet: native feel với rubber band effect
- Haptic feedback: light impact khi tap card, success notification khi lưu

### Android
- Material You color theming (optional)
- Back button: hardware back + gesture navigation
- Bottom sheet: Material BottomSheetDialog
- Ripple effect trên tất cả interactive elements

---

## 11. Screen Flow Diagram

```
Splash
  └── Login
        └── Home
              ├── [Tab] Bảng điểm
              │     ├── Điểm thật
              │     │     └── Score Detail
              │     └── Điểm ảo
              │           └── Virtual Score Editor
              ├── [Tab] Lịch
              │     ├── Lịch thi (Calendar View)
              │     └── Lịch học ảo (Week View)
              │           └── Add Subject Sheet
              ├── [Tab] Xếp hạng
              │     └── Ranking Detail
              └── [Tab] Cá nhân
                    └── Settings / Logout
```

---

## 12. Checklist cho Designer

- [ ] Thiết kế đủ 2 theme: Light Mode và Dark Mode
- [ ] Export assets @1x, @2x, @3x (iOS) và mdpi → xxxhdpi (Android)
- [ ] Prototype interactive cho flow: Login → Home → Xem điểm → Chỉnh điểm ảo
- [ ] Prototype interactive cho flow: Home → Lịch thi → Thêm lịch học ảo
- [ ] Prototype interactive cho flow: Home → Bảng xếp hạng
- [ ] Thiết kế skeleton loading cho tất cả list screen
- [ ] Thiết kế empty state cho tất cả list screen
- [ ] Thiết kế error state
- [ ] Kiểm tra contrast ratio với tool Figma A11y
- [ ] Handoff Figma với token đầy đủ (color, spacing, typography)

---

*Tài liệu được tạo dựa trên phân tích toàn bộ source code backend KMA Legend — Spring Boot 3.2.3, MySQL, API prefix `/api/v1`.*
