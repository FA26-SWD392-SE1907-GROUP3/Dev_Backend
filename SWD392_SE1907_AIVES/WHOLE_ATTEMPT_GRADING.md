# Rubric chấm toàn bộ lượt thi

Luồng câu hỏi chỉ dùng ba mức `EASY` (Dễ), `MEDIUM` (Trung bình), `HARD` (Khó). Tạo thủ công, import CSV/JSON và sinh AI không yêu cầu rubric hoặc Bloom riêng. Câu hỏi vẫn phải được giảng viên duyệt trước khi đưa vào đề.

Giảng viên tạo rubric với các tiêu chí có tổng điểm bằng `maxScore`, công bố rubric, rồi chọn **Rubric chấm toàn bài** tại cấu hình kỳ thi. Backend chỉ cho mở kỳ thi khi rubric đã công bố và thuộc cùng môn học.

Khi sinh viên bắt đầu, backend lưu một bản chụp rubric trên `exam_attempts.rubric_snapshot`. Nội dung câu hỏi và đáp án tham khảo vẫn được chụp trên `attempt_questions`; các câu hỏi không có rubric riêng. Thay đổi dữ liệu hiện tại không làm thay đổi căn cứ chấm của một lượt thi đã bắt đầu.

Khi toàn bộ câu chính/hỏi xoáy đã hoàn tất hoặc hết giờ, lượt thi chuyển `COMPLETED`, kết quả chấm chuyển `PENDING`. Worker riêng gọi AI bằng toàn bộ hội thoại, trạng thái từng câu và đáp án tham khảo. Mỗi tiêu chí được chấm một lần trên toàn bài; backend kiểm tra số tiêu chí, mã thứ tự, khoảng điểm và tự cộng tổng điểm. Điểm vượt thang, tiêu chí thiếu/trùng hoặc kết quả không hợp lệ không được lưu như một điểm thành công. Cuộc gọi AI nằm ngoài transaction và dùng scheduler riêng để không chặn đồng hồ phòng thi.

API:

- `GET /api/v1/attempts/{attemptId}/grading`: trạng thái, tổng điểm và nhận xét theo tiêu chí.
- `POST /api/v1/attempts/{attemptId}/grading/retry`: thử lại khi chấm lỗi hoặc tác vụ đã treo quá ba phút. Không chấm lại một kết quả đã hoàn tất.

Sinh viên chỉ đọc/thử lại bài của mình. Giảng viên cần được phân công môn; Admin có quyền theo cơ chế truy cập hiện có. Trước khi lượt thi hoàn tất, API không trả điểm. Giao diện hiển thị kết quả trong lịch sử lượt thi, cuối phòng thi và màn hình hội thoại của giảng viên.

Cấu hình dùng API key hiện có: `app.ai.api-key`, endpoint gốc `app.interview.openai-base-url`, mô hình `app.ai.grading-model` (mặc định `gpt-4.1-mini`). `app.grading.enabled=false` tắt worker chấm nền; kiểm thử dùng tùy chọn này và AI giả lập.

## Nâng cấp dữ liệu

Khi khởi động với PostgreSQL, ứng dụng chạy migration idempotent từ `src/main/resources/db/migration/06_whole_attempt_rubric.sql` sau Hibernate. Migration thêm cột mới, bỏ ràng buộc câu được duyệt phải có rubric và cho phép Bloom cũ để trống. Entity `Question` không còn thuộc tính rubric/Bloom; các cột SQL cũ được giữ để bảo toàn dữ liệu lịch sử. Migration không xóa câu hỏi, rubric, hội thoại hoặc snapshot lịch sử. Có thể áp dụng thủ công bản tương ứng trong `database/06_whole_attempt_rubric.sql`; dùng `app.database.apply-compatibility-migrations=false` nếu môi trường quản lý migration riêng.

Kỳ thi cũ chưa có rubric cần được cấu hình trước khi mở/bắt đầu lượt mới. Lượt thi cũ không có bản chụp rubric toàn bài hiển thị chưa có kết quả; hệ thống không tự suy đoán rubric hoặc chấm lại lịch sử.

Trong mỗi module, controller inject interface `...Service`; các lớp `...ServiceImpl implements ...Service` đặt trong `internal/service/impl`. DTO nằm trong `internal/dto`, transaction và repository thuộc lớp triển khai service.

## Kiểm tra bản thay đổi

- 100 kiểm thử backend thành công, gồm kiến trúc, phân quyền, snapshot toàn lượt thi, chấm/thử lại và migration chạy lặp.
- 21 kiểm thử API/turn detection và 49 ca Playwright đã được kiểm tra thành công; ca đồng bộ giám sát được kiểm tra lại bằng đồng hồ giả lập để tránh phụ thuộc thời gian chờ thực.
- Frontend build thành công. Backend JAR: `target/whole-attempt-validation/SWD392_SE1907_AIVES-0.0.1-SNAPSHOT.jar`.
- Kiểm thử dùng H2 và AI/API giả lập; không gọi AI trả phí hoặc áp dụng migration lên Neon trong quá trình kiểm thử.
