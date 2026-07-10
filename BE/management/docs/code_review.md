# BÁO CÁO ĐÁNH GIÁ MÃ NGUỒN (CODE REVIEW REPORT)

**Tác vụ**: Đánh giá và Phản biện kỹ thuật luồng Đăng ký & Phê duyệt Tổ chức (dịch vụ `management`).

---

## 1. Danh Sách Các File Mã Nguồn Đã Đánh Giá
1. [OrganizationController.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/controller/OrganizationController.java) (Tầng giao tiếp API)
2. [OrganizationService.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/service/OrganizationService.java) (Tầng logic nghiệp vụ)
3. [OrganizationRepository.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/repository/OrganizationRepository.java) (Tầng truy cập dữ liệu)
4. [GlobalExceptionHandler.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/controller/GlobalExceptionHandler.java) (Tầng xử lý ngoại lệ tập trung)
5. [SecurityConfig.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/security/SecurityConfig.java) (Cấu hình phân quyền an ninh)
6. [UserContextFilter.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/security/UserContextFilter.java) (Bộ lọc định danh và an ninh Gateway)
7. [OutboxPublisherScheduler.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/scheduler/OutboxPublisherScheduler.java) (Bộ quét gửi sự kiện bất đồng bộ)
8. [UserRolePromotedFailedConsumer.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/kafka/UserRolePromotedFailedConsumer.java) (Tiêu thụ sự kiện lỗi nâng quyền và rollback)

---

## 2. Chi Tiết Đánh Giá Từng Thành Phần (Detailed Review)

### 2.1. Tầng Giao Tiếp & Kiểm Soát Lỗi
- **[OrganizationController.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/controller/OrganizationController.java)**:
  - *Ưu điểm*: Sử dụng chuẩn RESTful API (`POST` cho tạo mới và phê duyệt). Định nghĩa rõ ràng các DTO đầu vào.
  - *Validation*: Khai báo `@Valid` trước các `@RequestBody` DTO để kích hoạt JSR-303 Validation Engine trước khi đi vào logic.
  - *Trích xuất định danh*: Sử dụng `@RequestHeader("X-User-Id")` để nhận dạng người dùng được chuyển tiếp từ API Gateway một cách rõ ràng.

- **[GlobalExceptionHandler.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/controller/GlobalExceptionHandler.java)**:
  - *Kiến trúc*: Áp dụng `@RestControllerAdvice` để cô lập xử lý lỗi ra khỏi luồng nghiệp vụ chính (Clean Code).
  - *Độ phủ*: Đầy đủ các bộ lọc lỗi cần thiết:
    - Bắt lỗi dữ liệu nhập liệu không hợp lệ (`BindException`, `MethodArgumentNotValidException`, `ConstraintViolationException`, `HandlerMethodValidationException`).
    - Bắt lỗi xung đột DB (`DataIntegrityViolationException`) để chuyển đổi mã lỗi thành `409 Conflict`.
    - Bắt lỗi logic nghiệp vụ chủ động (`ResponseStatusException`).
  - *Chuẩn hóa*: Tất cả các lỗi đều trả về cấu trúc thống nhất `{"error": "nội dung lỗi"}` giúp Frontend dễ đọc và hiển thị giao diện.

### 2.2. Tầng Nghiệp Vụ & Cơ Sở Dữ Liệu
- **[OrganizationService.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/service/OrganizationService.java)**:
  - *Quản lý giao dịch*: Đánh dấu `@Transactional` ở mức phương thức cho các thao tác thay đổi dữ liệu đảm bảo tính toàn vẹn (ACID) của giao dịch cục bộ PostgreSQL.
  - *Tránh lỗi sớm (Fail-Fast)*: Kiểm tra dữ liệu đầu vào và kiểm tra trùng lặp mã số thuế (`existsByTaxCode`) trước khi thực hiện ghi xuống CSDL. Việc này tối ưu hiệu năng CSDL, tránh được các lệnh rollback đắt đỏ của DB.
  - *Máy trạng thái (State Machine)*: Kiểm soát trạng thái của hồ sơ rất chặt chẽ, ngăn chặn các thao tác chuyển trạng thái bất hợp lệ ở tầng backend.
  - *Mối quan hệ nguyên tố*: Khi tạo tổ chức, đồng thời tạo luôn thành viên `OWNER` trong cùng một Transaction giúp tránh hiện tượng "mồ côi" tổ chức không có người quản lý.

- **[OrganizationRepository.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/repository/OrganizationRepository.java)**:
  - *Ưu điểm*: Định nghĩa phương thức dạng Spring Data JPA Query Method (`existsByTaxCode`) giúp Spring tự động sinh câu lệnh SQL tối ưu (`SELECT EXISTS...`), chỉ kiểm tra sự tồn tại của bản ghi mà không cần nạp toàn bộ thực thể lên bộ nhớ.

