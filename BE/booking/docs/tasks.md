# Checklist Nhiệm vụ: Booking Service (`booking`)

Tài liệu này theo dõi toàn bộ đầu việc cần triển khai tại **Booking Service (cổng 8084)**. Đây là service trung tâm xử lý đặt vé bất đồng bộ, hóa đơn, tích hợp cổng thanh toán và gửi tin nhắn Kafka.

---

## 📌 1. Cấu hình Properties & Dependencies (`pom.xml` & `application.properties`)
- [ ] Thêm dependency cho Spring Kafka, WebClient (hoặc OpenFeign), và Jackson (xử lý JSON).
- [ ] Thiết lập kết nối cơ sở dữ liệu `booking` trên PostgreSQL local.
- [ ] Cấu hình Kafka Producer và Consumer trong `application.properties`:
  ```properties
  gateway.shared-secret=gateway-secret-signature-2026
  
  # Cấu hình Kafka Broker
  spring.kafka.bootstrap-servers=localhost:9092
  
  # Consumer cho hàng đợi đặt vé
  spring.kafka.consumer.group-id=booking-group
  spring.kafka.consumer.auto-offset-reset=earliest
  spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
  spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer
  
  # Producer
  spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
  spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer
  
  # Cấu hình Cổng thanh toán (Tổng quát)
  payment.gateway.pay-url=https://sandbox-gateway.com/pay
  payment.gateway.merchant-code=YOUR_MERCHANT_CODE
  payment.gateway.hash-secret=YOUR_HASH_SECRET
  payment.gateway.return-url=http://localhost:8080/api/payments/callback
  ```

---

## 📌 2. Tạo các Entity chính trong gói `enties`
- [ ] Định nghĩa `Order.java` (id, userId, eventId, totalAmount, status).
- [ ] Định nghĩa `OrderItem.java` (id, orderId, ticketTierId, quantity, unitPrice).
- [ ] Định nghĩa `Payment.java` (id, orderId, amount, gatewayName, gatewayTxId, status, idempotencyKey).
- [ ] Định nghĩa `Ticket.java` (id, orderId, ticketCode, ticketTierId, checkinStatus, checkinAt).

---

## 📌 3. Triển khai Cơ chế Đọc User Context & Bảo vệ Endpoint
- [ ] Viết `UserContext.java`, `UserContextHolder.java`, và `UserContextFilter.java` (kiểm tra header `X-Gateway-Token` khớp với `gateway.shared-secret`).
- [ ] Cấu hình `AuditConfig.java` cho JPA Auditing để tự điền `createdBy`.

---

## 📌 4. Triển khai Đặt vé Bất đồng bộ (Async Booking Flow)
- [ ] **Viết REST Controller cho Đặt vé:**
  - Nhận yêu cầu `POST /api/bookings` (eventId, ticketTierId, quantity).
  - Sinh UUID: `bookingRequestId`.
  - Gửi dữ liệu yêu cầu đặt vé vào Kafka topic `booking-requests-topic`.
  - Trả về ngay lập tức HTTP `202 Accepted` kèm `bookingRequestId`.
- [ ] **Viết API Polling trạng thái:**
  - Tạo Endpoint `GET /api/bookings/status/{bookingRequestId}`.
  - Đọc trạng thái xử lý đặt vé từ một Map hoặc Cache (ví dụ: `Map<String, BookingStatusDto>`).
- [ ] **Viết Kafka Consumer xử lý Đặt vé (FIFO):**
  - Lắng nghe `booking-requests-topic`.
  - Kiểm tra số lượng vé khả dụng của hạng vé đó trong DB (không khóa ghế trước, nhiều người có thể cùng đặt vé cùng một lúc nhưng việc xử lý FIFO sẽ quyết định ai mua thành công đến khi hết vé).
  - Nếu đủ vé (available_count >= quantity):
    - Tạo `Order` (trạng thái `PENDING_PAYMENT`).
    - Cập nhật trạng thái của `bookingRequestId` thành `SUCCESS` kèm `orderId`.
  - Nếu hết vé:
    - Cập nhật trạng thái thành `FAILED` với lý do "Hết vé".

---

## 📌 5. Tích hợp Cổng thanh toán (Momo/VNPay/Stripe...)
- [ ] **Viết API tạo Link thanh toán:**
  - Nhận `POST /api/bookings/{orderId}/pay`.
  - Tạo `idempotencyKey` và ghi nhận một giao dịch `Payment` ở trạng thái `PENDING`.
  - Sinh URL thanh toán tương ứng với cấu hình của cổng thanh toán được chọn.
- [ ] **Viết API Callback (Chuyển hướng người dùng):**
  - Nhận `GET /api/payments/callback` để nhận phản hồi kết quả từ trình duyệt người dùng và chuyển hướng người dùng về trang Frontend.
- [ ] **Viết API IPN Webhook (Cập nhật trạng thái ngầm):**
  - Nhận `POST /api/payments/ipn` (endpoint này để public).
  - Kiểm tra chữ ký checksum của cổng thanh toán gửi lên.
  - So khớp số tiền hóa đơn và cập nhật trạng thái `SUCCESS` hoặc `FAILED` cho `Payment` và `Order` trong DB.
  - Nếu thanh toán thành công, sinh bản ghi `Ticket` (QR Code UUID) và gửi tin nhắn đồng bộ lên Kafka topic `order-paid-topic`.

---

## 📌 6. Tác vụ Quét Đơn hàng Quá hạn (Scheduler)
- [ ] Viết một method có annotation `@Scheduled(cron = "0 * * * * *")` chạy mỗi 1 phút:
  - Truy vấn các Order có trạng thái `PENDING_PAYMENT` có thời gian tạo quá 10 phút.
  - Chuyển trạng thái Order thành `CANCELLED`.
  - Phát sự kiện hủy đặt vé lên Kafka topic `order-cancelled-topic` để giải phóng/cộng trả lại số vé trống khả dụng ở Management Service.
