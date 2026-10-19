# Nghiệp vụ giáo viên — nhóm chức năng 1 và 2

## Vai trò

| Vai trò | Công việc |
| --- | --- |
| ADMIN | Tạo tài khoản, môn học, phân công giáo viên và ghi danh sinh viên. Không giám sát hoặc quyết định học thuật. |
| LECTURER | Quản lý tài liệu, rubric, câu hỏi, lịch thi và giám sát kỳ thi của môn được phân công. Chỉ đọc danh sách sinh viên để phân lịch. |
| STUDENT | Xem lịch của mình, vào phòng thi đúng lịch và xem lại hội thoại của lượt thi. |

Quyền được kiểm tra tại backend. Đổi role ở giao diện hoặc gọi API trực tiếp không bỏ qua được kiểm tra phân công môn học.

## Nhóm 1: câu hỏi và rubric

Giáo viên tạo/công bố rubric. Tại Ngân hàng câu hỏi, chọn **Import câu hỏi** (CSV UTF-8/JSON có mẫu tải xuống) hoặc **Nhờ AI tạo câu hỏi** (PDF/DOCX đã xử lý và rubric đã công bố). Vẫn có thể tạo thủ công. Câu mới luôn có trạng thái PENDING_REVIEW, nằm trong tab **Chờ giáo viên duyệt**. Sau khi sinh/import thành công, giao diện chuyển về danh sách này ngay trong môn đang chọn. Giáo viên xem nguồn/đáp án, chỉnh sửa nội dung, đáp án, Bloom, độ khó và rubric; hoặc loại bỏ câu không phù hợp.

Import tối đa 2 MB, 200 câu/lần. Cột CSV/khóa JSON: `content`, `referenceAnswer` (tùy chọn), `bloomLevel`, `difficulty`, `rubricId` (tùy chọn khi đã chọn rubric mặc định). Bloom nhận REMEMBER/UNDERSTAND/APPLY/ANALYZE hoặc Nhớ/Hiểu/Vận dụng/Phân tích; độ khó EASY/MEDIUM/HARD. Có bước xem trước và lỗi từng dòng, kiểm tra trùng và rubric đúng môn đã công bố. Commit kiểm tra lại dữ liệu; nếu có bất kỳ dòng lỗi, không nhập một phần. Lịch sử lưu question_import_batches và mỗi câu liên kết import_batch_id.

Giáo viên có thể duyệt, từ chối, sửa, loại bỏ hoặc giữ nguyên để xem xét sau. Import và sinh AI không tự duyệt. Chi tiết câu hỏi hiển thị Bloom, rubric gồm các tiêu chí và điểm tối đa. Khi bắt đầu thi, hệ thống lưu snapshot đáp án, Bloom/độ khó, tên rubric, tổng điểm và điểm từng tiêu chí để làm căn cứ chấm gợi ý sau này; việc này không đồng nghĩa đã triển khai chấm điểm AI.

Chỉ sau khi giáo viên duyệt, câu hỏi mới có trạng thái APPROVED và hiển thị ở **Ngân hàng chính thức**. Rubric gắn với câu hỏi phải thuộc môn và đã PUBLISHED. Các quyết định lưu reviewer, thời điểm và lịch sử trong question_reviews.

Sửa câu APPROVED đưa câu về PENDING_REVIEW, xóa thông tin phê duyệt và gỡ câu khỏi đề thi DRAFT. Phải duyệt lại và thêm lại vào đề sau khi chỉnh sửa. Câu đang dùng trong kỳ thi ACTIVE/SCHEDULED bị khóa sửa/loại bỏ. Sau khi kết thúc kỳ thi, có thể chỉnh sửa cho các kỳ thi sau; các snapshot lượt thi trước giữ nguyên.

Loại bỏ dùng trạng thái ARCHIVED để không phá lịch sử thi và bằng chứng nguồn. Câu bị loại không được chọn vào đề. Đây là loại bỏ khỏi sử dụng, không xóa vật lý bản ghi.

## Nhóm 2: kỳ thi và lịch thi

Giới hạn hỏi xoáy theo từng câu chính được cấu hình riêng bằng `maxFollowupsPerQuestion` (0–10). Hệ thống áp dụng đồng thời với tổng `maxFollowupsPerStudent` (0–100): chạm một trong hai giới hạn thì không hỏi tiếp câu đó. Giới hạn mỗi câu đặt lại khi chuyển sang câu chính mới; tổng của sinh viên không đặt lại. Ví dụ 3 câu chính, 2 hỏi xoáy/câu, tổng 4 thì toàn bộ lượt có tối đa 7 câu, không quá 2 hỏi xoáy dưới một câu chính.

Giáo viên tự tạo kỳ thi nháp, cấu hình số câu chính (1–100), **tổng số câu đào sâu tối đa mỗi sinh viên (0–100)**, cách chọn bộ câu RANDOM/ADAPTIVE và thời gian trả lời. Ví dụ 3 câu chính + 4 câu đào sâu nghĩa là một sinh viên nhận tối đa 7 câu; ngân sách đào sâu không đặt lại khi chuyển câu chính và được kiểm tra tại backend, kể cả sau reload/retry AI. Màn hình hiển thị số câu đào sâu đã dùng. AI có thể hỏi ít hơn giới hạn nếu câu trả lời đã rõ. Mỗi sinh viên có ngân sách riêng.

