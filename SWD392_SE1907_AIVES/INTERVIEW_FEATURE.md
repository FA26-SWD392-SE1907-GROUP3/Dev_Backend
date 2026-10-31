# Nhóm 3 — Phỏng vấn AI

## Cấu hình và khởi động

API key được đọc từ `OPENAI_API_KEY` trong `.env` backend; `.env` được Git bỏ qua. Key không được đưa vào frontend hoặc URL WebSocket.

Các cấu hình tùy chọn trong `.env.example`:

```dotenv
OPENAI_INTERVIEW_MODEL=gpt-4.1-mini
OPENAI_STT_MODEL=gpt-4o-mini-transcribe
OPENAI_REALTIME_STT_MODEL=gpt-live-transcribe
OPENAI_TTS_MODEL=gpt-4o-mini-tts
OPENAI_TTS_VOICE=coral
INTERVIEW_STORAGE_DIR=D:/AivesData/interviews
```

Thư mục audio cần quyền ghi và ổ đĩa bền vững. Môi trường local đã được cấu hình đường dẫn riêng trong `.env`. Khởi động lại backend và Vite để nạp key, API và proxy WebSocket mới.

Hai cột `reference_answer_snapshot`, `rubric_snapshot` được thêm vào `attempt_questions` để giữ ngữ cảnh ra đề tại thời điểm bắt đầu thi. Với `ddl-auto=update` hiện tại, Hibernate thêm cột khi khởi động. Nếu dùng `validate`, chạy trước script `database/03_interview_snapshots.sql`. Script chỉ thêm cột, không xóa dữ liệu. Các lượt thi mới lưu snapshot; dữ liệu cũ có thể chưa có snapshot.

## Luồng sử dụng

### Phòng hội thoại bằng giọng nói

Chế độ mặc định là cuộc phỏng vấn trực tuyến bằng giọng nói. Sinh viên cấp quyền micro và vào phòng một lần; AI đọc câu hỏi, rồi micro tự ghi khi giọng đọc kết thúc. Phụ đề hiển thị trong lúc nói. Sau khi đã phát hiện lời nói và có khoảng lặng 4 giây, bản ghi tự gửi; AI xử lý câu trả lời, hỏi làm rõ hoặc chuyển câu chính, đọc câu mới và tiếp tục mở micro. Không tự nộp chỉ vì sinh viên chưa nói và đang suy nghĩ. Có nút Tôi đã trả lời xong để kết thúc lượt nói sớm.

Phát hiện kết thúc lượt dùng mức năng lượng âm thanh và khoảng lặng, không suy đoán sinh viên đã nói đủ ý. Nên dùng tai nghe, phòng yên tĩnh; tiếng ồn có thể làm chậm kết thúc lượt, còn khoảng dừng dài giữa câu có thể gửi sớm. Bản chính thức vẫn được nhận dạng từ tệp tại backend trước khi sinh follow-up. Đây là hội thoại luân phiên; chưa hỗ trợ nói chen để ngắt giọng AI, video hoặc streaming giọng AI trong lúc sinh nội dung.

Chế độ dự phòng giữ thao tác ghi âm/nhập văn bản và nộp từng câu, được nhớ trong sessionStorage sau reload. Khi mở lại phòng giọng nói, sinh viên cần bấm Tham gia phỏng vấn để cấp lại kết nối micro; deadline cũ vẫn có hiệu lực. Nếu trình duyệt chặn autoplay, nút Nghe câu hỏi cho phép tiếp tục. Micro ngừng ghi khi AI nói và giải phóng khi hoàn tất/rời phòng.

Giảng viên cấu hình độc lập **maxFollowupsPerQuestion (0–10)** và **maxFollowupsPerStudent (0–100)**. Backend kiểm tra cả hai trước và sau lời gọi AI. Ví dụ 3 câu chính, tối đa 2 hỏi xoáy/câu và tổng 4 hỏi xoáy/sinh viên: mỗi câu không vượt 2, cả lượt không vượt 4. Không thay đổi cấu hình của kỳ thi đang có khi cập nhật mã nguồn; chỉnh kỳ thi DRAFT nếu muốn đổi giới hạn.

