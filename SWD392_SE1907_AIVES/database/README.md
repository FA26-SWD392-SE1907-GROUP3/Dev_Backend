# Database AIVES - SQL Server

Database mới: chạy `01-create-aives.sql`, `02-functional-schema.sql` rồi `04-google-auth.sql` trong SSMS.
Database hiện có: chạy các migration còn thiếu (02, 04). Script 01 sẽ dừng nếu đã có bảng ứng dụng.

Dữ liệu mẫu: [hướng dẫn seed](seed-guide.md). Đăng ký và đăng nhập Google:
[hướng dẫn cấu hình](../docs/google-auth.md). Migration 04 đã được áp dụng local,
thêm GoogleSubject và unique index có điều kiện, giữ nguyên tài khoản cũ.

Migration 02 đã được chạy thành công trên database AIVES trong phiên triển khai. Hibernate startup đã validate schema và kết nối SQL Server thành công; các API đọc đã được kiểm tra bằng tài khoản tạm, sau đó tài khoản kiểm tra được xóa.

Các bảng dùng NVARCHAR cho dữ liệu Unicode và VARCHAR cho enum được ánh xạ JDBC rõ ràng. Migration thêm text trích xuất, audit duyệt, giới hạn thời gian và quan hệ/thứ tự câu hỏi. Không dùng `ddl-auto=update` để thay migration trên database chia sẻ.

Xem [hướng dẫn đầy đủ](../docs/full-api.md) để chạy ứng dụng và tạo tài khoản admin AIVES. Tài khoản database `sa` và tài khoản đăng nhập ứng dụng là hai loại tài khoản khác nhau.
