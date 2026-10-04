# Dữ liệu kiểm thử

Sau migration `02-functional-schema.sql`, mở `03-seed-test-data.sql` bằng SSMS
và nhấn **Execute / F5**. Script không ghi đè dữ liệu cũ và chạy lại không thêm trùng.

Các tài khoản `demo_admin`, `demo_lecturer`, `demo_student01`, `demo_student02`,
`demo_student03` dùng chung mật khẩu **Aives@123**, chỉ dùng kiểm thử local.
Nếu username đã tồn tại, script giữ nguyên mật khẩu tài khoản đó.

Dữ liệu gồm 3 môn, 6 câu hỏi (4 đã duyệt, 1 chờ duyệt, 1 bị từ chối),
18 tiêu chí rubric, 2 kỳ thi và 3 lịch thi.

- Giảng viên: quản lý câu hỏi/rubric, xem kỳ thi và thử cấu hình kỳ thi nháp.
- Sinh viên: chọn kỳ thi đã công bố, trả lời 2 câu chính, 120 giây/câu.
- Admin: quản lý môn học và tài khoản.

Kỳ thi đã công bố có khung thi 24 giờ kể từ lần tạo, không có câu đào sâu,
nên không cần Ollama cho luồng này. Chạy lại không đặt lại tiến độ hay gia hạn
lịch thi. Khi hết hạn, tạo kỳ thi/lịch mới qua giao diện.
Nếu bật câu đào sâu cho kỳ thi nháp thì cần cấu hình AI theo `docs/full-api.md`.
Tài liệu và audio phải upload thật qua ứng dụng; transcript được tạo khi thi.

Đã kiểm tra script chạy hai lần trên SQL Server trong transaction rồi rollback:
5 tài khoản, 6 câu hỏi, 18 rubric, 2 kỳ thi; dữ liệu kiểm tra không được lưu.
