# AIVES Backend — Source đã điều chỉnh từ Dev_Backend.zip

Project chính nằm tại `SWD392_SE1907_AIVES/`. Giữ nguyên Java 21, Spring Boot 3.4.1, package `com.example.swd392_se1907_aives`, cấu trúc `controller/service/repository/domain` và JWT/Swagger từ repo nhóm. Không chứa thư mục `.git` từ bản upload nhằm tránh đóng gói lịch sử repository.

Đã thay dependency SQL Server bằng PostgreSQL, map lại **22 entity tương ứng 22 bảng** từ schema AIVES, chuyển ID sang UUID và các khóa ghép (`@EmbeddedId`), `TIMESTAMPTZ` sang `OffsetDateTime`, `JSONB` sang `JsonNode`, và sửa Auth/User API. Đã bổ sung API nền tảng cho course, rubric, question, exam và trả lời dạng text. Chưa tích hợp AI, STT, TTS, upload thực tế; chỉ lưu thông tin theo schema.

**Cảnh báo:** Mật khẩu Neon đã xuất hiện trong chat. Bạn nên reset password tại Neon Console ngay và chỉ dùng mật khẩu mới trong `.env` cục bộ. Không đưa `.env`, `DB_PASSWORD` hoặc `JWT_SECRET` vào Git.

## Chạy

1. Tạo/kiểm tra Neon PostgreSQL. Trong **database trống**, chạy `SWD392_SE1907_AIVES/database/01_schema.sql` bằng Neon SQL Editor. **Không chạy lại trên database đã có bảng**. Nếu muốn dữ liệu demo, chỉ chạy `02_seed.sql` trong DB dev/test **riêng biệt**, vì nó tạo tài khoản demo với mật khẩu công khai.
2. Sao chép `SWD392_SE1907_AIVES/.env.example` thành `.env`. Nhập `DB_URL` theo dạng **JDBC** `jdbc:postgresql://<neon-host>/neondb?sslmode=require&channelBinding=require`, kèm `DB_USERNAME`, `DB_PASSWORD` mới và `JWT_SECRET` ngẫu nhiên đủ dài (>=32 byte). File `.env` đã gitignore.
3. Mở thư mục `SWD392_SE1907_AIVES` trong IntelliJ (JDK 21), Maven Reload, chạy `Swd392Se1907AivesApplication`. Hoặc trên macOS/Linux: `./mvnw spring-boot:run`; trên Windows: `mvnw.cmd spring-boot:run`. Hệ thống đọc `.env` bằng `spring-dotenv` (đã có trong project gốc); trong môi trường production đặt biến môi trường trực tiếp.
4. Swagger: `http://localhost:8080/swagger-ui/index.html`; health: `http://localhost:8080/actuator/health`.

`spring.jpa.hibernate.ddl-auto=validate`: Hibernate kiểm tra entity với schema hiện có **chứ không tự tạo/sửa bảng**. Cần chạy SQL schema trước khi mở ứng dụng. URL Neon Pooled phù hợp các request thường; với script schema/migration, ưu tiên Neon Direct Connection.

## API căn bản

Các endpoint mặc định yêu cầu `Authorization: Bearer <JWT>` (trừ `/api/v1/auth/login`). Những thay đổi sau phản ánh đúng schema:

