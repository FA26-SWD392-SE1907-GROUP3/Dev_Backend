# AIVES Backend — SWD392

Spring Boot / Java 17+, SQL Server, nhánh phát triển `Quoc_BE`.

Backend cho 3 nhóm chức năng:

- Tài liệu môn học, trích xuất nội dung, sinh câu hỏi bằng AI từ tài liệu;
  câu hỏi thủ công/import JSON, nhãn Bloom, rubric và duyệt câu hỏi.
- Kỳ thi, lịch từng sinh viên, công bố/kết thúc kỳ thi và chọn câu hỏi
  ngẫu nhiên ưu tiên tránh câu của thí sinh thi trước khi ngân hàng đủ câu.
- Phòng vấn đáp, lưu transcript/ghi âm, hỏi đào sâu qua Ollama,
  giới hạn thời gian và số câu đào sâu; dữ liệu trả lời được giữ khi AI lỗi.
- Đăng nhập bearer token, đăng ký Sinh viên, Google ID token và Admin đổi vai trò.

## Chạy local

1. Tạo schema bằng các script `database/01-create-aives.sql`,
   `02-functional-schema.sql`, `04-google-auth.sql` trong thư mục dự án.
   Database hiện có chỉ chạy migration còn thiếu. Script 03 là dữ liệu demo tùy chọn.
2. Mở PowerShell tại repository:

```powershell
cd SWD392_SE1907_AIVES
.\scripts\start-backend.ps1 -CreateAdmin
```

Script hỏi thông tin SQL Server và tài khoản Admin ứng dụng. Lần chạy tiếp
không cần `-CreateAdmin`. Swagger: http://localhost:8080/swagger-ui/index.html.
Không lưu mật khẩu SQL Server vào Git.

AI thật cần `AI_URL` / `AI_MODEL` và dịch vụ Ollama hoạt động.
Google cần `GOOGLE_CLIENT_ID`. TTS/STT được thực hiện bởi trình duyệt frontend;
backend nhận transcript và ghi âm, không có kết quả AI giả làm phương án dự phòng.
Chấm điểm AI và báo cáo điểm thuộc nhóm chức năng 4/6, chưa triển khai tại đây.

## Tài liệu và kiểm tra

- [API và cách chạy](SWD392_SE1907_AIVES/docs/full-api.md)
- [Google](SWD392_SE1907_AIVES/docs/google-auth.md)
- [Admin đổi vai trò](SWD392_SE1907_AIVES/docs/account-role.md)
- [Database](SWD392_SE1907_AIVES/database/README.md)

```powershell
cd SWD392_SE1907_AIVES
.\mvnw.cmd verify
```

Test dùng profile H2 riêng, không cần tài khoản/database SQL Server local.
Ngày 05/10/2026: 12 test pass, build executable JAR thành công.
