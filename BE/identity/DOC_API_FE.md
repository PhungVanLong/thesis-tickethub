## Identity API (FE)

Tài liệu này mô tả chi tiết các API của **Identity Service** (Dịch vụ định danh và quản lý người dùng) dành cho phát triển Frontend (FE).

---

## 1. Thông tin chung (General Information)

- **Base URL**: `http://localhost:8080` (Cổng API Gateway tập trung)
- **Cơ chế xác thực**: Sử dụng **JSON Web Token (JWT)**.
- **Header xác thực**: Đối với các API yêu cầu bảo mật, Frontend cần truyền token trong Header:
  ```http
  Authorization: Bearer <accessToken>
  ```
- **Quyền hạn người dùng (Roles)**: Hệ thống định nghĩa các vai trò trong [UserRole.java](file:///d:/thesis/BE/identity/src/main/java/ict/thesis/identity/entity/enums/UserRole.java) bao gồm:
  - `CUSTOMER` (Khách hàng - mặc định khi đăng ký mới)
  - `ORGANIZER` (Ban tổ chức sự kiện)
  - `STAFF` (Nhân viên hệ thống)
  - `ADMIN` (Quản trị viên)

---

## 2. Định dạng lỗi (Error Response Format)

Khi xảy ra lỗi, API sẽ trả về mã trạng thái HTTP thích hợp cùng với một JSON Object có cấu trúc chi tiết được định nghĩa trong [ApiErrorResponse.java](file:///d:/thesis/BE/identity/src/main/java/ict/thesis/identity/exception/ApiErrorResponse.java):

```json
{
  "code": "Tên_mã_lỗi",
  "message": "Thông điệp mô tả lỗi bằng tiếng Anh hoặc tiếng Việt",
  "details": {
    "fields": {
      "tên_trường_bị_lỗi": "Mô tả chi tiết lỗi của trường đó"
    }
  },
  "timestamp": "2026-07-08T03:00:00Z",
  "path": "/api/auth/register"
}
```

### Các mã lỗi phổ biến:
- `VALIDATION_ERROR` (HTTP 400): Dữ liệu đầu vào không hợp lệ hoặc thiếu các trường bắt buộc.
- `UNAUTHORIZED` (HTTP 401): Thông tin đăng nhập không chính xác hoặc Token JWT hết hạn/không hợp lệ.
- `NOT_FOUND` (HTTP 404): Tài nguyên hoặc thực thể yêu cầu không tồn tại.
- `CONFLICT` (HTTP 409): Dữ liệu bị trùng lặp (ví dụ: đăng ký hoặc cập nhật email đã tồn tại).
- `INTERNAL_ERROR` (HTTP 500): Lỗi hệ thống ngoài dự kiến.

---

## 3. Chi tiết các Endpoint API (Endpoint Details)

### 3.1. Đăng ký tài khoản (Register)

Đăng ký tài khoản khách hàng (`CUSTOMER`) mới vào hệ thống.

- **URL**: `/api/auth/register`
- **Method**: `POST`
- **Xác thực**: Không yêu cầu (Public)
- **Request Headers**:
  - `Content-Type: application/json`

- **Request Body** (Chi tiết tại [RegisterRequest.java](file:///d:/thesis/BE/identity/src/main/java/ict/thesis/identity/dto/RegisterRequest.java)):

| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc | Mô tả |
| :--- | :--- | :---: | :--- | :--- |
| `email` | String | Có | Định dạng email hợp lệ | Email đăng ký và cũng là tên đăng nhập |
| `password` | String | Có | Độ dài từ 8 đến 64 ký tự | Mật khẩu tài khoản |
| `fullName` | String | Có | Độ dài từ 2 đến 100 ký tự | Họ và tên đầy đủ |
| `phone` | String | Không | Không | Số điện thoại liên hệ |

*Ví dụ Request Body:*
```json
{
  "email": "customer@gmail.com",
  "password": "password123",
  "fullName": "Nguyễn Văn A",
  "phone": "0987654321"
}
```

- **Response thành công (HTTP 200 OK)** (Chi tiết tại [AuthResponse.java](file:///d:/thesis/BE/identity/src/main/java/ict/thesis/identity/dto/AuthResponse.java)):

| Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `accessToken` | String | Token JWT dùng để xác thực cho các yêu cầu tiếp theo |
| `tokenType` | String | Định dạng token (luôn trả về `"Bearer"`) |
| `expiresInSeconds` | Long | Thời gian hiệu lực của token (tính bằng giây) |

*Ví dụ Response 200:*
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresInSeconds": 604800
}
```

- **Các trường hợp lỗi**:
  - **409 Conflict**: Email đã được sử dụng.
    ```json
    {
      "code": "CONFLICT",
      "message": "Email already exists",
      "details": {},
      "timestamp": "2026-07-08T03:05:00Z",
      "path": "/api/auth/register"
    }
    ```
  - **400 Bad Request**: Dữ liệu không hợp lệ (ví dụ mật khẩu quá ngắn).
    ```json
    {
      "code": "VALIDATION_ERROR",
      "message": "Validation failed",
      "details": {
        "fields": {
          "password": "size must be between 8 and 64"
        }
      },
      "timestamp": "2026-07-08T03:06:00Z",
      "path": "/api/auth/register"
    }
    ```

---

### 3.2. Đăng nhập (Login)

Xác thực thông tin tài khoản và trả về token JWT.

- **URL**: `/api/auth/login`
- **Method**: `POST`
- **Xác thực**: Không yêu cầu (Public)
- **Request Headers**:
  - `Content-Type: application/json`

- **Request Body** (Chi tiết tại [LoginRequest.java](file:///d:/thesis/BE/identity/src/main/java/ict/thesis/identity/dto/LoginRequest.java)):

| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc | Mô tả |
| :--- | :--- | :---: | :--- | :--- |
| `email` | String | Có | Định dạng email | Email đăng nhập |
| `password` | String | Có | Không trống | Mật khẩu |

*Ví dụ Request Body:*
```json
{
  "email": "customer@gmail.com",
  "password": "password123"
}
```

- **Response thành công (HTTP 200 OK)** (Chi tiết tại [AuthResponse.java](file:///d:/thesis/BE/identity/src/main/java/ict/thesis/identity/dto/AuthResponse.java)):
  *Cấu trúc tương tự API Đăng ký.*
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresInSeconds": 604800
  }
  ```

- **Các trường hợp lỗi**:
  - **401 Unauthorized**: Email hoặc mật khẩu không khớp.
    ```json
    {
      "code": "UNAUTHORIZED",
      "message": "Invalid credentials",
      "details": {},
      "timestamp": "2026-07-08T03:10:00Z",
      "path": "/api/auth/login"
    }
    ```

---

### 3.3. Yêu cầu khôi phục mật khẩu (Forgot Password)

Tạo mã OTP khôi phục mật khẩu và gửi về email đã đăng ký.

- **URL**: `/api/auth/forgot-password`
- **Method**: `POST`
- **Xác thực**: Không yêu cầu (Public)
- **Request Headers**:
  - `Content-Type: application/json`

- **Request Body** (Chi tiết tại [ForgotPasswordRequest.java](file:///d:/thesis/BE/identity/src/main/java/ict/thesis/identity/dto/ForgotPasswordRequest.java)):

| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc | Mô tả |
| :--- | :--- | :---: | :--- | :--- |
| `email` | String | Có | Định dạng email | Email cần khôi phục mật khẩu |

*Ví dụ Request Body:*
```json
{
  "email": "customer@gmail.com"
}
```

- **Response thành công (HTTP 200 OK)**:
  - Trả về text đơn giản:
    ```text
    Email reset password will be sent (TODO).
    ```
  > [!NOTE]
  > Hệ thống hiện tại đang trong quá trình thử nghiệm (Mock) và sẽ ghi nhận mã OTP gồm 6 chữ số vào console log của Server với hiệu lực trong vòng 15 phút.

- **Các trường hợp lỗi**:
  - **404 Not Found**: Email không tồn tại trong hệ thống.
    ```json
    {
      "code": "NOT_FOUND",
      "message": "Email không tồn tại trong hệ thống",
      "details": {},
      "timestamp": "2026-07-08T03:12:00Z",
      "path": "/api/auth/forgot-password"
    }
    ```

---

### 3.4. Đặt lại mật khẩu (Reset Password)

Thiết lập mật khẩu mới sau khi xác thực OTP thành công.

- **URL**: `/api/auth/reset-password`
- **Method**: `POST`
- **Xác thực**: Không yêu cầu (Public)
- **Request Headers**:
  - `Content-Type: application/json`

- **Request Body** (Chi tiết tại [ResetPasswordRequest.java](file:///d:/thesis/BE/identity/src/main/java/ict/thesis/identity/dto/ResetPasswordRequest.java)):

| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc | Mô tả |
| :--- | :--- | :---: | :--- | :--- |
| `token` | String | Có | Không trống | Mã OTP hoặc Reset Token nhận được qua email |
| `newPassword` | String | Có | Không trống | Mật khẩu mới cần đổi |

*Ví dụ Request Body:*
```json
{
  "token": "123456",
  "newPassword": "newSecurePassword123"
}
```

- **Response thành công (HTTP 200 OK)**:
  ```text
  Password reset successfully (TODO).
  ```

- **Lưu ý đặc biệt**:
  > [!WARNING]
  > API này hiện tại đang trong quá trình phát triển (chưa hoàn thiện logic đổi pass). Nếu gọi sẽ ném ra lỗi `500 Internal Server Error` (lỗi hệ thống do `UnsupportedOperationException`).

---

### 3.5. Lấy thông tin người dùng (Get User Profile)

Lấy thông tin cá nhân của một người dùng theo ID.

- **URL**: `/api/users/{id}`
- **Method**: `GET`
- **Xác thực**: **Yêu cầu JWT** (Phải đính kèm Header `Authorization`)
- **Path Parameters**:
  - `id` (Long, bắt buộc): ID của người dùng cần tra cứu.

- **Response thành công (HTTP 200 OK)** (Chi tiết tại [UserResponse.java](file:///d:/thesis/BE/identity/src/main/java/ict/thesis/identity/dto/UserResponse.java)):

| Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `id` | Long | ID định danh duy nhất của người dùng |
| `email` | String | Địa chỉ email của tài khoản |
| `fullName` | String | Họ và tên đầy đủ |
| `role` | String | Vai trò hiện tại của tài khoản (`CUSTOMER`, `ORGANIZER`, `STAFF`, `ADMIN`) |
| `verified` | Boolean | Trạng thái xác thực email (`true` hoặc `false`) |
| `active` | Boolean | Trạng thái hoạt động của tài khoản (`true` hoặc `false`) |
| `createdAt` | String (ISO-8601) | Thời điểm tạo tài khoản (Ví dụ: `2026-07-08T03:00:50Z`) |
| `updatedAt` | String (ISO-8601) | Thời điểm cập nhật tài khoản gần nhất |

*Ví dụ Response 200:*
```json
{
  "id": 1,
  "email": "customer@gmail.com",
  "fullName": "Nguyễn Văn A",
  "role": "CUSTOMER",
  "verified": false,
  "active": true,
  "createdAt": "2026-07-08T03:00:50Z",
  "updatedAt": "2026-07-08T03:15:20Z"
}
```

- **Các trường hợp lỗi**:
  - **401 Unauthorized**: Không đính kèm Header `Authorization` hoặc token không hợp lệ.
  - **404 Not Found**: Không tồn tại người dùng tương ứng với ID đã truyền.
    ```json
    {
      "code": "NOT_FOUND",
      "message": "User not found",
      "details": {},
      "timestamp": "2026-07-08T03:20:00Z",
      "path": "/api/users/999"
    }
    ```

---

### 3.6. Cập nhật thông tin người dùng (Update User Profile)

Cập nhật thông tin chi tiết của người dùng.

- **URL**: `/api/users/{id}`
- **Method**: `PUT`
- **Xác thực**: **Yêu cầu JWT** (Phải đính kèm Header `Authorization`)
- **Path Parameters**:
  - `id` (Long, bắt buộc): ID của người dùng cần cập nhật.
- **Request Headers**:
  - `Content-Type: application/json`

- **Request Body** (Chi tiết tại [UpdateUserRequest.java](file:///d:/thesis/BE/identity/src/main/java/ict/thesis/identity/dto/UpdateUserRequest.java)):
  *Tất cả các trường trong body đều là tùy chọn (optional). Frontend chỉ gửi những trường cần cập nhật.*

| Trường | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `email` | String | Định dạng email hợp lệ | Cập nhật địa chỉ email mới (Kiểm tra duy nhất trùng lặp) |
| `fullName` | String | Không trống | Họ và tên đầy đủ mới |
| `phone` | String | Không trống | Số điện thoại mới |
| `avatarUrl` | String | Không trống | Đường dẫn ảnh đại diện mới |
| `role` | String | Phải khớp với các vai trò được hỗ trợ | Cập nhật quyền hạn mới |
| `verified` | Boolean | `true` / `false` | Xác thực email |
| `active` | Boolean | `true` / `false` | Trạng thái kích hoạt tài khoản |

*Ví dụ Request Body:*
```json
{
  "fullName": "Nguyễn Văn B",
  "phone": "0911222333",
  "avatarUrl": "https://example.com/avatar.jpg"
}
```

- **Response thành công (HTTP 200 OK)** (Chi tiết tại [UserResponse.java](file:///d:/thesis/BE/identity/src/main/java/ict/thesis/identity/dto/UserResponse.java)):
  *Trả về thông tin người dùng sau khi đã cập nhật thành công (giống response của API Get).*
  ```json
  {
    "id": 1,
    "email": "customer@gmail.com",
    "fullName": "Nguyễn Văn B",
    "role": "CUSTOMER",
    "verified": false,
    "active": true,
    "createdAt": "2026-07-08T03:00:50Z",
    "updatedAt": "2026-07-08T03:30:00Z"
  }
  ```

- **Các trường hợp lỗi**:
  - **401 Unauthorized**: Không đính kèm Header `Authorization` hoặc token không hợp lệ.
  - **404 Not Found**: Không tồn tại người dùng tương ứng với ID đã truyền.
  - **409 Conflict**: Email cập nhật đã bị trùng với một tài khoản khác trong hệ thống.
  - **400 Bad Request**: Role truyền vào không thuộc danh sách hỗ trợ hoặc quên truyền ID trên đường dẫn (nếu gọi `PUT /api/users` thay vì `PUT /api/users/{id}`).
    ```json
    {
      "code": "BAD_REQUEST",
      "message": "Missing id in path. Use /api/users/{id}",
      "details": {},
      "timestamp": "2026-07-08T03:35:00Z",
      "path": "/api/users"
    }
    ```

