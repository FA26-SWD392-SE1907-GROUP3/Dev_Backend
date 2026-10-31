# Tài liệu, rubric và câu hỏi AI

## Thiết lập

Thêm vào `.env` của backend (không đặt API key trong frontend):

```dotenv
OPENAI_API_KEY=YOUR_OPENAI_API_KEY
OPENAI_MODEL=gpt-4.1-mini
MATERIAL_STORAGE_DIR=D:/AivesData/materials
```

`MATERIAL_STORAGE_DIR` phải là thư mục lưu trữ bền vững có quyền ghi. Mặc định là `./storage/materials`, tính từ working directory khi chạy Java. Khi triển khai container, gắn persistent volume vào thư mục này. Neon chỉ lưu metadata và các đoạn văn bản; tệp gốc nằm trên ổ đĩa backend. Không thay đổi đường dẫn sau khi đã tải tài liệu lên nếu chưa di chuyển tệp tương ứng.

Nếu chưa có `OPENAI_API_KEY`, upload, tải tệp, xử lý văn bản và rubric vẫn dùng được. Nút sinh câu hỏi bị tắt và giao diện giải thích cách bật. Đọc [OpenAI Responses / Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs?api-mode=responses) nếu thay đổi tích hợp.

Khởi động lại backend sau khi cấu hình biến môi trường. Tất cả API bên dưới cần JWT LECTURER và chỉ cho phép giáo viên quản lý môn đã được phân công. ADMIN tạo tài khoản, môn học và phân công giáo viên; không duyệt câu hỏi hoặc tổ chức kỳ thi. Không tự cấp quyền cho giảng viên khi tạo rubric hoặc upload tài liệu. Xem quy trình đầy đủ tại `TEACHER_WORKFLOW.md`.

## Phân công và rubric

1. Admin mở **Môn học**, chọn môn, chọn **Phân công giảng viên**, nhập email giảng viên đang hoạt động và xác nhận. Danh sách giảng viên đã phân công hiển thị ngay bên dưới.
2. Giảng viên tải lại giao diện. Danh sách môn chỉ gồm các môn được phân công. Sinh viên chỉ thấy môn đã ghi danh ACTIVE.
3. Tạo rubric nháp; mở **Tiêu chí** để đọc/thêm/sửa/xóa tiêu chí được lưu trong database.
4. Tổng điểm phải bằng điểm tối đa mới được công bố. Rubric đã công bố không cho thay đổi tiêu chí. Thao tác ghi được khóa theo rubric để tránh vượt tổng điểm khi có yêu cầu đồng thời.

## Tài liệu và AI

Màn hình Sinh câu hỏi AI yêu cầu chọn tài liệu READY và **chọn rõ rubric PUBLISHED**; không tự tạo/công bố hoặc chọn thay rubric cho giáo viên. Khi thiếu, giao diện báo ngay tại form. Nếu chưa có rubric đã công bố, mở Tiêu chí Rubric để thêm tiêu chí đủ tổng điểm và công bố; có nút tải lại danh sách rubric khi quay lại. Nếu đã có rubric, chọn mục đó thay cho dòng Chọn rubric.

Trong lúc sinh, hiển thị trạng thái chờ. Lỗi provider hoặc lỗi validation hiển thị cạnh nút Sinh câu hỏi AI. Khi thành công ở trang Tài liệu, câu hỏi và đáp án tham khảo được hiển thị ngay, vẫn giữ trạng thái chờ duyệt; có thể xem lại bằng Lịch sử sinh câu hỏi → Xem câu hỏi (`GET /api/v1/generation-jobs/{jobId}/questions`, chỉ giáo viên phụ trách môn). Khi sinh từ Ngân hàng câu hỏi, giao diện chuyển về danh sách chờ giáo viên duyệt như trước. Endpoint xem kết quả cần khởi động lại backend để nạp controller mới.

