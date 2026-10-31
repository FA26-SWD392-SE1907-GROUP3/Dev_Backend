# Reset dữ liệu Neon, chỉ giữ tài khoản và môn học

Chạy tại thư mục backend chứa `pom.xml`. Script dùng JDK 21 trở lên, Maven wrapper và cấu hình `.env` hiện có; không chứa mật khẩu hoặc API key trong mã.

## Xem trước — không xóa dữ liệu

```powershell
.\scripts\Reset-NeonData.ps1
```

Script hiện hostname, database, số dòng cần giữ và số dòng sẽ xóa. Mặc định chỉ xem trước.

## Sao lưu rồi reset

```powershell
.\scripts\Reset-NeonData.ps1 -Execute
```

Giữ nguyên toàn bộ `public.users` và `public.courses`, gồm ID, mật khẩu, quyền và nội dung môn học. Xóa dữ liệu của mọi bảng còn lại trong schema `public`, kể cả bảng nghiệp vụ mới được thêm sau này. Schema hệ thống như `neon_auth` được giữ nguyên.

Phân công giảng viên, ghi danh sinh viên, học liệu, câu hỏi, rubric, kỳ thi và transcript đều bị xóa. Sau reset, cần phân công/ghi danh lại nếu muốn giảng viên hoặc sinh viên truy cập môn học.

Nên dừng backend và tránh thao tác trên website trong lúc reset để dữ liệu không thay đổi giữa lúc sao lưu và xóa. Nếu dữ liệu đã đổi, có quan hệ khóa ngoại không an toàn hoặc không lấy được khóa bảng, script dừng thay vì tiếp tục xóa.

## Bản sao lưu

Mỗi lần `-Execute` tạo một bản sao mới trong `backups/neon-reset/before-reset-.../`:

- `manifest.json`: số dòng, cột, khóa ngoại và checksum.
- `*.jsonl`: dữ liệu gốc của từng bảng ứng dụng.
- `restore-cleared-data.sql`: phục hồi các bảng đã xóa; script phục hồi giữ users/courses và dừng nếu hai bảng này đã thay đổi so với bản sao lưu.
- `reset-result.json`: kết quả reset, số dòng còn lại và xác nhận dữ liệu cần giữ không đổi.

Thư mục `backups/` được Git bỏ qua vì chứa dữ liệu riêng tư và hash mật khẩu. Đừng đưa bản sao lưu vào Git. Có thể chọn thư mục sao lưu khác:

```powershell
.\scripts\Reset-NeonData.ps1 -Execute -BackupDirectory 'D:\AIVES-Backups'
```

## Cấu hình và phạm vi

`.env` cần có `DB_URL` dạng `jdbc:postgresql://...`, `DB_USERNAME` và `DB_PASSWORD`. Đổi kết nối ở `.env` khi đổi Neon; không cần sửa script. Có thể chỉ định tệp khác bằng `-EnvFile`.

Lần đầu, Maven lấy classpath từ các thư viện đã có trong `pom.xml` và lưu cache trong `target/neon-reset-tool/`. Script không build hay khởi động ứng dụng, không thay Spring Data JPA, không chạy seed/migration, không đổi cấu trúc bảng hoặc sequence và không xóa tệp ghi âm/học liệu trên ổ đĩa.

Khi reset, script kiểm tra bản sao lưu, khóa các bảng, xóa trong một giao dịch, kiểm tra các bảng cần xóa đã rỗng và đối chiếu toàn bộ dữ liệu users/courses trước khi commit. Nếu bước kiểm tra thất bại trước commit, giao dịch được rollback.

Nếu PowerShell chặn chạy script, có thể chạy riêng lệnh đã xem trước này:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Reset-NeonData.ps1 -Execute
```
