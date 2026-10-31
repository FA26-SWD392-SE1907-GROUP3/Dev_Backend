# AIVES Backend — Spring Boot + Neon PostgreSQL (Sprint 1)

Backend khởi đầu cho **AIVES — AI-powered Viva Exam System**, bám trực tiếp vào 22 bảng từ thiết kế database của bạn. Backend Java, không có frontend.

## Tech stack

- **Java 21** / **Spring Boot 3.5.16** / Maven
- **Spring MVC REST** + `NamedParameterJdbcTemplate` (JDBC) + PostgreSQL driver
- **JWT HS256**, Spring Security, BCrypt, validation
- **Neon PostgreSQL** (JDBC `sslmode=require`)
- Sơ đồ SQL có sẵn: `database/01_schema.sql` — không bật Hibernate `ddl-auto` để tránh thay đổi các FK phức hợp và CHECK constraints.

Lưu ý: Đây là **Sprint 1 / backend MVP**, chưa phải sản phẩm hoàn chỉnh.

## 1. Chuẩn bị Neon

1. Mở Neon Console, tạo project PostgreSQL 16+.
2. Vào **SQL Editor**, chạy **toàn bộ** `database/01_schema.sql` **một lần** trên database trống.
3. Chỉ để chạy demo local: chạy thêm `database/02_seed.sql` một lần để có tài khoản/câu hỏi/lịch mẫu. **Không chạy seed trên production**.
4. Vào **Connect** của Neon, ghi lại **host**, **database**, **user**, **password**. Với Spring JDBC, chuyển URL từ `postgresql://` sang dạng `jdbc:postgresql://...`, hoặc tự tạo JDBC URL theo ví dụ dưới đây.

Ví dụ:

```text
jdbc:postgresql://ep-abc123.ap-southeast-1.aws.neon.tech/neondb?sslmode=require
```

Khuyến nghị ban đầu dùng **direct connection** + Hikari max 5. Khi triển khai thật nhiều instance có thể dùng pooled host theo chỉ dẫn Neon, cân nhắc giới hạn kết nối tổng thể.

**Không chia sẻ password Neon, JWT secret hoặc đưa `.env` lên GitHub.**

## 2. Chạy trong IntelliJ IDEA

1. Mở IntelliJ → **Open** → chọn thư mục này chứa `pom.xml`.
2. Chọn **JDK 21** rồi đợi IntelliJ tải Maven dependencies.
3. Tạo **Run/Debug Configurations** cho class `com.aives.backend.AivesApplication`.
4. Dán các **Environment variables** thực tế:

```text
DB_URL=jdbc:postgresql://YOUR_NEON_HOST/neondb?sslmode=require
DB_USERNAME=YOUR_NEON_USER
DB_PASSWORD=YOUR_NEON_PASSWORD
JWT_SECRET=YOUR_OWN_RANDOM_SECRET_AT_LEAST_32_BYTES_LONG
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

5. Run `AivesApplication`. Backend chạy trên `http://localhost:8080`.
6. Mở `http://localhost:8080/actuator/health` để xem sức khỏe ứng dụng.

Nếu dùng terminal (Linux/macOS/Git Bash), có Maven trong PATH:

```bash
cp .env.example .env
# Sửa file .env bằng thông tin Neon & JWT secret của bạn
set -a; source .env; set +a
mvn test
mvn spring-boot:run
```

PowerShell: dùng IntelliJ **Run Configuration → Environment Variables** (dễ nhất), hoặc đặt riêng từng `$env:DB_URL=...`, `$env:DB_USERNAME=...`, v.v.

### Lưu ý về cấu trúc password demo

`02_seed.sql` đã tạo password bằng PostgreSQL `crypt(..., gen_salt('bf'))`, tương thích bcrypt. Backend cũng dùng BCrypt khi tạo user mới.

## 3. Đăng nhập thử

**Nếu đã chạy 02_seed.sql:**

- Admin: `admin@aives.local` / `Demo@123456`
- Lecturer: `lecturer@aives.local` / `Demo@123456`
- Student: `student1@aives.local` / `Demo@123456`

