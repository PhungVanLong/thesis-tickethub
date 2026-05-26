# API Gateway Rules

## 1. Mục đích
`api-gateway` là lớp vào cửa duy nhất của hệ thống microservices. Nhiệm vụ của gateway là:

1. Phân biệt request công khai và request nội bộ.
2. Cho request công khai đi thẳng tới service đích.
3. Bóc và kiểm tra JWT đối với request nội bộ.
4. Từ chối sớm nếu token sai, hết hạn hoặc thiếu.
5. Gắn thông tin user từ token vào header để service phía dưới sử dụng.
6. Route request hợp lệ tới service phù hợp qua Eureka.

---

## 2. Thành phần chính

### 2.1. Route configuration
File cấu hình chính:
- `src/main/resources/application.properties`

Gateway đang route tới các service bằng `lb://` thông qua Eureka discovery.

### 2.2. JWT filter tập trung
Filter đang dùng:
- `src/main/java/ict/thesis/api_gateway/filter/JwtGlobalAuthenticationFilter.java`

Đây là filter trung tâm chịu trách nhiệm:
- kiểm tra whitelist public path
- kiểm tra header `Authorization`
- parse JWT
- chặn request nếu token không hợp lệ
- thêm header `X-User-Id` và `X-User-Role`

### 2.3. Filter cũ
- `AuthenticationFilter.java` đang tồn tại nhưng đã bị đánh dấu `@Deprecated`.
- Rule vận hành thực tế phải lấy theo `JwtGlobalAuthenticationFilter`.

---

## 3. Luồng xử lý tổng quát

### Bước 1: Nhận request từ FE / client
Client gửi request vào gateway.

### Bước 2: Kiểm tra path
Gateway kiểm tra path request có nằm trong danh sách public hay không.

- Nếu là public -> cho đi thẳng
- Nếu không phải public -> bắt buộc phải có JWT

### Bước 3: Kiểm tra JWT
Nếu request là internal:
1. Kiểm tra header `Authorization`
2. Kiểm tra format `Bearer <token>`
3. Parse token bằng secret cấu hình trong `jwt.secret`
4. Nếu token sai / hết hạn / thiếu -> trả về `401 Unauthorized`

### Bước 4: Gắn header user
Nếu token hợp lệ, gateway gắn thêm:
- `X-User-Id`
- `X-User-Role`

### Bước 5: Route xuống service con
Request sau khi qua filter sẽ được chuyển tiếp tới service đích theo route đã cấu hình.

---

## 4. Phân loại request

## 4.1. Public API
Các API công khai là các route được whitelist trong `gateway.security.public-paths`.

Hiện tại gồm:
- `/api/auth/login`
- `/api/auth/register`
- `/api/movies/**`
- `/swagger-ui.html`
- `/swagger-ui/**`
- `/v3/api-docs/**`
- `/api-docs/**`
- `/actuator/health`
- `/actuator/info`

### Quy tắc public
- Không cần token
- Gateway cho qua ngay
- Dùng cho đăng nhập, đăng ký, hoặc các API read-only công khai

> Nếu sau này thêm API công khai mới, chỉ cần thêm path vào `gateway.security.public-paths`.

## 4.2. Internal API
Các API còn lại được xem là nội bộ / bảo vệ.

### Quy tắc internal
- Bắt buộc có `Authorization: Bearer <token>`
- Token phải hợp lệ và chưa hết hạn
- Token sai -> `401 Unauthorized`
- Token thiếu -> `401 Unauthorized`
- Token hợp lệ -> gateway forward request và thêm headers user

---

## 5. Route mapping hiện tại

### 5.1. `management`
- `lb://management`
- Route path chính:
  - `/api/events/**`

#### Ý nghĩa
Các API liên quan tới sự kiện sẽ được chuyển tới service `management`.

#### Lưu ý
Tùy rule public-paths hiện tại, route này được coi là **internal** trừ khi bạn thêm nó vào whitelist.

---

### 5.2. `identity`
- `lb://identity`
- Route public:
  - `/api/auth/login`
  - `/api/auth/register`
- Route internal:
  - mọi path còn lại không nằm trong whitelist

#### Ý nghĩa
- Login / register đi thẳng
- Những API user/profile hoặc API nội bộ khác phải có token

---

## 6. Rule kiểm tra JWT

JWT filter thực hiện các bước sau:

1. Lấy đường dẫn request.
2. Nếu path nằm trong whitelist public -> cho qua.
3. Nếu method là `OPTIONS` -> cho qua (hỗ trợ CORS preflight).
4. Kiểm tra header `Authorization`.
5. Kiểm tra format `Bearer`.
6. Parse JWT bằng `jwt.secret`.
7. Nếu parse thất bại -> trả `401`.
8. Nếu thành công:
   - đọc `sub` làm `X-User-Id`
   - đọc claim `role` làm `X-User-Role`
   - forward request xuống service con

