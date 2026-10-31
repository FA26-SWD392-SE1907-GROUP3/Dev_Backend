# Sửa lỗi cạn connection pool ngày 19/10/2026

## Bằng chứng và giới hạn chẩn đoán

- Log được cung cấp cho thấy Spring Boot khởi động thành công trên cổng 8080.
- Từ 10:54, Hikari báo `total=5, active=5, idle=0`, có 14–19 yêu cầu chờ kết nối và timeout sau khoảng 30 giây. Lỗi ảnh hưởng cả xác thực, tải trạng thái phỏng vấn và bộ xử lý timeout.
- Frontend trước bản sửa gọi GET trạng thái mỗi ba giây mà không kiểm tra GET trước đã hoàn tất. Khi backend chậm, cùng một phòng thi có thể tích tụ nhiều yêu cầu.
- Thread dump của JVM 25820 tại thời điểm kiểm tra thấy worker đang đọc kết quả JDBC. Truy vấn chỉ đọc `pg_stat_activity` khi chẩn đoán không thấy session bị chặn bởi khóa. Vì trạng thái quá tải đã qua, chưa đủ bằng chứng xác định một câu SQL hoặc một lỗi mạng cụ thể đã gây đợt nghẽn ban đầu.

## Thay đổi

1. Phòng phỏng vấn chỉ cho một GET tải trạng thái chạy tại một thời điểm. Khi đổi lượt thi hoặc rời phòng, request bị hủy; phản hồi cũ không ghi đè thao tác mới. Việc nộp câu trả lời và giới hạn giờ phía máy chủ được giữ.
2. Đọc toàn bộ hội thoại bằng một truy vấn theo danh sách câu hỏi, rồi nhóm và sắp theo thứ tự câu/lượt trong bộ nhớ. Không truy vấn lại hội thoại cho từng câu đã hoàn thành; dùng cùng dữ liệu để tính số lượt hỏi đào sâu trong response.
3. Cấu hình Hikari/pgJDBC có giới hạn chờ và cho phép điều chỉnh bằng biến môi trường:

| Biến | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `DB_POOL_SIZE` | 5 | Giới hạn connection pool hiện có |
| `DB_POOL_CONNECTION_TIMEOUT_MS` | 10000 | Thời gian chờ mượn connection, mili giây |
| `DB_CONNECT_TIMEOUT_SECONDS` | 10 | Chờ thiết lập socket |
| `DB_SOCKET_TIMEOUT_SECONDS` | 30 | Chờ đọc socket |
| `DB_QUERY_TIMEOUT_SECONDS` | 15 | Query timeout mặc định của driver; có thể được statement/transaction timeout ghi đè |
| `DB_CANCEL_TIMEOUT_SECONDS` | 5 | Chờ gửi lệnh hủy query |
| `DB_TRANSACTION_TIMEOUT` | 30s | Giới hạn giao dịch mặc định của Spring |

`tcpKeepAlive` được bật. Đơn vị và hành vi của các thuộc tính driver được đối chiếu với [tài liệu pgJDBC](https://jdbc.postgresql.org/documentation/use/); thuộc tính Spring được đối chiếu với [Spring Boot application properties](https://docs.spring.io/spring-boot/appendix/application-properties/index.html). Giới hạn chờ giúp yêu cầu thất bại có kiểm soát khi database/mạng bị gián đoạn; không thay thế việc khôi phục hạ tầng khi có sự cố kéo dài.

4. Lỗi không mở được giao dịch, lỗi timeout hoặc mất kết nối được trả HTTP 503 với `Retry-After: 5` và thông báo tiếng Việt. Áp dụng cả trước controller trong JWT filter, tránh trả lỗi xác thực 401 khi database đang bận. Thao tác ghi không được tự động gửi lại.

## Kiểm chứng

- Kiểm thử tập trung: 24 test đạt, gồm ranh giới module, các tình huống database không sẵn sàng, luồng phỏng vấn và kiểm tra số truy vấn khi lịch sử tăng từ một lên 20 câu.
- Playwright trên Microsoft Edge: tám test phòng phỏng vấn đạt, gồm giọng nói, hỏi đào sâu, khôi phục lỗi AI và server phản hồi chậm không tạo poll chồng nhau.
- Frontend: 21 test API/nhận biết lượt nói đạt; build Vite thành công.
- Build JAR và toàn bộ 89 kiểm thử backend đạt, không failure/error; kết quả ở `target/database-recovery-validation/surefire-reports/`.
- Các test backend dùng H2, AI và giao diện dùng mock. Chẩn đoán Neon chỉ đọc metadata; không sửa dữ liệu hoặc hủy session database.

## Áp dụng

Khi kết thúc ca thi đang chạy: Stop backend, Rebuild Project rồi Run lại; tải lại frontend. JVM đang chạy từ trước bản sửa chưa có các timeout và code mới. Không xóa output của JVM hiện hành trong lúc thi. JAR kiểm chứng được build ở `target/database-recovery-validation/`.