Màn hình hiển thị danh sách câu hỏi trong đề và số câu hợp lệ. Giáo viên thêm/gỡ câu đã duyệt khi đề còn DRAFT; chỉ kích hoạt khi đủ câu hợp lệ.

Admin quản lý ghi danh tại **Môn học & sinh viên**, có bộ lọc môn theo giảng viên. Giáo viên chỉ chọn sinh viên đã được ghi danh ACTIVE để phân lịch; không còn nút ghi danh hoặc menu Môn học & sinh viên. API tạo lịch không tạo/kích hoạt lại ghi danh kể cả khi client cũ gửi enrollIfNeeded=true. Danh sách thí sinh hiển thị email, họ tên, trạng thái và khung giờ. Giáo viên sửa hoặc bỏ lịch khi sinh viên chưa bắt đầu thi. Từ chối sửa/xóa nếu đã có lượt thi, kể cả trạng thái candidate bị sửa sai. Xem EXAM_MONITORING.md về tiến độ, transcript và âm thanh.

Sinh viên mở kỳ thi theo tên trong lịch của mình. RANDOM chọn ngẫu nhiên từ các câu APPROVED trong đề, ưu tiên tránh bộ câu của lượt bắt đầu ngay trước trong cùng kỳ thi. ADAPTIVE còn ưu tiên câu ít dùng trong 20 lượt gần nhất và đa dạng Bloom/độ khó; thích ứng ở đây dựa trên lịch sử sử dụng đề, không suy đoán năng lực hoặc điểm của sinh viên. Câu đào sâu do AI thích ứng với câu trả lời trong từng lượt thi.

Khi nguồn câu ít hơn hai lần số câu chính, chỉ phần trùng không thể tránh được mới được dùng. Giao diện cảnh báo mức trùng tối thiểu và đề nghị thêm câu. Việc cấp bộ câu được tuần tự hóa theo kỳ thi để xử lý cả các lượt bắt đầu đồng thời.

Giáo viên có thể **Kết thúc kỳ thi** khi không còn lượt IN_PROGRESS. Kỳ thi chuyển CLOSED, giữ lịch sử và không nhận lượt thi mới. Thời gian kết thúc của từng thí sinh vẫn được kiểm soát riêng như trước.

## API quản lý bổ sung

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| PUT | `/api/v1/questions/{id}` | Sửa và đưa câu về chờ duyệt |
| POST multipart | `/api/v1/courses/{id}/question-imports` | `file`, `rubricId` mặc định tùy chọn, `preview=true/false` (mặc định true) |
| DELETE | `/api/v1/questions/{id}` | Loại bỏ khỏi sử dụng |
| GET | `/api/v1/courses/{id}/students` | Danh sách ghi danh của môn |
| PUT | `/api/v1/exams/{id}` | Sửa cấu hình kỳ thi DRAFT |
| GET | `/api/v1/exams/{id}/questions` | Câu hỏi trong đề |
| DELETE | `/api/v1/exams/{id}/questions/{questionId}` | Gỡ câu khỏi đề DRAFT |
| GET | `/api/v1/exams/{id}/candidates` | Danh sách thí sinh |
| PUT | `/api/v1/exams/{id}/candidates/{candidateId}` | Đổi khung giờ trước khi bắt đầu |
| DELETE | `/api/v1/exams/{id}/candidates/{candidateId}` | Bỏ lịch trước khi bắt đầu |
| PATCH | `/api/v1/exams/{id}/close` | Kết thúc kỳ thi |

Thêm cột `exams.max_followups_per_student`. Với `ddl-auto=update` hiện tại, Hibernate thêm cột khi khởi động; khi giá trị còn null, ứng dụng dùng giá trị cũ làm tổng giới hạn cho một sinh viên. Nếu quản lý schema bằng SQL hoặc dùng validate, chạy `database/04_student_followup_budget.sql`. Cột `max_followups_per_question` hiện được dùng để giới hạn riêng từng câu chính; giao diện và backend áp dụng đồng thời giới hạn này với tổng `maxFollowupsPerStudent`. Không xóa lịch sử hoặc câu đã hỏi của lượt thi cũ; nếu đã dùng quá giới hạn mới thì không sinh thêm câu đào sâu.

Khởi động lại backend để nạp controller/schema và quyền mới, tải lại frontend, đăng nhập bằng tài khoản LECTURER được phân công môn.

Các thao tác sửa/xóa bổ sung cho tài khoản, môn học, phân công, ghi danh, tài liệu, rubric và kỳ thi nháp được mô tả tại [CRUD_OPERATIONS.md](CRUD_OPERATIONS.md), gồm điều kiện bảo vệ dữ liệu đang dùng và lịch sử thi.
