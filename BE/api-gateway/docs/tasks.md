# Checklist Nhiệm vụ: API Gateway (`api-gateway`)

Tài liệu này theo dõi các đầu việc cần làm tại API Gateway để tích hợp bảo mật JWT và cơ chế chống gọi trực tiếp (bypass Gateway).

---

## 📌 1. Cấu hình Properties (`application.properties`)
- [x] Thêm cấu hình mã bí mật dùng chung giữa Gateway và các Service:
  ```properties
  gateway.shared-secret=gateway-secret-signature-2026
  ```
- [x] Cấu hình Route định tuyến cho **Booking Service**:
  ```properties
  # Route 4: API Booking & Payment (Đặt vé, thanh toán, IPN callback)
  spring.cloud.gateway.server.webflux.routes[3].id=booking-route
  spring.cloud.gateway.server.webflux.routes[3].uri=lb://booking
  spring.cloud.gateway.server.webflux.routes[3].predicates[0]=Path=/api/bookings/**, /api/payments/**
  ```

---

## 📌 2. Cập nhật `JwtGlobalAuthenticationFilter.java`
- [x] Thay đổi mã giải mã token để đọc thêm các Claims mới: `email` và `permissions` (danh sách quyền).
- [x] Thực hiện chèn thêm các Header chuyển tiếp xuống service nội bộ:
  - `X-Gateway-Token`: Giá trị của `gateway.shared-secret`.
  - `X-User-Id`: ID người dùng (`claims.getSubject()`).
  - `X-User-Role`: Vai trò (`claims.get("role")`).
  - `X-User-Email`: Email (`claims.get("email")`).

---

## 📌 3. Kiểm thử & Xác minh (Verification)
- [ ] Khởi chạy Eureka Server (`discovery-service`).
- [ ] Khởi chạy `api-gateway`.
- [ ] Kiểm tra xem Gateway có nhận diện chính xác các API công khai (như `/api/auth/login`, `/api/auth/register`, `/api/events/**`) để cho đi qua mà không check JWT.
- [ ] Kiểm tra xem các API yêu cầu bảo mật có bị chặn lại và trả về `401 Unauthorized` nếu thiếu hoặc sai JWT không.
