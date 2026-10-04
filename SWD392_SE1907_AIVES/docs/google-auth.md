# Đăng ký và đăng nhập Google

Chạy `database/04-google-auth.sql` sau migration 02 trước khi khởi động backend mới.
Migration này đã được áp dụng cho SQL Server local trong phiên triển khai.

## API thật

| API | Hành vi |
| --- | --- |
| POST /api/auth/register | Tạo tài khoản STUDENT và trả token + user, HTTP 201 |
| POST /api/auth/login | Đăng nhập bằng username/password |
| GET /api/auth/google/config | Client ID công khai và trạng thái cấu hình |
| GET /api/auth/google/challenge | Nonce một lần, hết hạn sau 5 phút |
| POST /api/auth/google | Nhận credential + nonce, xác thực Google ID token, trả session AIVES |

Body đăng ký: `username`, `password`, `fullName`, `email`. Username 3–50 ký tự
chữ/số/dấu chấm/gạch dưới/gạch ngang. Mật khẩu ít nhất 8 ký tự, tối đa 72 byte
UTF-8, được lưu BCrypt. Không cho người tự đăng ký chọn quyền giảng viên/admin.
Đăng ký local hiện chưa có bước xác nhận email.

## Cấu hình Google

1. Trong Google Cloud Console, tạo/chọn project và cấu hình OAuth consent screen
   trong Google Auth Platform; thêm test users nếu ứng dụng đang ở chế độ Testing.
2. Tạo OAuth Client ID loại **Web application**.
3. Thêm Authorized JavaScript origins: `http://localhost:3000`.
   Nếu mở frontend bằng 127.0.0.1, thêm `http://127.0.0.1:3000`.
4. Đặt biến môi trường ở terminal backend rồi khởi động lại:

```powershell
$env:GOOGLE_CLIENT_ID = 'YOUR_CLIENT_ID.apps.googleusercontent.com'
.\mvnw.cmd spring-boot:run
```

Frontend lấy Client ID từ backend; không cần thêm biến Google phía frontend.
Luồng này dùng Google Identity Services popup/credential callback, không cần
Client Secret hay redirect URI cho callback AIVES. Backend và trình duyệt cần
kết nối tới Google. Chưa có Client ID thì nút Google thể hiện trạng thái chưa cấu hình.

Backend xác thực chữ ký RSA bằng khóa công khai Google, issuer, audience,
expiration, email_verified, subject và nonce một lần. Tài khoản Google mới là
STUDENT. Danh tính được lưu theo Google `sub`, không tự liên kết bằng email với
tài khoản local có sẵn; người đó tiếp tục dùng mật khẩu local. Tài khoản bị vô hiệu
hóa không đăng nhập được. Google đổi email không tạo ra tài khoản khác khi `sub` giữ nguyên.

Tài liệu chính thức:
- https://developers.google.com/identity/gsi/web/guides/get-google-api-clientid
- https://developers.google.com/identity/gsi/web/guides/verify-google-id-token
- https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html

Kiểm tra Google bằng tài khoản thật cần Client ID do nhóm tạo. Test backend dùng
token fixture và bộ kiểm tra chữ ký; không thay thế việc cấu hình/test Google thật.