1. Giảng viên tạo kỳ thi, đặt số câu chính, **tổng số câu đào sâu tối đa cho toàn bộ lượt thi của mỗi sinh viên** (`maxFollowupsPerStudent`) và thời gian trả lời (15–600 giây). Đặt 0 để chỉ hỏi câu chính. Ngân sách không nhân với số câu chính và không đặt lại khi đổi câu. Chọn RANDOM/ADAPTIVE, thêm đủ câu APPROVED vào đề, phân lịch sinh viên và kích hoạt. Xem `TEACHER_WORKFLOW.md` và migration `database/04_student_followup_budget.sql`.
2. Sinh viên mở lịch thi, vào phòng và bắt đầu lượt thi. Mỗi lần chỉ hiển thị một câu hiện tại; không gửi đáp án/rubric riêng cho sinh viên.
3. Giọng đọc OpenAI được bật mặc định và có điều khiển phát lại; giao diện thông báo giọng đọc là AI. Trình duyệt có thể yêu cầu bấm nút Nghe câu hỏi để cho phát âm thanh lần đầu.
4. Trong phòng giọng nói, đồng hồ bắt đầu khi AI đọc xong và micro mở. Trong chế độ dự phòng, chọn ghi âm hoặc bắt đầu trả lời bằng văn bản. Đồng hồ được lưu trên backend, không được đặt lại khi tải lại trang. Thời hạn mỗi lượt không vượt giờ kết thúc ca thi.
5. Micro cần HTTPS hoặc localhost và quyền của trình duyệt. Web Audio gửi PCM 24 kHz qua WebSocket backend tới OpenAI Realtime để hiển thị văn bản trực tiếp. MediaRecorder đồng thời giữ bản ghi đầy đủ. Khi nộp, backend dùng file STT để tạo transcript chính thức và lưu audio trên ổ đĩa.
6. AI nhận câu hỏi chính, đáp án/rubric snapshot và hội thoại để quyết định có cần một câu làm rõ hay không. Câu hỏi làm rõ không đưa đáp án cho sinh viên; số câu luôn bị chặn bởi cấu hình kỳ thi.
7. Câu trả lời đã nộp được giữ lại nếu AI hỏi tiếp bị lỗi. Nút Tiếp tục phỏng vấn chỉ xử lý tiếp câu trả lời đã lưu, không nộp trùng. Lỗi STT cho phép thử nộp lại bản ghi/ghi lại/nhập văn bản nếu vẫn còn thời gian.
8. Khi gần hết giờ, frontend tự gửi nội dung có sẵn; ghi âm dừng sớm 2 giây để gửi tệp. Backend kiểm tra thời hạn khi nhận yêu cầu, tự kết thúc câu hết giờ và ca hết lịch; độ trễ mạng lớn vẫn có thể khiến bản nộp đến quá hạn. Worker kiểm tra các lượt đang thi mỗi 5 giây, cùng kiểm tra tại từng API.
9. Hội thoại và âm thanh đã nộp vẫn xem/nghe được khi hoàn tất. Bản ghi đang giữ ở trình duyệt mà chưa nộp sẽ mất khi đóng/tải lại trang; dữ liệu đã nộp nằm trên server. Có thể tiếp tục lượt đang thi qua lịch sử lượt thi.

## API và phân quyền

Nếu không vào được phòng thi, khởi động lại backend để nạp API và cấu hình bảo mật mới, sau đó đăng xuất/đăng nhập lại. JWT thiếu, sai, hết hạn hoặc thuộc tài khoản bị khóa trả HTTP 401 kèm thông báo; frontend chuyển về đăng nhập. HTTP 403 được giữ cho tài khoản đã xác thực nhưng không có quyền dự thi, kèm lý do cụ thể. Lỗi nội bộ/đường dẫn không tồn tại không bị bộ lọc bảo mật che thành 403 khi dispatch lỗi.

Admin ghi danh hoặc kích hoạt lại ghi danh tại **Môn học & sinh viên**. Giảng viên chỉ phân lịch cho tài khoản STUDENT đang hoạt động và đã có ghi danh ACTIVE. `POST /api/v1/exams/{id}/candidates` không tạo/kích hoạt lại ghi danh; trường legacy enrollIfNeeded không cấp quyền cho giảng viên. Lịch trùng trả 409. Nếu ghi danh bị DROPPED, nhờ Admin ghi danh lại trước khi vào thi; không tạo lịch trùng. Giảng viên xem tiến độ, transcript và âm thanh tại Giám sát ca thi; admin không có chức năng này.

