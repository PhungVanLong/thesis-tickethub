# Checklist Nhiệm vụ: Identity Service (`identity`)

Tài liệu này theo dõi các đầu việc cần làm tại Identity Service để bổ sung đầy đủ Claims vào token JWT.

---

## 📌 1. Cấu hình Properties (`application.properties`)
- [ ] Đảm bảo mã khóa bí mật JWT trùng khớp 100% với API Gateway:
  ```properties
  jwt.secret=change-this-secret-key-please-change-very-long
  jwt.expiration-seconds=604800 //1week
  ```
---

## 📌 2. Cập nhật mã nguồn JWT (`JwtService.java`)
- [ ] Chỉnh sửa phương thức `generateToken` để nhận thêm tham số: `email` và danh sách quyền `permissions`.
  ```java
  public String generateToken(String userId, String role, String email, List<String> permissions) {
      Instant now = Instant.now();
      Instant expiresAt = now.plusSeconds(expirationSeconds);

      return Jwts.builder()
                 .subject(userId)
                 .issuedAt(Date.from(now))
                 .expiration(Date.from(expiresAt))
                 .claim("role", role)
                 .claim("email", email)
                 .claim("permissions", permissions) // Lưu list permissions
                 .signWith(secretKey)
                 .compact();
  }
  ```

---

## 📌 3. Cập nhật mã nguồn Auth Service (`AuthServiceImpl.java`)
- [ ] Tìm class thực thi logic đăng nhập và đăng ký (thường là `AuthServiceImpl.java` hoặc `AuthService.java`).
- [ ] Cập nhật luồng gọi `generateToken` tại phương thức `login` và `register`:
  - Lấy thông tin `user.getId()`, `user.getRole().name()`, `user.getEmail()`.
  - Định nghĩa danh sách các quyền cơ bản theo role (Ví dụ: `CUSTOMER` có quyền `["booking:create", "booking:view"]`, `ORGANIZER` có quyền `["event:create", "event:edit"]`).
  - Truyền các tham số này vào hàm `generateToken(...)` để trả về JWT đầy đủ thông tin cho client.

---

## 📌 4. Kiểm thử
- [ ] Khởi chạy Eureka Server và `identity` service.
- [ ] Gọi API đăng nhập `POST /api/auth/login` bằng Postman.
- [ ] Lấy chuỗi JWT trả về, dán vào trang [jwt.io](https://jwt.io) để giải mã.
- [ ] Xác minh xem phần Payload có chứa đầy đủ các khóa: `sub`, `email`, `role`, `permissions` hay chưa.
