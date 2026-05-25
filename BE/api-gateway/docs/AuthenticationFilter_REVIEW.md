AuthenticationFilter — Code review

Tóm tắt
- File: `src/main/java/ict/thesis/api_gateway/filter/AuthenticationFilter.java`
- Mục đích: làm Gateway `GatewayFilterFactory` kiểm tra JWT trong header `Authorization`, giải mã, và thêm header `X-User-Id`/`X-User-Role` trước khi chuyển xuống service đích.

Những gì đã làm đúng
- Đăng ký filter dưới dạng `@Component` và mở rộng `AbstractGatewayFilterFactory` — Spring Cloud Gateway có thể dùng nó làm route filter.
- Sử dụng JJWT (`io.jsonwebtoken`) + `Keys.hmacShaKeyFor(...)` để tạo `SecretKey` tương thích HMAC.
- Trả về 401 khi thiếu/không hợp lệ token và chặn request tại Gateway (ngăn truy cập không hợp lệ tới downstream services).
- Thêm `X-User-Id` và `X-User-Role` vào request headers để downstream service dễ sử dụng thông tin người dùng.

Vấn đề / rủi ro cần xử lý
1. Bảo mật secret
   - Hiện `jwt.secret` phải được cấu hình (application.properties). KHÔNG commit secret thật vào VCS.
   - Gợi ý: đọc từ biến môi trường hoặc vault (Spring Cloud Config / HashiCorp Vault) và đảm bảo key đủ dài (256-bit cho HS256).

2. Tính tương thích tên Filter
   - Spring Cloud Gateway đăng ký `GatewayFilterFactory` theo tên lớp loại bỏ hậu tố `GatewayFilterFactory`. Vì lớp tên `AuthenticationFilter` nên tên factory sẽ là `Authentication` (hoặc `authentication` tùy cấu hình). Trong dự án cấu hình route đang dùng `AuthenticationFilter` (chỉ số), nhưng hiện đang hoạt động — kiểm tra kỹ nếu đổi tên lớp.

3. Xử lý lỗi chi tiết
   - Hiện trả 401 nhưng không có body JSON giải thích lỗi. Nên trả JSON nhỏ (code/message) để client hiểu lý do.

4. Kiểm tra expiry/claims
   - Code hiện chỉ parse và lấy `sub` + `role`. Cần kiểm tra `exp` (hết hạn) và các claim khác (issuer/audience) nếu cần.

5. Xử lý null/giá trị rỗng
   - Nếu `userId` hoặc `role` null thì filter thêm header rỗng. Thay vào đó bạn có thể: block request (nếu bắt buộc), hoặc không thêm header nếu thiếu.

6. Lỗi logging và giám sát
   - Nên log các lỗi ở mức `warn/error` (không log payload token) để dễ debug.

7. Unit / Integration tests
   - Viết unit test cho filter (mock `ServerWebExchange` và chain) và e2e test với token hợp lệ/sai.

8. Performance
   - Việc parse token cho mỗi request có overhead rất nhỏ nhưng nếu cần tối ưu bạn có thể cache validation results trong ngắn hạn (ít phút) theo token id.

Cải tiến đề xuất (ưu tiên)
- [P1] Thay `@Value("${jwt.secret}")` bằng đọc từ `Environment` hoặc `@ConfigurationProperties` và validate độ dài.
- [P1] Trả JSON lỗi: {"status":401,"error":"Unauthorized","message":"Missing Authorization Header"}
- [P1] Kiểm tra `exp`, `iss`, `aud` nếu hệ thống của bạn dùng.
- [P2] Thêm logging (logger.warn / logger.debug) cho lỗi xác thực.
- [P2] Thêm Unit test cho các case: thiếu header, format sai, token expire, token valid.
- [P3] Hỗ trợ multiple roles (Array) nếu Identity trả `roles` list — parse an array, gửi `X-User-Roles` hoặc JWT claim raw.
- [P3] Hợp thức hóa tên filter: đổi tên lớp thành `AuthenticationGatewayFilterFactory` nếu muốn tên factory là `Authentication` theo convention.

Cách kiểm thử (local)
1. Start `identity` service và lấy JWT (POST /api/v1/auth/login hoặc endpoint tương ứng) — lưu token.
2. Gọi endpoint đi qua gateway (ví dụ `http://localhost:8080/api/auth/users/me`) kèm header:

```bash
curl -i -H "Authorization: Bearer <JWT>" http://localhost:8080/api/auth/users/me
```

- Kết quả mong đợi: gateway trả 200 từ service đích; request tới identity service nhận thêm header `X-User-Id`.
- Trường hợp token thiếu/không hợp lệ: gateway trả 401.

Các thay đổi đã thực hiện trong repo
- Thêm file: `src/main/java/ict/thesis/api_gateway/filter/AuthenticationFilter.java`
- Thêm dependency JJWT vào `pom.xml` (jjwt-api, jjwt-impl, jjwt-jackson)
- Tắt/Khôi phục cấu hình route tương ứng trong `src/main/resources/application.properties` (`spring.cloud.gateway.server.webflux.routes[1].filters[0]=AuthenticationFilter`)

Next steps tôi có thể làm cho bạn
- (1) Chỉnh sửa filter theo các gợi ý P1 (JSON lỗi, kiểm tra `exp`, đọc secret an toàn) và commit thay đổi.
- (2) Viết unit tests cho filter.
- (3) Hướng dẫn cấu hình secret an toàn (env var / vault) và thêm ví dụ `application-local.properties`.

Bạn muốn tôi thực hiện bước nào tiếp theo? Chọn một số (1/2/3) hoặc mô tả thêm yêu cầu cụ thể.
