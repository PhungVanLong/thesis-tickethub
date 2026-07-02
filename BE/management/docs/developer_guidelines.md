# HƯỚNG DẪN BẢO VỆ LUỒNG & PHÁT TRIỂN HỆ THỐNG DÀNH CHO AI (AI DEVELOPMENT GUIDELINES)

Tài liệu này được viết để các mô hình ngôn ngữ lớn (LLM/AI) khác khi tiếp quản dự án có thể nhanh chóng hiểu rõ cấu trúc luồng **Đăng ký và Phê duyệt Tổ chức** đã được hoàn thiện chính xác, đồng thời biết cách phát triển các luồng mới mà không phá vỡ hoặc làm thay đổi logic hiện tại.

---

## 1. Các Quy Tắc Bảo Vệ Luồng Hiện Tại (Protected Flow Rules)

Hồ sơ Đăng ký và Phê duyệt Tổ chức là luồng nghiệp vụ quan trọng liên kết giữa nhiều dịch vụ (`management-service` và `identity-service`) thông qua giao dịch phân tán. Khi phát triển các tính năng khác, các AI Model tuyệt đối **KHÔNG** được thay đổi các quy tắc sau:

### 1.1. Tầng An Ninh & Phân Quyền (Security Layer)
- **Luôn cho phép `/error` truy cập công khai**: Trong [SecurityConfig.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/security/SecurityConfig.java), đường dẫn `/error` bắt buộc phải nằm trong `.permitAll()`. Nếu chặn đường dẫn này, Spring Security 6 sẽ tự động chuyển đổi tất cả lỗi logic (`400`, `500`) thành `403 Forbidden` trên client.
- **Dọn dẹp Context ThreadLocal**: Trong [UserContextFilter.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/security/UserContextFilter.java), việc dọn dẹp ngữ cảnh lưu trữ `UserContextHolder.clearContext()` phải được nằm trong khối `finally` của bộ lọc. Tuyệt đối không di chuyển hay xóa bỏ khối `finally` này để tránh rò rỉ thông tin người dùng giữa các luồng xử lý của Web Server.

### 1.2. Tầng Logic Nghiệp Vụ & Dữ Liệu (Business & Data Layer)
- **Kiểm tra trùng lặp tại Service (Fail-Fast)**: Trước khi lưu tổ chức mới trong [OrganizationService.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/service/OrganizationService.java), bắt buộc phải gọi phương thức `existsByTaxCode` của Repository để kiểm tra trùng mã số thuế. Không được bỏ qua bước này và phụ thuộc hoàn toàn vào ngoại lệ duy nhất của cơ sở dữ liệu.
- **Ràng buộc Máy Trạng Thái (State Machine)**: Trạng thái tổ chức được quản lý nghiêm ngặt qua 4 bước: `PENDING` -> `ACTIVE` / `REJECTED` -> `BANNED`. Mọi bước chuyển trạng thái khác ngoài thiết kế này đều bị từ chối bằng lỗi `400 Bad Request`.
- **Sự kiện giao dịch cục bộ (Outbox)**: Việc cập nhật trạng thái tổ chức thành `ACTIVE` (hoặc `BANNED`) và lưu bản ghi `OutboxEvent` mới vào CSDL phải luôn được thực thi trong cùng một giao dịch `@Transactional` cục bộ để đảm bảo tính nguyên tố (Atomicity).

### 1.3. Tầng Xử Lý Lỗi (Exception Handling)
- **Xử lý tập trung**: Không tự ý thay đổi hoặc viết đè các phương thức xử lý lỗi trong [GlobalExceptionHandler.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/controller/GlobalExceptionHandler.java).
- Các ngoại lệ như `MethodArgumentNotValidException`, `BindException`, `ConstraintViolationException`, `HandlerMethodValidationException`, `ResponseStatusException`, và `DataIntegrityViolationException` đã được cấu hình định dạng JSON trả về dạng thống nhất `{"error": "message"}`. Khi viết Exception mới, hãy sử dụng lại cấu trúc này.

---

## 2. Hướng Dẫn Phát Triển Luồng Nghiệp Vụ Mới (How to Develop New Flows)

Khi được yêu cầu phát triển các luồng tính năng mới (ví dụ: đặt vé, tạo sự kiện, thanh toán), hãy áp dụng đúng các mẫu kiến trúc (Architectural Patterns) đã được thiết lập sẵn trong dự án:

### 2.1. Sử dụng Transactional Outbox Pattern cho các thao tác liên dịch vụ
Nếu tính năng mới yêu cầu cập nhật dữ liệu ở dịch vụ hiện tại và đồng bộ sang một dịch vụ khác (ví dụ: đặt vé thành công cần giảm số lượng ghế trống ở dịch vụ sự kiện):
1. **Lưu dữ liệu cục bộ & Ghi Outbox**: Lưu dữ liệu chính và lưu một bản ghi sự kiện vào bảng `outbox_event` với trạng thái `PENDING` trong cùng một `@Transactional` method.
2. **Sử dụng Scheduler có sẵn**: Bộ quét [OutboxPublisherScheduler.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/scheduler/OutboxPublisherScheduler.java) sẽ tự động quét và gửi sự kiện lên Kafka Broker. Bạn chỉ cần khai báo thêm `eventType` mới.

### 2.2. Áp dụng Saga Pattern để chịu lỗi phân tán
Với các hành động cần gọi sang dịch vụ khác:
1. Viết consumer nhận kết quả xử lý từ dịch vụ đích (Success/Failed).
2. Nếu nhận sự kiện thất bại (Failed), bắt buộc phải viết **Giao dịch bù (Compensating Transaction)** trong Consumer đó để rollback lại dữ liệu đã lưu ở bước đầu tiên về trạng thái trước khi thực hiện (như cách triển khai tại [UserRolePromotedFailedConsumer.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/kafka/UserRolePromotedFailedConsumer.java)).

### 2.3. Quy tắc phát triển mã nguồn & Giao tiếp
- **Ngôn ngữ viết Code**: Tất cả comment, thông báo ngoại lệ, log hệ thống trong code Java bắt buộc phải viết bằng **Tiếng Anh đơn giản (mức độ từ vựng dưới B1)**.
- **Ngôn ngữ Giao tiếp / Tài liệu**: Khi trao đổi với người dùng trong Chat, hoặc khi viết tài liệu kế hoạch/walkthrough/hướng dẫn, bắt buộc phải viết bằng **Tiếng Việt**.
