# Giám sát ca thi và quản lý môn học

## Phân quyền

- ADMIN: Môn học & sinh viên; tạo môn, phân công giáo viên, ghi danh/kích hoạt lại sinh viên, lọc môn theo giảng viên. Không có giám sát và không gọi được API giám sát.
- LECTURER: Giám sát ca thi của môn được phân công; không có menu quản lý môn/sinh viên và không gọi được API ghi danh. Vẫn đọc danh sách đã ghi danh để phân lịch thi.
- STUDENT: Không gọi được API giám sát hay đọc hội thoại/bản ghi của người khác.

## Sử dụng

Giảng viên chọn Giám sát ca thi rồi chọn kỳ thi. Trang cập nhật mỗi 5 giây và có nút Cập nhật ngay. Hiển thị thí sinh chưa bắt đầu, đang thi, đã hoàn thành; tìm theo họ tên/email, lọc trạng thái hoặc Cần chú ý. Tiến độ gồm số câu chính đã kết thúc, số câu hết giờ, số hỏi xoáy đã dùng, câu hiện tại, deadline và thời điểm hoạt động cuối. Không suy đoán mất kết nối từ thời điểm hoạt động cuối.

Mỗi thí sinh có lịch sử từng lượt thi. Xem hội thoại để đối chiếu câu hỏi, câu trả lời, thời điểm và nghe bản ghi đã lưu; tải transcript TXT UTF-8. Bản ghi chưa nộp và phụ đề tạm ở trình duyệt sinh viên chưa xuất hiện tại giám sát. Đây là giám sát tiến độ/hội thoại, chưa có camera, ghi hình liên tục hoặc chống gian lận tự động. Không thực hiện chấm điểm trong màn hình này.

Nếu polling lỗi, giữ dữ liệu của lần thành công trước và hiển thị cảnh báo. Endpoint kiểm tra vai trò và môn học tại backend, kiểm tra lượt thi thuộc kỳ thi và bản ghi thuộc lượt thi; không trả đường dẫn tệp hoặc đáp án/rubric snapshot. Audio gửi Cache-Control no-store.

## API

| Method | Endpoint | Quyền |
| --- | --- | --- |
| GET | /api/v1/exams/{examId}/monitor | LECTURER của môn |
| GET | /api/v1/exams/{examId}/attempts/{attemptId}/transcript | LECTURER của môn |
| GET | /api/v1/exams/{examId}/attempts/{attemptId}/audio/{turnId} | LECTURER của môn |
| GET | /api/v1/courses?lecturerId={id} | ADMIN |
| POST | /api/v1/courses/{courseId}/students | ADMIN |

GET /courses và GET /courses/{id}/students vẫn phục vụ danh sách môn/sinh viên theo quyền cho các chức năng học thuật. POST phân lịch không còn tự ghi danh, kể cả khi request cũ gửi enrollIfNeeded=true. Dữ liệu ghi danh/lịch sử hiện có được giữ nguyên. Khởi động lại backend và tải lại frontend để nạp API, menu và phân quyền mới.

Nếu bản cũ báo `Internal server error` ngay khi chọn kỳ thi, xem phần chẩn đoán trong [CRUD_OPERATIONS.md](CRUD_OPERATIONS.md). Trường hợp đã kiểm tra là JVM cũ chưa nạp controller giám sát, khiến route không tồn tại bị trả nhầm 500; cần Stop/Run backend. Phiên bản mới trả đúng 404, hiển thị hướng dẫn tải bản mới và ghi log các lỗi ngoài dự kiến.
