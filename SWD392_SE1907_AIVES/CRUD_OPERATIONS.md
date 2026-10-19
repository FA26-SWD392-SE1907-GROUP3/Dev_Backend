# Thao tác sửa, xóa và bảo toàn lịch sử

## Admin

| Dữ liệu | Sửa | Xóa / ngừng sử dụng |
| --- | --- | --- |
| Tài khoản | Họ tên, email, vai trò | Vô hiệu hóa, giữ lịch sử; có thể mở lại. Không tự khóa/xóa tài khoản đang đăng nhập. |
| Môn học | Mã môn, tên, mô tả | Ngừng sử dụng và khôi phục; chặn khi còn kỳ thi ACTIVE/SCHEDULED. |
| Phân công giảng viên | Đổi email sang giảng viên khác đang hoạt động | Gỡ phân công; chặn đổi/gỡ khi môn còn kỳ thi ACTIVE/SCHEDULED. |
| Ghi danh sinh viên | Ghi danh lại sinh viên đã ngừng học | Chuyển DROPPED; không xóa tài khoản, lịch hay lượt thi. Chặn khi sinh viên đang thi môn đó. |

Sinh viên được xác định bằng tài khoản; sửa thông tin sinh viên tại Người dùng & Vai trò. Thay sinh viên trong lớp bằng ngừng ghi danh người cũ và ghi danh người mới. Môn ngừng sử dụng không xuất hiện trong danh sách môn của giảng viên/sinh viên; Admin vẫn có thể xem và khôi phục.

## Giảng viên của môn được phân công

| Dữ liệu | Sửa | Xóa / ngừng sử dụng |
| --- | --- | --- |
| Tài liệu | Đổi tên, giữ định dạng PDF/DOCX | Xóa metadata, văn bản đã trích xuất và tệp sau khi giao dịch thành công; chặn nếu đã có lịch sử sinh câu hỏi liên quan. Nội dung mới được tải thành tài liệu mới để giữ nguồn trích dẫn. |
| Rubric | Tên, mô tả, điểm tối đa của DRAFT | Chuyển ARCHIVED nếu chưa gắn câu hỏi. Rubric đã công bố giữ cố định để bảo toàn căn cứ chấm điểm. |
| Tiêu chí rubric | Tên, mô tả, điểm, thứ tự trong DRAFT | Xóa tiêu chí trong DRAFT; tổng điểm không vượt điểm tối đa. |
| Câu hỏi | Nội dung, đáp án, Bloom, độ khó, rubric; trở lại chờ duyệt | Loại bỏ khỏi sử dụng; giữ lịch sử lượt thi. |
| Kỳ thi | Cấu hình DRAFT và thêm/gỡ câu trong đề | Xóa kỳ thi DRAFT chưa có bất kỳ lượt thi nào, gồm đề và lịch nháp; ca đang mở được kết thúc khi không còn lượt đang thi. |
| Lịch thí sinh | Khung giờ trước khi bắt đầu | Bỏ lịch trước khi có lượt thi; giữ ghi danh môn. |

Các ràng buộc được thực hiện ở backend; không chỉ ẩn nút tại giao diện. Thay đổi tài liệu, rubric, kỳ thi không cấp quyền quản lý môn/sinh viên cho giảng viên. Giám sát vẫn chỉ dành cho giảng viên được phân công.

## API bổ sung

| Method | Endpoint | Quyền |
| --- | --- | --- |
| DELETE | `/api/v1/users/{id}` | ADMIN |
| PUT / DELETE | `/api/v1/courses/{id}` | ADMIN |
| PATCH | `/api/v1/courses/{id}/restore` | ADMIN |
| PUT / DELETE | `/api/v1/courses/{courseId}/lecturers/{lecturerId}` | ADMIN |
| DELETE | `/api/v1/courses/{courseId}/students/{studentId}` | ADMIN |
| PUT / DELETE | `/api/v1/materials/{id}` | LECTURER của môn |
| PUT / DELETE | `/api/v1/rubrics/{id}` | LECTURER của môn |
| DELETE | `/api/v1/exams/{id}` | LECTURER của môn |

PUT tài liệu nhận `{ "name": "ten-moi.pdf" }`; PUT phân công nhận `{ "email": "giangvien@example.com" }`. Các thao tác bị chặn bởi lịch sử hoặc trạng thái trả 409 với lý do cụ thể. Xem TEACHER_WORKFLOW.md và EXAM_MONITORING.md cho các API còn lại.

## Lỗi giám sát sau cập nhật

Với kỳ thi FA26_SWD392_CP1, kiểm tra chỉ đọc trên Neon bằng mã hiện tại trả được dữ liệu giám sát. Tiến trình cũ đang giữ cổng 8080 chưa nạp controller mới: Spring ném NoResourceFoundException, còn bộ xử lý lỗi cũ trả nhầm 500. Bộ xử lý mới trả 404 cho route/tài nguyên không tồn tại và ghi log cho lỗi máy chủ ngoài dự kiến. Cần Stop/Run lại backend trong IDE, sau đó tải lại trang; tải lại frontend không cập nhật các controller trong JVM cũ.

Mapping mô tả môn và rubric dùng TEXT, đồng nhất với 01_schema.sql. Với ddl-auto=update, Hibernate đồng bộ khi khởi động; môi trường quản lý schema riêng cần bảo đảm hai cột description là TEXT. Không cần xóa dữ liệu hay chạy lại seed.