1. Chọn môn, upload PDF/DOCX tối đa 20 MB. PDF tối đa 300 trang và phải có văn bản; không hỗ trợ OCR cho ảnh scan. DOCX được đọc nội dung văn bản trong `word/document.xml`, gồm cả văn bản trong bảng.
2. Chọn **Xử lý văn bản**. Nội dung được chia đoạn và lưu ở `material_chunks`. Xử lý lại tài liệu READY không tạo trùng đoạn.
3. Có thể tải bản gốc, xem văn bản đã trích xuất, chọn tối đa 5 tài liệu READY cùng một rubric PUBLISHED và tạo 1–20 câu hỏi. Tổng văn bản mỗi lần không vượt 60.000 ký tự.
4. OpenAI được gọi đồng bộ, timeout 90 giây. Lịch sử lưu COMPLETED hoặc FAILED và thông báo lỗi; lỗi provider không lưu câu hỏi dở dang. API key không được trả về client.
5. Câu hỏi có nguồn AI và trạng thái PENDING_REVIEW. Giảng viên mở **Ngân hàng câu hỏi → Chi tiết** để kiểm tra đáp án tham khảo và đoạn tài liệu nguồn, sau đó duyệt hoặc từ chối.

## Các API mới

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| GET | `/api/v1/courses/{courseId}/lecturers` | Giảng viên được phân công |
| GET/POST | `/api/v1/courses/{courseId}/materials` | Danh sách / upload multipart trường `file` |
| GET | `/api/v1/materials/{id}/download` | Tải bản gốc |
| POST | `/api/v1/materials/{id}/index` | Trích xuất và chia đoạn |
| GET | `/api/v1/materials/{id}/chunks` | Xem văn bản |
| GET | `/api/v1/rubrics/{id}/criteria` | Đọc tiêu chí đã lưu |
| PUT/DELETE | `/api/v1/rubrics/{id}/criteria/{criterionId}` | Sửa/xóa tiêu chí nháp |
| GET | `/api/v1/ai/status` | Trạng thái cấu hình, không trả API key |
| GET/POST | `/api/v1/courses/{courseId}/generation-jobs` | Lịch sử / tạo câu hỏi AI |
| GET | `/api/v1/questions/{id}` | Câu hỏi, đáp án và các đoạn nguồn |

POST generation-jobs nhận `materialIds`, `rubricId`, `desiredCount`, `bloomLevel`, `difficulty`, `instruction`. HTTP 201 trả job; client cần kiểm tra `status` vì job có thể FAILED. Lỗi validation, quyền và thiếu cấu hình trả HTTP 4xx/503 trước khi tạo job.

## Kiểm tra

Nếu Xử lý văn bản trả lỗi 500 với log `value too long for type character varying(255)`, kiểm tra mapping `material_chunks.content`: phải là TEXT, không phải VARCHAR(255). Entity đã đặt `columnDefinition="text"`; migration `database/05_material_chunk_text.sql` mở rộng cột văn bản và các trường câu hỏi liên quan mà không xóa nội dung. Khởi động lại backend sau khi cập nhật để Hibernate không đưa kiểu cột về VARCHAR(255). Tài liệu đã tải còn tệp gốc chỉ cần xử lý lại.

Lưu chunk và trạng thái READY trong một transaction. Nếu xử lý/lưu thất bại, rollback toàn bộ chunk và ghi FAILED trong transaction riêng; frontend tải lại trạng thái và hiển thị nút Thử xử lý lại. Lỗi lưu dữ liệu có thông báo riêng và log kèm materialId. Chia đoạn không cắt đôi cặp surrogate Unicode.

Backend: `mvnw.cmd test` (H2 và OpenAI giả lập, không dùng database Neon hay tiêu tốn API key).

Frontend: `npm test`, `npm run build`, `npm run test:e2e`. Nếu dùng Edge đã cài thay Chromium của Playwright, đặt `PLAYWRIGHT_CHANNEL=msedge` trước khi chạy E2E.