**Login:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"lecturer@aives.local","password":"Demo@123456"}'
```

Copy trường `accessToken`, thêm `Authorization: Bearer <TOKEN>` ở các request sau.

## 4. REST endpoints đã implement

| Phạm vi | HTTP | Đường dẫn | Quyền | Tác dụng |
|---|---|---|---|---|
| Auth | POST | `/api/v1/auth/login` | Public | Email/password → JWT |
| Admin | POST | `/api/v1/admin/users` | ADMIN | Tạo user hash bcrypt |
| Admin | POST | `/api/v1/admin/courses` | ADMIN | Tạo môn học |
| Admin | POST | `/api/v1/admin/courses/{courseId}/lecturers/{userId}` | ADMIN | Gán giảng viên |
| Foundation | POST | `/api/v1/courses/{courseId}/students/{studentId}` | Giảng viên môn/Admin | Ghi danh sinh viên |
| Foundation | GET | `/api/v1/courses` | JWT | Liệt kê môn mình quản lý/học |
| Module 1 | GET | `/api/v1/courses/{courseId}/topics` | Thành viên môn | Chủ đề |
| Module 1 | GET | `/api/v1/courses/{courseId}/rubrics` | Thành viên môn | Rubric (sinh viên chỉ thấy PUBLISHED) |
| Module 1 | GET | `/api/v1/courses/{courseId}/rubrics/{rubricId}/criteria` | Thành viên môn | Tiêu chí rubric |
| Module 1 | POST | `/api/v1/courses/{courseId}/rubrics` | Giảng viên môn/Admin | Tạo rubric + criteria |
| Module 1 | POST | `/api/v1/courses/{courseId}/rubrics/{rubricId}/publish` | Giảng viên môn/Admin | Xuất bản rubric |
| Module 1 | GET | `/api/v1/courses/{courseId}/questions` | Giảng viên môn/Admin | List ngân hàng câu hỏi |
| Module 1 | POST | `/api/v1/courses/{courseId}/questions` | Giảng viên môn/Admin | Tạo câu hỏi MANUAL/DRAFT |
| Module 1 | PUT | `/api/v1/courses/{courseId}/questions/{id}` | Giảng viên môn/Admin | Sửa câu hỏi manual DRAFT/REJECTED |
| Module 1 | POST | `/api/v1/courses/{courseId}/questions/{id}/submit` | Giảng viên môn/Admin | Chuyển PENDING_REVIEW |
| Module 1 | POST | `/api/v1/courses/{courseId}/questions/{id}/review` | Giảng viên môn/Admin | Duyệt/từ chối, audit review |
| Module 2 | GET | `/api/v1/courses/{courseId}/exams` | Thành viên môn | List kỳ thi |
| Module 2 | POST | `/api/v1/courses/{courseId}/exams` | Giảng viên môn/Admin | Tạo exam DRAFT |
| Module 2 | POST | `/api/v1/exams/{examId}/questions/{questionId}` | Giảng viên môn/Admin | Thêm câu hỏi APPROVED vào pool |
| Module 2 | POST | `/api/v1/exams/{examId}/schedule` | Giảng viên môn/Admin | Chốt thời gian, trạng thái SCHEDULED |
| Module 2 | POST | `/api/v1/exams/{examId}/candidates` | Giảng viên môn/Admin | Xếp lịch thí sinh |
| Module 2 | GET | `/api/v1/exams/me` | STUDENT | Lịch thi của mình |
| Module 3 | POST | `/api/v1/exams/{examId}/attempts/start` | STUDENT đúng giờ | RANDOM chọn câu, lưu snapshot |
| Module 3 | GET | `/api/v1/attempts/{attemptId}/current` | Chủ sở hữu STUDENT | Câu hỏi đang trả lời |
| Module 3 | GET | `/api/v1/attempts/{attemptId}/turns` | Chủ sở hữu STUDENT | Transcript |
| Module 3 | POST | `/api/v1/attempt-questions/{id}/answers` | Chủ sở hữu STUDENT | Gửi câu trả lời **dạng text** |

Dùng file `requests.http` để thử nhanh bằng IntelliJ HTTP Client (hoặc Postman). `@token` trong file phải được cập nhật sau login.

### Mẫu dữ liệu request

**Tạo rubric**:

```json
{
  "name": "Rubric REST API", "description": "Thang 10 điểm", "maxScore": 10,
  "criteria": [
    {"name": "Kiến thức", "maxPoints": 5},
    {"name": "Giải thích", "maxPoints": 3},
    {"name": "Ví dụ", "maxPoints": 2}
  ]
}
```

**Tạo exam**:

```json
{
  "title": "Thi vấn đáp Web", "selectionMode": "RANDOM",
  "mainQuestionCount": 2, "maxFollowupsPerQuestion": 0,
  "answerTimeLimitSeconds": 120, "slotDurationMinutes": 20,
  "instructions": "Trả lời ngắn gọn và rõ ràng"
}
```

**Schedule exam** (nhớ thay bằng thời điểm tương lai UTC):

```json
{"startsAt":"2026-11-01T08:00:00Z", "endsAt":"2026-11-01T11:00:00Z"}
```

**Gán candidate:**

```json
{"studentId":"10000000-0000-4000-8000-000000000003", "scheduledStart":"2026-11-01T08:00:00Z", "scheduledEnd":"2026-11-01T08:20:00Z"}
```

**Student answer**: `{"text":"HTTP GET dùng để lấy dữ liệu..."}`

## 5. Luồng sử dụng demo

1. Admin tạo lecturer, student và course (hoặc dùng seed).
2. Gán lecturer và enroll students.
3. Giảng viên tạo rubric → publish.
4. Giảng viên tạo câu hỏi MANUAL → submit → review APPROVE (câu hỏi cần rubric PUBLISHED).
5. Giảng viên tạo exam `RANDOM` → thêm đủ câu hỏi APPROVED vào pool → schedule → xếp slots.
6. Sinh viên đăng nhập → `GET /exams/me` → đúng slot gọi `/attempts/start` → `/attempts/{id}/current` → POST text answer → làm câu kế tiếp → `COMPLETED`.

**Quan trọng:** khi chưa tới giờ thi hoặc đã hết slot, API `/attempts/start` chủ động trả `409 Conflict`. Các exam seed có giờ dựa vào thời điểm seed; không thể bắt đầu ngay nếu chưa đến giờ.

## 6. Chưa thực hiện (những phần thật sự cần tiếp tục)

- **Module 1:** file upload S3/MinIO, parse PDF/DOCX, chunking, embeddings RAG, AI sinh câu hỏi, import Excel/CSV.
- **Module 2:** engine lựa chọn `ADAPTIVE` (hiện trả `501` khi bắt đầu), đổi trạng thái exam, timer enforcement, retry, chống cheating, phòng thi đồng thời, khóa xếp lịch trong database.
- **Module 3:** **chưa gọi LLM/STT/TTS**; backend đang lưu text turn chính và snapshot. Chưa có hỏi xoáy AI và truyền audio/realtime.
- Production hardening: refresh token, JWT revocation, login rate limiting, observability, test integration PostgreSQL, migrations versioned, audit/permission matrix nâng cao, background workers.

**Không hiểu nhầm `interview_turns.speaker='AI'` là AI đang chạy:** ở bản này đó chỉ là thông điệp câu hỏi lấy từ ngân hàng, chưa có provider tạo giọng nói hay sinh follow-up.

## 7. Bảo mật và database

- Tất cả API trừ `/auth/login` và `/actuator/health` cần JWT.
- Phân quyền cả theo role và theo course để ngăn giảng viên đọc/sửa môn không quản lý.
- Student không truy cập đáp án mẫu hoặc toàn ngân hàng câu hỏi.
- JWT secret >= 32 bytes, đặt bằng ENV; KHÔNG hardcode vào source.
- Database dùng PostgreSQL source-of-truth; backend không tạo/xóa bảng tự động.
- Dữ liệu audio chỉ lưu object key, không lưu file nhị phân trên Neon.
- Nên dùng database Neon branch riêng cho dev/test.

## 8. Kiểm thử

```bash
mvn test
mvn package
```

Trong môi trường tạo bộ code này **không có Maven và không thể tải dependency từ Maven Central**, nên **chưa thể thực thi compile/tests end-to-end**. Các file đã được rà soát về mặt cấu trúc nhưng cần bạn chạy `mvn test` với internet trong IntelliJ. Nếu gặp lỗi Maven hoặc Neon, gửi log để mình sửa trực tiếp.
