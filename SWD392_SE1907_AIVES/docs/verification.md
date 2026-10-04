# Kết quả kiểm tra - 02/10/2026

## Backend

- Maven package thành công, Java 21 chạy source target Java 17.
- 8 test, không lỗi: 3 SubjectService, 1 retrieval, 1 application context, 3 HTTP workflow integration.
- Luồng kiểm tra: xác thực/thu hồi token, phân quyền và quyền sở hữu, rubric trước duyệt, chống trùng lịch, ngân hàng đang dùng, start/next retry, giới hạn hỏi thêm, deadline server, không lặp câu chính, giữ transcript khi AI lỗi, upload/index TXT, AI generation có rubric và import rollback toàn bộ khi có câu không hợp lệ.
- Integration test dùng H2 riêng và test double cho AI. Không xác minh chất lượng hay độ trễ model thật.

## SQL Server thực tế

- Kết nối AIVES trên SQL Server của workspace thành công.
- Migration 02 chạy thành công, giữ dữ liệu hiện có.
- Startup Hibernate validate schema thành công.
- Tài khoản kiểm tra tạm đăng nhập thành công và gọi subjects/users/questions/documents/exams đều HTTP 200.
- Kiểm thử trình duyệt tạo/sửa/xóa môn học tiếng Việt; tạo câu hỏi/rubric và duyệt trên SQL Server thực tế.
- Tài khoản kiểm tra và dữ liệu tạo bởi các test này đã được dọn.

## Frontend

- Vite production build thành công.
- 4 API client test đạt: JSON request, lỗi conflict, lỗi network, empty 204.
- 2 Playwright test trên Microsoft Edge đạt: login + subject CRUD + persistence sau reload; question + rubric + approval.
- Chưa kiểm tra microphone/TTS/STT trên thiết bị thực tế, hay AI model thật. Cần cấu hình dịch vụ AI và kiểm tra trước khi demo chức năng 3 với giọng nói.

## Phạm vi

Triển khai nhóm chức năng 1, 2, 3 và tài khoản/môn học hỗ trợ. RAG sử dụng truy xuất từ khóa trên đoạn text, chưa có embedding/vector database. TTS/STT ở trình duyệt. Chưa triển khai AI chấm điểm và báo cáo điểm của nhóm 4, 6.

Code chưa commit/push tự động. Các tài khoản SQL Server và admin AIVES cần được cấu hình riêng theo `full-api.md`.
# Kiểm tra trước khi đẩy Quoc_BE — 05/10/2026

`mvn verify`: 12 test, 0 lỗi/thất bại/bỏ qua; executable JAR build thành công.
Bao gồm xác thực/đăng ký, Admin đổi vai trò và thu hồi token cũ, kiểm tra chữ ký
Google và các claim, quyền sở hữu, tài liệu/import/rubric và vòng đời vấn đáp.
Kiểm tra Google dùng khóa/token fixture; đăng nhập Google thật cần Client ID.
Test AI dùng fixture; chất lượng RAG/hỏi đào sâu cần model thật và thiết bị microphone.
