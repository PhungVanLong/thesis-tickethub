# Hướng dẫn Tích hợp API Luồng Đặt Vé & Thanh toán (Dành cho Frontend)

Tài liệu này đặc tả chi tiết toàn bộ các API, cấu trúc Payload dữ liệu và cơ chế kết nối thời gian thực (SSE) mà Frontend (Angular) cần sử dụng để hoàn thiện luồng Đặt vé cạnh tranh (Pay-to-win).

---

## 1. Bản đồ các Endpoint & Luồng đi của dữ liệu

FE kết nối tới các dịch vụ thông qua API Gateway (mặc định chạy tại port `8080`).

```
1. Mở sơ đồ ghế:
   [FE] -- GET /api/events/{eventId}/seat-maps/stream (SSE) --> [Gateway] --> [Management Service]
   (Nhận event SEAT_UPDATE để đổi màu ghế: Cam = RESERVED, Đỏ = SOLD, Ẩn màu = AVAILABLE)

2. Bấm đặt vé:
   [FE] -- POST /api/bookings --> [Gateway] --> [Booking Service] (Trả về requestId tức thì)

3. Lắng nghe kết quả tạo hóa đơn:
   [FE] -- GET /api/bookings/stream/{requestId} (SSE) --> [Gateway] --> [Booking Service]
   (Nhận SUCCESS kèm orderId -> Redirect sang trang Checkout /checkout/{orderId})

4. Trang Checkout (Thanh toán / Thoát trang):
   - Thanh toán thành công: POST /api/bookings/{orderId}/mock-pay (Ghế chuyển sang SOLD)
   - Thoát trang Checkout:  POST /api/bookings/{orderId}/mock-cancel (Giải phóng ghế về AVAILABLE)
```

---

## 2. Chi tiết Đặc tả API

### 2.1. Đăng ký nhận cập nhật trạng thái Ghế thời gian thực (SSE)
Kết nối này được duy trì khi người dùng mở màn hình chọn ghế nhằm cập nhật liên tục màu sắc ghế.

*   **URL**: `http://localhost:8080/api/events/{eventId}/seat-maps/stream`
*   **Method**: `GET` (Header: `Accept: text/event-stream`)
*   **Các sự kiện nhận được từ BE**:
    *   **Sự kiện `INIT`**: Bắn ngay khi kết nối thành công.
        *   *Data*: `"Connected to event {eventId} seat map stream"`
    *   **Sự kiện `SEAT_UPDATE`**: Bắn khi trạng thái ghế của event thay đổi.
        *   *Data (JSON)*:
            ```json
            {
              "eventId": 7,
              "seatIds": [102, 103],
              "status": "RESERVED" // Hoặc "SOLD", "AVAILABLE", "DISABLED"
            }
            ```
            *   Trạng thái `RESERVED`: FE **đổi màu ghế thành màu Cam** (hoặc thêm hiệu ứng nhấp nháy), nhưng **VẪN CHO PHÉP click chọn** để cạnh tranh mua.
            *   Trạng thái `SOLD`: FE **đổi màu ghế thành Đỏ** và **KHOÁ hoàn toàn** (không cho click chọn nữa).
            *   Trạng thái `AVAILABLE`: Ghế được giải phóng, FE đổi màu về màu gốc của phân hạng vé tương ứng.

---

### 2.2. Gửi yêu cầu đặt vé (Submit Booking Request)
Gửi yêu cầu giữ ghế bất đồng bộ lên hàng đợi xử lý.

*   **URL**: `http://localhost:8080/api/bookings`
*   **Method**: `POST`
*   **Headers**: 
    *   `Content-Type: application/json`
    *   `Authorization: Bearer <JWT_TOKEN>`
*   **Request Payload**:
    ```json
    {
      "eventId": 7,
      "customerId": 1,
      "idempotencyKey": "bf05e59b-1d7d-41a4-9457-9d7a0be9f323", // UUID random sinh từ FE
      "items": [
        {
          "seatId": 102,       // ID ghế thực tế lấy từ dbSeat.id (Nên truyền để BE xử lý tối ưu)
          "ticketTierId": 12   // ID của hạng vé tương ứng
        }
      ]
    }
    ```
*   **Response (HTTP 200)**:
    ```json
    {
      "requestId": "550e8400-e29b-41d4-a716-446655440000"
    }
    ```

---

### 2.3. Lắng nghe tiến độ tạo Đơn hàng (SSE)
Sau khi nhận được `requestId`, FE ngay lập tức kết nối tới endpoint này để nhận thông tin xem đơn hàng đã được tạo thành công hay bị lỗi.

