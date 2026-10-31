# Sửa lỗi `Could not resolve placeholder 'JWT_SECRET'`

## Nguyên nhân từ log bạn gửi

Đây là lỗi **thiếu cấu hình**, không phải lỗi Entity hoặc Neon:

```text
Caused by: org.springframework.util.PlaceholderResolutionException:
Could not resolve placeholder 'JWT_SECRET' in value "${JWT_SECRET}" <-- "${jwt.secret}"
```

Dòng chạy IntelliJ cho thấy working directory `D:\Dev_Backend`, nhưng thư mục chứa `pom.xml` là `D:\Dev_Backend\SWD392_SE1907_AIVES`. `spring-dotenv` có thể không tìm được `.env` nếu chạy từ thư mục cha.

## Cách sửa (IntelliJ IDEA trên Windows)

1. Mở **đúng thư mục `SWD392_SE1907_AIVES`** bằng IntelliJ (thư mục có `pom.xml`).
2. Copy `.env.example` thành file **`.env`** tại **cùng cấp với `pom.xml`**.
3. Thay các giá trị mẫu bằng `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (mật khẩu Neon **đã rotate**) và `JWT_SECRET` mới đủ dài. Không đưa `.env` lên Git.
4. Trong IntelliJ: **Run -> Edit Configurations -> Swd392Se1907AivesApplication**. Thiết lập **Working directory** là `$PROJECT_DIR$` (hoặc `D:\Dev_Backend\SWD392_SE1907_AIVES`).
5. Run lại ứng dụng. Phiên bản source này cũng có `LocalEnvLoader` tìm `.env` theo vị trí `pom.xml`, kể cả lúc IntelliJ chạy từ thư mục cha.

Tạo JWT secret ngẫu nhiên từ PowerShell (chạy một lần):

```powershell
$bytes = New-Object byte[] 48
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Ví dụ **cấu trúc** file `.env` (**KHÔNG** copy giá trị placeholder để chạy thật):

```dotenv
DB_URL=jdbc:postgresql://YOUR_NEW_NEON_HOST/neondb?sslmode=require&channelBinding=require
DB_USERNAME=neondb_owner
DB_PASSWORD=YOUR_NEW_NEON_PASSWORD
JWT_SECRET=YOUR_NEW_RANDOM_SECRET_AT_LEAST_32_BYTES
```

Nếu vẫn bị lỗi, có thể thêm riêng 4 biến trên trong **Edit Configurations -> Environment variables**. Không dán mật khẩu/secret thực lên ảnh chụp hoặc chat.

### Kiểm tra

- Ứng dụng start và in dòng `Started Swd392Se1907AivesApplication`.
- Truy cập `http://localhost:8080/actuator/health` để kiểm tra app.
- Nếu lỗi tiếp theo nói về `DB_URL`, kết nối hoặc thiếu bảng, đó là bước khác: kiểm tra Neon và chạy `database/01_schema.sql` **chỉ trên database trống**.

**Bảo mật:** Chuỗi Neon đã gửi trong chat phải được đổi mật khẩu trên Neon trước khi tiếp tục; source ZIP không chứa mật khẩu hoặc JWT secret thật.
