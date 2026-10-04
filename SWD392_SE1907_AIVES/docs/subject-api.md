# API môn học

CRUD môn học đã có phân trang, validation và xử lý lỗi. Backend hiện yêu cầu đăng nhập:

1. POST `/api/auth/login` với username/password của tài khoản AIVES.
2. Dùng token trả về trong header `Authorization: Bearer <token>`.
3. Admin có quyền thêm/sửa/xóa môn học; tài khoản đã đăng nhập có thể xem danh sách và chi tiết.

| Method | Endpoint | Kết quả |
|---|---|---|
| POST | /api/subjects | 201 |
| GET | /api/subjects?page=0&size=20 | 200 |
| GET | /api/subjects/{id} | 200 hoặc 404 |
| PUT | /api/subjects/{id} | 200 |
| DELETE | /api/subjects/{id} | 204, 404 hoặc 409 nếu đang được tham chiếu |

Body cho POST/PUT:

```json
{
  "subjectCode": "SWD392",
  "subjectName": "Software Architecture and Design",
  "description": "AIVES project"
}
```

Xem [hướng dẫn đầy đủ](full-api.md) để cấu hình database, tạo admin, chạy backend/frontend và demo các chức năng 1, 2, 3.