Mặc định CORS cho phép localhost và 127.0.0.1 trên cổng 3000/5173. Nếu đặt `CORS_ALLOWED_ORIGINS` riêng, danh sách đó thay thế mặc định và cần chứa đúng origin trình duyệt, không có dấu `/` cuối.

Tất cả API dưới đây yêu cầu JWT của chính sinh viên thuộc lượt thi:

| Method | `/api/v1/interviews/...` | Mục đích |
| --- | --- | --- |
| GET | `/{id}` | Trạng thái, câu hiện tại, deadline và transcript |
| POST | `/{id}/begin` | Bắt đầu đồng hồ; body `promptId` |
| POST | `/{id}/answers` | Nộp text; body `promptId`, `text` |
| POST | `/{id}/audio-answers` | Multipart `promptId`, `file` |
| POST | `/{id}/continue` | Thử xử lý AI tiếp; body `answerTurnId` |
| POST | `/{id}/timeout` | Đồng bộ khi hết giờ; body `promptId` |
| GET | `/{id}/speech/{turnId}` | Giọng đọc câu hỏi, có cache |
| GET | `/{id}/audio/{turnId}` | Nghe lại audio đã lưu |
| POST | `/{id}/live-ticket` | Vé WebSocket dùng một lần, hết hạn sau 30 giây |

WebSocket `/api/v1/interviews/live?ticket=...` chỉ chấp nhận vé còn hạn cho câu đang trả lời, giới hạn một kết nối mỗi câu và tự đóng khi hết giờ. Không chấp nhận API key/JWT chính trong query. Reverse proxy triển khai phải chuyển tiếp WebSocket và giữ Origin; `CORS_ALLOWED_ORIGINS` phải chứa đúng origin frontend. Vite đã bật `ws: true`.

Hai yêu cầu nộp đồng thời được tuần tự hóa bằng khóa DB trên lượt thi. Yêu cầu cũ có `promptId` không đúng bị từ chối để tránh nộp nhầm cho câu mới. Yêu cầu OpenAI chạy ngoài transaction; thông tin lỗi/thời gian xử lý lưu trong `ai_interview_requests`. Trường hợp quá trình chết giữa chừng cho phép thử tiếp sau thời hạn xử lý 150 giây.

## Phạm vi kiểm tra

Nếu trình duyệt báo kết nối nhận dạng trực tiếp bị gián đoạn và request `/api/v1/interviews/live` trả 400, kiểm tra thứ tự handler. Mapping WebSocket phải chạy trước MVC `GET /interviews/{id}`, nếu không chuỗi `live` bị convert sang UUID rồi lỗi trước bước upgrade. `InterviewWebSocketConfig` đặt thứ tự -1 trên ServletWebSocketHandlerRegistry. Khởi động lại JVM backend để nạp cấu hình; chỉ reload frontend không sửa được định tuyến của JVM đang chạy. LiveWebSocketIntegrationTests dùng server Tomcat nhúng với cổng thật để kiểm tra upgrade, chuyển ready/phụ đề, vé một lần, origin và bảo vệ REST bằng JWT.

Backend có kiểm thử H2 cho hỏi xoáy, giới hạn lượt, hết giờ, ca thi kết thúc, nộp đồng thời, phân quyền, cache TTS, audio/STT và phục hồi lỗi AI. Playwright dùng micro giả lập và API giả lập cho luồng giao diện. Các API OpenAI thật đã được thử riêng bằng âm thanh tổng hợp/câu trả lời mẫu ngắn, không dùng dữ liệu sinh viên.

Tính năng này chưa thực hiện chấm điểm AI, công bố điểm, chống gian lận hoặc ghi hình toàn bộ buổi thi; các phần đó thuộc các nhóm chức năng khác.

Tài liệu OpenAI: [Realtime transcription](https://developers.openai.com/api/docs/guides/realtime-transcription), [Text to speech](https://developers.openai.com/api/docs/guides/text-to-speech), [File transcription](https://developers.openai.com/api/docs/guides/speech-to-text).
