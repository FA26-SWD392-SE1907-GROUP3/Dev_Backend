# AIVES: backend và frontend cho nhóm chức năng 1, 2, 3

Backend ở nhánh `Quoc_BE`; frontend ở nhánh `FE_Quoc`. Chức năng 4 (AI chấm điểm), 5 (giám sát toàn diện) và 6 (báo cáo điểm) không thuộc phạm vi triển khai này. Transcript và ghi âm câu trả lời có thể được giảng viên xem lại.

## Database và khởi động

- Database mới: chạy `database/01-create-aives.sql`, sau đó `database/02-functional-schema.sql` bằng SSMS.
- Database AIVES hiện tại đã được chạy migration 02 trong phiên làm việc này. Không chạy lại script 01 trên database đã có bảng.
- Migration 02 chuyển enum sang chữ hoa, thêm ExtractedText, ApprovedBy/ApprovedAt, AnswerTimeLimitSeconds, ParentLogID và SequenceNumber. Không xóa dữ liệu.
- Không dùng profile `local` để thay migration. Cấu hình mặc định `validate` kiểm tra schema, không tự sửa bảng.

Trong thư mục `SWD392_SE1907_AIVES`, đặt biến môi trường và chạy:

```powershell
$env:DB_USERNAME = 'sa'
$env:DB_PASSWORD = 'your_sql_server_password'
$env:DB_URL = 'jdbc:sqlserver://localhost:1433;databaseName=AIVES;encrypt=true;trustServerCertificate=true'
$env:ADMIN_USERNAME = 'quoc'
$env:ADMIN_PASSWORD = 'choose_your_own_password_with_at_least_8_characters'
$env:ADMIN_EMAIL = 'your_email@example.com'
.\mvnw.cmd spring-boot:run
```