### 2.3. Tầng Bảo Mật & Xác Thực Ngăn Chặn Bypass
- **[UserContextFilter.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/security/UserContextFilter.java)** & **[SecurityConfig.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/security/SecurityConfig.java)**:
  - *An ninh Gateway*: Sử dụng cơ chế chia sẻ khóa bí mật `X-Gateway-Token` để ngăn chặn việc bypass API Gateway (truy cập trực tiếp dịch vụ nội bộ từ internet).
  - *Ngăn rò rỉ bộ nhớ (Memory Leak Protection)*: Triển khai dọn dẹp ngữ cảnh bảo mật `UserContextHolder.clearContext()` trong khối lệnh `finally`. Điều này cực kỳ quan trọng trong môi trường máy chủ chạy đa luồng (Thread Pool) để tránh rò rỉ dữ liệu phiên làm việc từ luồng này sang luồng khác.
  - *Sửa lỗi endpoint /error*: Cho phép công khai đường dẫn `/error` giúp bảo toàn đúng mã lỗi hệ thống thay vì bị Spring Security mặc định chặn và trả về lỗi giả tạo `403 Forbidden`.

### 2.4. Đảm Bảo Nhất Quán Dữ Liệu Phân Tán
- **[OutboxPublisherScheduler.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/scheduler/OutboxPublisherScheduler.java)**:
  - *Độ tin cậy*: Triển khai mô hình lưu bảng Outbox nội bộ cùng với cập nhật trạng thái tổ chức. Điều này loại bỏ hoàn toàn rủi ro mất gói tin khi hệ thống bị sập ngay sau khi cập nhật dữ liệu mà chưa kịp gửi sang hàng đợi Kafka.
  - *Cập nhật trạng thái sự kiện*: Chỉ cập nhật sự kiện Outbox thành `PUBLISHED` sau khi nhận được xác nhận thành công từ Kafka Broker (At-least-once Delivery).

- **[UserRolePromotedFailedConsumer.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/kafka/UserRolePromotedFailedConsumer.java)**:
  - *Saga Compensating*: Thực thi chính xác logic giao dịch bù khi nhận được sự kiện lỗi từ `identity-service`, khôi phục lại trạng thái tổ chức cũ giúp hệ thống đạt trạng thái nhất quán cuối cùng (Eventual Consistency).

---

## 3. Đánh Giá Điểm Mạnh & Đóng Góp Kỹ Thuật (Strengths)
1. **Tuân thủ nguyên lý SOLID**:
   - *Single Responsibility Principle (SRP)*: Tách biệt rõ ràng tầng Controller (giao tiếp), Service (nghiệp vụ), Scheduler (đẩy tin nhắn) và Consumer (tiêu thụ tin nhắn).
2. **Khả năng Chịu lỗi cao (Fault Tolerance)**: Thiết kế bất đồng bộ qua Outbox giúp hệ thống vẫn ghi nhận phê duyệt tổ chức bình thường kể cả khi Identity Service hoặc Kafka Broker đang tạm dừng hoạt động. Sự kiện sẽ tự động được gửi bù khi các hệ thống này trực tuyến trở lại.
3. **Tách biệt kiểm soát logic nghiệp vụ và bảo mật**: Gateway lo phần xác thực thô (Token JWT), dịch vụ Management lo phần kiểm tra quyền nghiệp vụ (đọc từ headers).

---

## 4. Các Khuyến Nghị Cải Tiến (Recommendations)

Nhằm làm tăng tính hoàn thiện của mã nguồn cho đồ án bảo vệ, chúng tôi đưa ra các khuyến nghị cải tiến sau:

1. **Email Sending Bất đồng bộ (Asynchronous Email)**:
   - *Hiện trạng*: Trong `UserRolePromotedSuccessConsumer`, khi nâng quyền thành công, email thông báo được gửi trực tiếp.
   - *Khuyến nghị*: Quá trình kết nối đến máy chủ SMTP để gửi mail thường tốn thời gian và dễ lỗi. Nên cấu hình gửi email bất đồng bộ (sử dụng `@Async` của Spring hoặc đẩy tác vụ gửi mail vào một hàng đợi/chủ đề Kafka riêng) để tránh chặn luồng tiêu thụ tin nhắn Kafka (Kafka Consumer Thread).
2. **Quản lý Retry Count trong Outbox Scheduler**:
   - *Hiện trạng*: `OutboxPublisherScheduler` quét các sự kiện `PENDING` và đẩy lên Kafka.
   - *Khuyến nghị*: Nếu Kafka gặp sự cố dài hạn, vòng lặp quét liên tục sẽ cố gắng gửi lại các sự kiện lỗi. Cần giới hạn số lần thử lại tối đa (ví dụ: `retry_count < 5`). Nếu vượt quá số lần này, chuyển sự kiện vào trạng thái `FAILED` hoặc đưa vào hàng đợi lỗi (Dead Letter Queue - DLQ) để quản trị viên xử lý thủ công, tránh làm nghẽn Scheduler.
3. **Mã hóa Payload nhạy cảm trong bảng Outbox**:
   - *Hiện trạng*: Payload được lưu dưới dạng chuỗi JSON thô trong DB (`"{\"organizationId\":" + ... }"`).
   - *Khuyến nghị*: Đối với các dữ liệu nhạy cảm của tổ chức hoặc người dùng, nên tiến hành mã hóa thông tin payload trước khi ghi xuống bảng Outbox để đảm bảo an toàn an ninh dữ liệu.
