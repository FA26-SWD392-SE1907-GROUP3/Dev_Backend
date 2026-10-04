# Admin đổi vai trò tài khoản

Trong frontend: **Tài khoản → Sửa → Vai trò → Lưu**.
Chọn Quản trị viên (`ADMIN`), Giảng viên (`LECTURER`) hoặc Sinh viên (`STUDENT`).

API `PUT /api/users/{id}` chỉ dành cho ADMIN:

```json
{
  "fullName": "Nguyễn Minh An",
  "email": "an@example.com",
  "status": "ACTIVE",
  "role": "LECTURER"
}
```

Không gửi `role` thì giữ nguyên vai trò, để tương thích các client cũ.
Vai trò không hợp lệ trả 400. Tài khoản không phải Admin gọi API trả 403.
Admin không thể tự hạ quyền tài khoản đang đăng nhập (409).
Sau khi cập nhật, các token của tài khoản được sửa bị thu hồi;
người dùng đăng nhập lại để xem menu và dùng quyền mới.
Lịch thi, câu hỏi và dữ liệu đã tạo được giữ nguyên. Không cần migration SQL.