### Claim đang dùng
- `sub` -> user id
- `role` -> role của user

---

## 7. Headers gateway gắn thêm
Khi token hợp lệ, gateway thêm:

- `X-User-Id`: ID của user
- `X-User-Role`: role của user

### Mục đích
Downstream service có thể dùng các header này để:
- kiểm tra quyền
- log audit
- xác định user đang gọi
- map sang bảng local như `ref_user`

---

## 8. Error handling rule

### 8.1. Thiếu token
- Status: `401 Unauthorized`
- Khi request nội bộ không có `Authorization`

### 8.2. Sai format token
- Status: `401 Unauthorized`
- Khi header không bắt đầu bằng `Bearer `

### 8.3. Token hết hạn / token sai chữ ký / token lỗi
- Status: `401 Unauthorized`
- Gateway chặn ngay, không forward xuống service dưới

### 8.4. Request public
- Status: do service đích quyết định
- Gateway không kiểm JWT

---

## 9. Quy tắc viết route mới
Khi thêm route mới vào gateway, hãy làm theo các nguyên tắc sau:

### Nếu API là public
- Thêm path vào `gateway.security.public-paths`
- Đảm bảo path được route tới service đúng
- Không cần thay đổi JWT check

### Nếu API là internal
- Không thêm vào public-paths
- Filter sẽ tự yêu cầu token
- Service dưới có thể lấy `X-User-Id` / `X-User-Role`

### Nếu route liên quan tới quản trị hoặc thao tác nhạy cảm
- Phải yêu cầu token
- Nên kiểm tra role ở downstream service hoặc tại gateway nếu có rule riêng

---

## 10. Rule khuyến nghị cho từng nhóm API

### 10.1. Đăng nhập / Đăng ký
- Public
- Không cần token
- Route trực tiếp vào `identity`

### 10.2. Danh sách phim / dữ liệu public
- Public
- Không cần token
- Route trực tiếp vào service đọc dữ liệu

### 10.3. Tạo project / tạo event / đặt vé / các thao tác có thay đổi dữ liệu
- Internal
- Bắt buộc token
- Gateway xác thực trước khi forward

### 10.4. Duyệt, publish, approve, admin actions
- Internal
- Bắt buộc token
- Nên kiểm tra role `ADMIN` ở downstream hoặc tại gateway nếu muốn chặn sớm

---

## 11. Config keys quan trọng

### `jwt.secret`
- Secret dùng để verify chữ ký JWT
- Phải giống với service phát hành token (`identity`)

### `gateway.security.public-paths`
- Danh sách path public, phân tách bằng dấu phẩy
- Hỗ trợ wildcard Ant-style, ví dụ:
  - `/api/movies/**`
  - `/swagger-ui/**`

### `spring.cloud.gateway.server.webflux.discovery.locator.enabled`
- Bật discovery-based routing qua Eureka

### `spring.cloud.gateway.server.webflux.discovery.locator.lower-case-service-id`
- Chuyển service id sang lower-case khi resolve qua Eureka

---

## 12. Ví dụ request

### 12.1. Public request
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "12345678"
}
```

Gateway:
- cho qua ngay
- không kiểm token

---

### 12.2. Internal request
```http
POST /api/events/create
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "organizerId": 1,
  "title": "My Event"
}
```

Gateway:
- kiểm JWT
- nếu hợp lệ -> thêm `X-User-Id`, `X-User-Role`
- sau đó forward xuống service dưới

---

### 12.3. Internal request thiếu token
```http
POST /api/events/create
Content-Type: application/json
```

Gateway:
- trả `401 Unauthorized`
- không forward xuống service dưới

---

## 13. Checklist khi mở rộng hệ thống
Khi thêm service hoặc API mới, kiểm tra:

- [ ] Route đã có trong gateway chưa?
- [ ] API đó public hay internal?
- [ ] Nếu public, đã thêm vào `gateway.security.public-paths` chưa?
- [ ] Service đích đã sẵn sàng nhận `X-User-Id` và `X-User-Role` chưa?
- [ ] JWT secret có đồng bộ giữa `identity` và `api-gateway` chưa?
- [ ] Eureka service name có khớp với `lb://...` không?

---

## 14. Ghi chú triển khai hiện tại
- `JwtGlobalAuthenticationFilter` là filter trung tâm đang hoạt động.
- `AuthenticationFilter` là filter cũ, không nên dùng làm nguồn rule chính.
- Gateway hiện đang đi theo mô hình:
  - public -> pass-through
  - internal -> JWT required

---

## 15. Kết luận
Rule của `api-gateway` hiện tại là:

1. Public path -> đi thẳng
2. Internal path -> bóc token, kiểm tra JWT
3. Token sai/hết hạn -> `401`
4. Token đúng -> gắn header user và route xuống service con

Đây là rule trung tâm để bảo vệ toàn bộ hệ thống microservices.

