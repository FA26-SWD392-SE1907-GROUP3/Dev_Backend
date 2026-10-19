# Tính năng hiện tại

- [Tài liệu, rubric và sinh câu hỏi](TEACHING_FEATURES.md)
- [Phòng phỏng vấn AI: TTS, STT trực tiếp, hỏi xoáy và thời hạn](INTERVIEW_FEATURE.md)

Phần sau giữ thông tin áp dụng bản vá API ban đầu.

# AIVES API patch — reviewed against Dev_Backend.zip

## Cách áp dụng
Sao lưu project. Chép thư mục `src` trong bản vá vào `D:\Dev_Backend\SWD392_SE1907_AIVES`, cho phép ghi đè **2 file** AuthController.java và ExamAttemptRepository.java. Thêm mới StudentExamController.java. Rebuild Maven và chạy lại.

## API được bổ sung
- `GET /api/v1/student/exams` — danh sách lịch thi của sinh viên đang đăng nhập.
- `GET /api/v1/student/exams/{candidateId}/attempts` — lịch sử lần thi của chính sinh viên.

## API hiện có trong bản nguồn
- AuthController: POST login.
- UserController: danh sách, chi tiết, tạo, sửa, đổi mật khẩu, khóa/mở tài khoản.
- CourseController: danh sách/tạo khóa học, phân công giảng viên, ghi danh sinh viên.
- QuestionBankController: rubric, tiêu chí, tạo/duyệt câu hỏi.
- ExamController: tạo, thêm câu hỏi, gán thí sinh, kích hoạt, xem chi tiết kỳ thi.
- AttemptController: bắt đầu thi, xem câu hỏi được giao, nộp câu trả lời dạng text.

## Chưa được triển khai bởi bản vá này
- Quản lý chủ đề và import/chỉnh sửa/lưu trữ câu hỏi đầy đủ.
- Chống trùng câu giữa các sinh viên và lựa chọn câu chính thích ứng.
- Chấm điểm AI, nhận xét rubric, công bố kết quả, báo cáo thống kê.
- Quản lý đầy đủ vòng đời kỳ thi, lịch thi, CRUD chỉnh sửa/xóa.
- Chạy thử toàn bộ buổi thi bằng dữ liệu người dùng thật trên môi trường triển khai.

## Lưu ý đăng nhập
AuthController gốc dùng `@FieldDefaults(makeFinal=true)` nên không thể kết luận chắc chắn bị null injection; bản thay thế chỉ làm rõ `private final`. JWT_SECRET phải có ít nhất 32 byte cho HS256. Kiểm tra BCrypt và lỗi cụ thể từ backend nếu còn 500. Không chép giá trị bí mật thật vào Git.

Đã kiểm tra Maven test trên H2 và giao diện bằng Playwright. Xem phạm vi kiểm tra trong hai tài liệu tính năng bên dưới.
# Kiến trúc backend hiện tại

Backend đã được tổ chức theo **Modular Monolith** với ba module Identity, Teaching và Examination. Mỗi module có `api` và `internal`, giao tiếp qua interface/record; entity và repository thuộc module sở hữu. `ModuleArchitectureTests` kiểm tra ranh giới và chu trình phụ thuộc khi build.

Xem [MODULAR_MONOLITH.md](MODULAR_MONOLITH.md) để biết sơ đồ, hợp đồng, giao dịch và cách chạy sau khi đổi package. Lần đầu nạp cấu trúc mới cần **Stop backend → Rebuild Project → Run**, hoặc Stop rồi `mvnw.cmd clean package`, để tránh class cũ còn trong output.
