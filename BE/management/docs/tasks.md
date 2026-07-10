# Checklist Nhiệm vụ: Management Service (`management`)

Tài liệu này theo dõi các đầu việc tại Management Service để cấu hình bảo mật nội bộ, đọc User Context và kết nối Kafka để cập nhật số lượng vé khả dụng khi thanh toán thành công.

---

## 📌 1. Cấu hình Properties & Dependencies (`pom.xml` & `application.properties`)
- [ ] Thêm dependency Spring Kafka vào file `pom.xml`:
  ```xml
  <dependency>
      <groupId>org.springframework.kafka</groupId>
      <artifactId>spring-kafka</artifactId>
  </dependency>
  ```
- [ ] Thêm các cấu hình kết nối Kafka và Shared Secret vào `application.properties`:
  ```properties
  gateway.shared-secret=gateway-secret-signature-2026
  
  # Cấu hình Kafka Consumer
  spring.kafka.bootstrap-servers=localhost:9092
  spring.kafka.consumer.group-id=management-group
  spring.kafka.consumer.auto-offset-reset=earliest
  spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
  spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer
  ```

---

## 📌 2. Tạo Class User Context & Filter
- [ ] Tạo gói `ict.thesis.management.security` và viết class `UserContext.java` để chứa thông tin người dùng.
- [ ] Viết class `UserContextHolder.java` chứa luồng `ThreadLocal<UserContext>` cùng phương thức `clearContext()`.
- [ ] Viết class `UserContextFilter.java` kế thừa `OncePerRequestFilter`:
  - Kiểm tra request header `X-Gateway-Token`. Nếu không khớp với `gateway.shared-secret`, trả về ngay lập tức mã `403 Forbidden` kèm thông báo "Direct access is prohibited!".
  - Trích xuất các header `X-User-Id`, `X-User-Role`, `X-User-Email`, `X-User-Permissions` và nạp vào `UserContextHolder`.
  - Luôn luôn gọi `UserContextHolder.clearContext()` trong khối `finally`.

---

## 📌 3. Cấu hình Spring Security & JPA Auditing
- [ ] Cập nhật class [SecurityConfig.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/security/SecurityConfig.java) để đưa `UserContextFilter` vào trước `UsernamePasswordAuthenticationFilter`.
- [ ] Viết cấu hình `AuditConfig.java` triển khai `AuditorAware<String>` lấy `userId` từ `UserContextHolder` để phục vụ tự động gán `@CreatedBy` và `@LastModifiedBy`.

---

## 📌 4. Lập lịch Lắng nghe Tin nhắn Kafka (Kafka Consumer)
- [ ] Viết class `OrderPaidKafkaListener.java` lắng nghe topic `order-paid-topic`:
  ```java
  @KafkaListener(topics = "order-paid-topic", groupId = "management-group")
  public void handleOrderPaidEvent(String message) {
      // 1. Parse JSON message lấy ticketTierId và quantity
      // 2. Tìm kiếm TicketTier tương ứng trong DB
      // 3. Giảm available_count của hạng vé: available_count = available_count - quantity
      // 4. Lưu lại vào DB
  }
  ```
- [ ] Viết class `OrderCancelledKafkaListener.java` lắng nghe topic `order-cancelled-topic`:
  - Khi nhận sự kiện, tiến hành hoàn trả số lượng vé trống: `available_count = available_count + quantity`.