| Method | Endpoint | Vai trò | Mục đích |
|---|---|---|---|
| POST | `/api/v1/auth/login` | Public | Email + password → JWT |
| GET / POST | `/api/v1/users` | ADMIN | Liệt kê / tạo user |
| GET / PUT | `/api/v1/users/{id}` | ADMIN/chủ tài khoản | Xem/sửa profile |
| PUT | `/api/v1/users/{id}/password` | Chủ tài khoản | Đổi mật khẩu |
| PATCH | `/api/v1/users/{id}/activate` / `deactivate` | ADMIN | Đổi trạng thái |
| GET | `/api/v1/courses` | Đăng nhập | Liệt kê môn |
| POST | `/api/v1/courses` | ADMIN | Tạo môn |
| POST | `/api/v1/courses/{courseId}/lecturers` | ADMIN | Gán giảng viên |
| POST | `/api/v1/courses/{courseId}/students` | ADMIN / giảng viên đã gán | Ghi danh sinh viên |
| POST | `/api/v1/rubrics` | ADMIN / giảng viên đã gán | Tạo rubric |
| POST | `/api/v1/rubrics/{id}/criteria` | ADMIN / giảng viên đã gán | Thêm tiêu chí rubric |
| PATCH | `/api/v1/rubrics/{id}/publish` | ADMIN / giảng viên đã gán | Xuất bản khi đủ tổng điểm |
| POST | `/api/v1/questions` | ADMIN / giảng viên đã gán | Tạo câu hỏi MANUAL đang chờ duyệt |
| PATCH | `/api/v1/questions/{id}/review?approve=true` | ADMIN / giảng viên đã gán | Duyệt hoặc từ chối |
| GET | `/api/v1/courses/{courseId}/questions` | ADMIN / giảng viên đã gán | Danh sách câu hỏi |
| POST | `/api/v1/exams` | ADMIN / giảng viên đã gán | Tạo kỳ thi DRAFT |
| POST | `/api/v1/exams/{id}/questions` | ADMIN / giảng viên đã gán | Thêm câu hỏi APPROVED vào pool |
| POST | `/api/v1/exams/{id}/candidates` | ADMIN / giảng viên đã gán | Xếp lịch cho học viên đã ghi danh |
| PATCH | `/api/v1/exams/{id}/activate` | ADMIN / giảng viên đã gán | Mở kỳ thi khi đủ câu hỏi |
| POST | `/api/v1/attempts/start/{candidateId}` | STUDENT đúng lịch | Bắt đầu lượt thi, chọn câu ngẫu nhiên |
| GET | `/api/v1/attempts/{id}/questions` | STUDENT sở hữu | Xem câu hỏi đã gán |
| POST | `/api/v1/attempts/questions/{assignedQuestionId}/answer` | STUDENT sở hữu | Lưu câu trả lời bằng text |

### Đăng nhập demo (chỉ khi đã chạy seed trong DB dev)

```http
POST /api/v1/auth/login
Content-Type: application/json

{"email":"lecturer@aives.local","password":"Demo@123456"}
```

### Request ví dụ

```http
POST /api/v1/questions
Authorization: Bearer <JWT_LEC>
Content-Type: application/json

{"courseId":"20000000-0000-4000-8000-000000000001","rubricId":"50000000-0000-4000-8000-000000000001","content":"REST là gì?","referenceAnswer":"Representational State Transfer","bloomLevel":"UNDERSTAND","difficulty":"EASY"}
```

```http
POST /api/v1/exams/{examId}/candidates
Authorization: Bearer <JWT_LEC>
Content-Type: application/json

{"studentId":"10000000-0000-4000-8000-000000000003","scheduledStart":"2026-11-01T08:00:00+07:00","scheduledEnd":"2026-11-01T08:20:00+07:00"}
```

## Phạm vi và lưu ý

- Các bảng nhánh AI (`question_generation_jobs`, `material_chunks`, `ai_interview_requests`, …) **đã có entity** nhưng workflow xử lý AI/RAG/STT/TTS chưa được triển khai.
- Luồng thi text MVP chỉ hỗ trợ lần thi đầu, chọn RANDOM; follow-up AI/chấm điểm chưa được thực hiện.
- Cần test integration đầy đủ với Neon trước khi deploy; **chưa xác nhận build thành công** ở môi trường này vì Maven wrapper không thể tải distribution qua mạng.
- Lưu ý hạn chế MVP: race condition khi bắt đầu attempt, bảo vệ chống submit đồng thời, pagination và tối ưu truy vấn cần hoàn thiện cho production.
