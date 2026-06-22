# Checklist Nhiệm vụ: Promotion Service (`promotion`)

Tài liệu này theo dõi các đầu việc tại Promotion Service để cấu hình bảo mật nội bộ, đọc User Context và cập nhật số lượt sử dụng voucher khi đơn hàng được thanh toán thành công.

---

## 📌 1. Cấu hình Properties & Dependencies (`pom.xml` & `application.properties`)
- [ ] Thêm dependency Spring Kafka vào file `pom.xml`.
- [ ] Thêm các cấu hình kết nối Kafka và Shared Secret vào `application.properties`:
  ```properties
  gateway.shared-secret=gateway-secret-signature-2026
  
  # Cấu hình Kafka Consumer
  spring.kafka.bootstrap-servers=localhost:9092
  spring.kafka.consumer.group-id=promotion-group
  spring.kafka.consumer.auto-offset-reset=earliest
  spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
  spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer
  ```

---

## 📌 2. Tạo Class User Context & Filter
- [ ] Tạo gói `ict.thesis.promotion.security` và viết class `UserContext.java`.
- [ ] Viết class `UserContextHolder.java` chứa luồng `ThreadLocal<UserContext>`.
- [ ] Viết class `UserContextFilter.java` kế thừa `OncePerRequestFilter` để kiểm tra request header `X-Gateway-Token` khớp với `gateway.shared-secret` và trích xuất headers nạp vào `UserContextHolder`.

---

## 📌 3. Lắng nghe Tin nhắn Kafka để cập nhật lượt dùng Voucher
- [ ] Viết class `OrderPaidKafkaListener.java` lắng nghe topic `order-paid-topic`:
  - Kiểm tra xem payload có chứa thông tin `voucherCode` hay không.
  - Nếu có sử dụng voucher, truy vấn thông tin Voucher trong database theo `code`.
  - Thực hiện tăng số lượt sử dụng: `used_count = used_count + 1`.
  - Lưu lại vào DB.
- [ ] Viết class `OrderCancelledKafkaListener.java` lắng nghe topic `order-cancelled-topic`:
  - Nếu hóa đơn bị hủy do quá hạn thanh toán và trước đó đã áp dụng voucher, thực hiện hoàn trả lượt dùng: `used_count = used_count - 1`.
- [ ] Viết API `/api/vouchers/apply` để Booking Service gọi chéo sang kiểm tra tính hợp lệ và giảm giá của voucher trước khi người dùng đặt vé.