Hoặc dùng script nhập mật khẩu ẩn:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-backend.ps1 -CreateAdmin
```

Admin chỉ được tạo khi username chưa tồn tại; biến ADMIN_PASSWORD không đặt lại mật khẩu tài khoản đã có. Sau lần tạo đầu, các lần chạy tiếp không cần biến ADMIN_*. `sa` là tài khoản database, không phải tài khoản đăng nhập AIVES.

Trong thư mục `Dev_Frontend`:

```powershell
npm.cmd ci
npm.cmd run dev
```

Mở http://localhost:3000, đăng nhập bằng tài khoản admin AIVES vừa tạo. Vite proxy `/api` sang cổng 8080. Swagger: http://localhost:8080/swagger-ui/index.html. Login lấy token, rồi điền token vào nút Authorize trong Swagger.

## AI và giọng nói

Adapter hiện gọi API chat của Ollama và yêu cầu JSON có cấu trúc. Cấu hình bằng `AI_URL` và `AI_MODEL`. Không cài model hoặc gửi dữ liệu ra dịch vụ AI trong phiên triển khai này.

```powershell
$env:AI_URL = 'http://localhost:11434/api/chat'
$env:AI_MODEL = 'name_of_a_model_you_have_installed'
```

Ollama phải đang chạy và có model tương ứng. Tham khảo API chính thức: https://docs.ollama.com/api/chat. Nếu nhóm chọn nhà cung cấp khác, thay adapter AiService theo giao thức của nhà cung cấp đó; chỉ đổi URL không bảo đảm tương thích.

- Upload tài liệu được Apache Tika trích xuất text; TXT/PDF/DOCX/PPTX, tối đa 20 MB. PDF ảnh cần OCR trước, tài liệu không có text chuyển ERROR.
- RAG hiện chia đoạn text, truy xuất theo độ khớp từ khóa của chủ đề rồi gửi các đoạn liên quan đến model; chưa dùng vector database/embedding. Dữ liệu nguồn được đánh dấu là dữ liệu không tin cậy trong prompt.
- AI sinh câu hỏi kèm rubric tổng 10 điểm, luôn ở PENDING_APPROVAL. Giảng viên sửa rubric và duyệt trước khi xuất bản kỳ thi.
- Hỏi đào sâu đối chiếu transcript với rubric và lịch sử câu hỏi. Nếu AI lỗi, API trả 503/502, không dùng câu hỏi giả. Câu trả lời đã gửi ở request trước vẫn được giữ để thử lại.
- TTS/STT dùng API giọng nói thật của trình duyệt. Chrome/Edge được ưu tiên; cần quyền microphone, localhost hoặc HTTPS. Web Speech Recognition tùy trình duyệt có thể dùng dịch vụ nhận dạng bên ngoài, không bảo đảm offline. Khi không hỗ trợ, giao diện báo lỗi và cho phép nhập transcript, không sinh transcript mẫu.
- Ghi âm MediaRecorder được lưu riêng trên máy backend. Đường dẫn nội bộ không trả về frontend; tải file phải qua kiểm tra quyền.

## API chính

Các API nghiệp vụ cần header `Authorization: Bearer <token>`. Token có hạn một giờ và lưu trong bộ nhớ backend; restart backend sẽ yêu cầu đăng nhập lại. Frontend lưu token theo phiên tab (sessionStorage). Logout hoặc vô hiệu hóa tài khoản thu hồi token.

| Nhóm | Endpoint |
|---|---|
| Xác thực | POST `/api/auth/login`, GET `/api/auth/me`, POST `/api/auth/logout` |
| Vai trò | GET `/api/roles` |
| Tài khoản | GET/POST `/api/users`, GET/PUT/DELETE `/api/users/{id}` |
| Môn học | GET/POST `/api/subjects`, GET/PUT/DELETE `/api/subjects/{id}` |
| Tài liệu | GET/POST multipart `/api/documents`, GET/PUT/DELETE `/api/documents/{id}`, GET `/{id}/download`, POST `/{id}/reindex`, POST `/{id}/generate` |
| Câu hỏi | GET/POST `/api/questions`, GET/PUT/DELETE `/api/questions/{id}`, POST `/{id}/review`, POST `/api/questions/import` |
| Rubric | GET/POST `/api/questions/{questionId}/rubrics`, PUT `/api/questions/{questionId}/rubrics/{id}`, DELETE `/api/rubrics/{id}` |
| Kỳ thi | GET/POST `/api/exams`, GET/PUT/DELETE `/api/exams/{id}`, POST `/{id}/publish`, POST `/{id}/complete` |
| Lịch thi | GET/POST `/api/exams/{examId}/schedules`, PUT `/api/exams/{examId}/schedules/{id}`, GET/DELETE `/api/schedules/{id}`, GET `/api/my-schedules` |
| Phỏng vấn | GET `/api/interviews/{scheduleId}`, POST `/{scheduleId}/start`, POST `/{scheduleId}/answer`, POST `/{scheduleId}/next?afterLogId={logId}`, POST `/{scheduleId}/finish` |
| Ghi âm | POST multipart/GET `/api/logs/{logId}/audio` |

Admin quản lý môn học và tài khoản. Lecturer quản lý tài liệu/câu hỏi/kỳ thi của mình, admin có quyền quản lý các bản ghi này. Sinh viên chỉ xem và tham gia lượt thi được gán. Không trả PasswordHash qua API.

## Quy tắc và luồng demo

1. Admin tạo môn học, tài khoản Lecturer và Student.
2. Lecturer đăng nhập, upload tài liệu; nhập/import câu hỏi hoặc sinh qua AI.
3. Thêm rubric cho câu hỏi thủ công, chỉnh sửa câu AI nếu cần, sau đó duyệt. Chỉnh câu hỏi/rubric sẽ chuyển về chờ duyệt.
4. Tạo kỳ thi DRAFT và lịch cho sinh viên. Thời gian lịch phải nằm trong kỳ thi; không trùng sinh viên trong một kỳ thi hoặc chồng lịch của cùng sinh viên.
5. Xuất bản khi đủ câu đã duyệt có rubric và đã có sinh viên. Ngân hàng câu hỏi đang phục vụ kỳ thi PUBLISHED bị khóa sửa/xóa cho tới khi kỳ thi hoàn thành.
6. Sinh viên bắt đầu đúng khung giờ, nghe câu hỏi, ghi âm và kiểm tra transcript rồi gửi. Nhấn Tiếp tục để AI quyết định hỏi đào sâu hoặc chọn câu chính tiếp theo.
7. Câu chính không lặp trong cùng lượt thi; ưu tiên tránh câu đã hỏi thí sinh liền trước khi ngân hàng đủ câu. Không thể tránh trùng tuyệt đối nếu ngân hàng quá nhỏ.
8. Server kiểm tra hạn trả lời và giới hạn hỏi thêm mỗi câu chính. Request gửi lại không tạo thêm câu hỏi. Kết thúc khi hết câu hoặc hết thời gian lượt thi.
9. Lecturer xem transcript/ghi âm trong Lịch thi; kết thúc kỳ thi khi các lượt đã hoàn thành hoặc kỳ thi hết giờ.

Import JSON là một mảng từ 1 đến 100 QuestionInput; nếu một câu không hợp lệ, toàn bộ import rollback. Trạng thái chuẩn dùng chữ hoa: DRAFT/PUBLISHED/COMPLETED, PENDING_APPROVAL/APPROVED/REJECTED, NOT_STARTED/IN_PROGRESS/COMPLETED/MISSED.

## Kiểm tra

```powershell
.\mvnw.cmd test
.\mvnw.cmd package -DskipTests
```

Test backend dùng H2 độc lập, không xóa hay sửa database SQL Server của người dùng. Kiểm thử tích hợp đi qua HTTP với phân quyền, duyệt câu hỏi, lịch thi, hỏi thêm, retry, giữ transcript khi AI lỗi, upload/index TXT và import nguyên tử. AI được thay bằng test double trong integration test; kết quả này không xác minh chất lượng model thật.

Frontend: `npm.cmd run build`, `node --test tests/api.test.mjs`. Kiểm tra giọng nói và model thật cần chạy trên trình duyệt với thiết bị/dịch vụ của nhóm.

## Git

Chưa commit/push tự động. Review thay đổi trên Quoc_BE và FE_Quoc, chạy demo rồi commit theo phạm vi thực tế (migration/entity, auth, question/document, exam/interview, frontend). Không ghi lại ngày làm giả. Không đưa mật khẩu, uploads hoặc node_modules vào Git.
# Cập nhật đăng ký và Google

Backend mới cần migration `database/04-google-auth.sql` sau migration 02.
Xem [đăng ký và cấu hình Google](google-auth.md) cho API `/auth/register`,
Google Client ID và luồng xác thực. Frontend đã có màn hình đăng ký sinh viên
và đăng nhập Google khi backend được cấu hình Client ID.
