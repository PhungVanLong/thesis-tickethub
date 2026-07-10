# Tiến độ thực hiện các Task - 2026-07-07

Hôm nay chúng ta đã hoàn thành các nhiệm vụ cốt lõi sau trên Backend:

## 1. Xác thực (Validation) thông tin vé sự kiện
- **File sửa đổi**: [TicketTierRequest.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/dto/request/TicketTierRequest.java)
- **Nội dung**:
  - Thêm kiểm tra giá vé phải $\ge 0$ (`@DecimalMin(value = "0.0")`).
  - Thêm kiểm tra số lượng vé tổng cộng phải $\ge 1$ (`@Min(value = 1)`).

## 2. Lưu trạng thái "PENDING" & Gửi thông báo cho Admin
- **File sửa đổi**:
  - [EventStatus.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/entity/enums/EventStatus.java): Đổi trạng thái `PENDING_APPROVAL` thành `PENDING` cho ngắn gọn, thống nhất.
  - [EventService.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/service/EventService.java): Đổi sang gán `EventStatus.PENDING` khi tạo sự kiện và chèn thêm `OutboxEvent` loại `"EVENT_PENDING"` chứa thông tin sự kiện để thông báo cho Admin.
  - [OutboxPublisherScheduler.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/scheduler/OutboxPublisherScheduler.java): Ánh xạ sự kiện `"EVENT_PENDING"` gửi đến Kafka topic `"event-pending-topic"`.

## 3. Cấu hình CORS để sửa lỗi 403 Forbidden ở Gateway
- **File sửa đổi**:
  - [SecurityConfig.java](file:///d:/thesis/BE/api-gateway/src/main/java/ict/thesis/api_gateway/security/SecurityConfig.java): Bật cấu hình CORS cho WebFlux Security, trỏ tới `CorsConfigurationSource` cho phép origin `http://localhost:4200` gọi API.

## 4. Bật log chi tiết cho API Gateway
- **File sửa đổi**:
  - [application.properties](file:///d:/thesis/BE/api-gateway/src/main/resources/application.properties): Bật log cấp độ `TRACE` và `DEBUG` cho routing của gateway và client Netty để dễ dàng debug.

## 5. Cập nhật Tài liệu API
- **File sửa đổi**:
  - [management-api.md](file:///d:/thesis/BE/management/management-api.md)
  - [fe_integration_guide.md](file:///d:/thesis/BE/management/docs/fe_integration_guide.md)
  - [api-gateway.md](file:///d:/thesis/BE/management/api-gateway.md)
- **Nội dung**: Đồng bộ lại các tham số (thay `organizerId` bằng `organizationId`, lược bỏ `adminUserId` khỏi body request của API duyệt vì đã lấy từ token, đổi `PENDING_APPROVAL` thành `PENDING`).