*   **URL**: `http://localhost:8080/api/bookings/stream/{requestId}`
*   **Method**: `GET` (Header: `Accept: text/event-stream`)
*   **Các sự kiện nhận được từ BE**:
    *   **Sự kiện `INIT`**: Bắn ngay khi kết nối thành công.
        *   *Data*: `"Connected"`
    *   **Sự kiện `SUCCESS`**: Hóa đơn đã được tạo xong ở trạng thái `PENDING`.
        *   *Data (String)*: `orderId` (Ví dụ: `25` -> FE dùng ID này redirect sang trang `/checkout/25`).
    *   **Sự kiện `FAILED`**: Đặt vé thất bại (Ví dụ: Trùng lịch, lỗi hệ thống).
        *   *Data (String)*: Lý do thất bại (Ví dụ: `"Ghế đã được bán hoặc khoá bởi quản trị viên."`).

---

### 2.4. Lấy chi tiết đơn hàng (Trang Checkout)
Lấy thông tin chi tiết của đơn hàng để hiển thị hóa đơn thanh toán.

*   **URL**: `http://localhost:8080/api/orders/{orderId}`
*   **Method**: `GET`
*   **Headers**: `Authorization: Bearer <JWT_TOKEN>`
*   **Response (HTTP 200)**:
    ```json
    {
      "orderId": 25,
      "orderCode": "ORD-A1B2C3D4",
      "subtotal": 500000,
      "totalAmount": 500000,
      "status": "PENDING",
      "eventTitle": "Đại Nhạc Hội Mùa Hè 2026",
      "eventDate": "2026-07-20T19:00:00Z",
      "eventVenue": "Sân vận động Mỹ Đình",
      "bannerUrl": "https://drive.google.com/...",
      "items": [
        {
          "ticketTierName": "VIP",
          "price": 500000,
          "quantity": 1,
          "seatLabel": "A-12"
        }
      ]
    }
    ```

---

### 2.5. Giả lập Thanh toán (Mock Payment)
Xử lý khi người dùng nhấn "Thanh toán ngay" thành công trên giao diện.

*   **URL**: `http://localhost:8080/api/bookings/{orderId}/mock-pay`
*   **Method**: `POST`
*   **Headers**: `Authorization: Bearer <JWT_TOKEN>`
*   **Response (HTTP 200)**:
    ```json
    {
      "message": "Payment simulated successfully"
    }
    ```

---

### 2.6. Giả lập Huỷ đơn thanh toán (Mock Cancel)
Được gọi khi người dùng chủ động nhấn hủy, hoặc khi **chuyển trang/rời khỏi trang Checkout** mà chưa thanh toán để giải phóng ghế ngay lập tức.

*   **URL**: `http://localhost:8080/api/bookings/{orderId}/mock-cancel`
*   **Method**: `POST`
*   **Headers**: `Authorization: Bearer <JWT_TOKEN>`
*   **Response (HTTP 200)**:
    ```json
    {
      "message": "Cancellation simulated successfully"
    }
    ```

---

## 3. Quy định xử lý Vòng đời ở Frontend

1. **Dọn dẹp SSE (OnDestroy)**: Trong các file Component (`seat-selection.ts`), luôn đóng kết nối của `EventSource` khi component bị hủy:
   ```typescript
   ngOnDestroy() {
     this.sse?.close();
     this.seatMapSse?.close();
   }
   ```
2. **Giải phóng ghế tự động**: Tại component Checkout, sử dụng cờ `paymentCompleted` và `ngOnDestroy` để tự động gọi API `/mock-cancel` giải phóng ghế nếu người dùng điều hướng ra trang khác mà chưa thực hiện thanh toán thành công.

---

## 4. Luồng xử lý chi tiết tại trang Checkout (Dedicated Checkout Page)

Trang Checkout (`/checkout/{orderId}`) chịu trách nhiệm thực hiện thanh toán giả lập hoặc huỷ đơn để giải phóng ghế:

1. **Khởi tạo (ngOnInit)**:
   - Lấy `orderId` từ Route params.
   - Gọi API `GET /api/orders/{orderId}` để lấy thông tin chi tiết đơn hàng (Tên sự kiện, Thời gian, Hạng vé, Số tiền, Nhãn ghế).
   
2. **Thanh toán (payOrder)**:
   - Khi bấm "Thanh toán", thực hiện `POST /api/bookings/{orderId}/mock-pay`.
   - Khi thành công, set cờ `paymentCompleted = true` để tránh trigger tự động huỷ đơn, sau đó hiển thị thông báo thành công và chuyển hướng về trang tài khoản cá nhân `/my-account?tab=tickets`.
   
3. **Huỷ đơn và Giải phóng ghế tự động (ngOnDestroy)**:
   - Sử dụng cờ `paymentCompleted` (mặc định là `false`).
   - Nếu Component bị hủy (`ngOnDestroy`) mà cờ `paymentCompleted` vẫn là `false` (người dùng thoát trang, click nút Trở về hoặc chuyển hướng đi nơi khác), tự động thực hiện gọi API `POST /api/bookings/{orderId}/mock-cancel` nhằm chuyển trạng thái đơn hàng thành `CANCELLED` và giải phóng ghế về trạng thái `AVAILABLE` ngay lập tức.
