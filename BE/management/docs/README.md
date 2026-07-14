# TÀI LIỆU DỰ ÁN - DỊCH VỤ QUẢN LÝ (MANAGEMENT SERVICE)

Thư mục này chứa toàn bộ tài liệu hướng dẫn kỹ thuật, đặc tả tích hợp Frontend và báo cáo học thuật liên quan đến luồng nghiệp vụ **Đăng ký và Phê duyệt Tổ chức**.

## Danh sách tài liệu

1. **[Báo cáo Học thuật (academic_report.md)](file:///d:/thesis/BE/management/docs/academic_report.md)**:
   - Mô tả thiết kế kiến trúc hệ thống tổng quát từ tầng giao diện (UI) đến cơ sở dữ liệu.
   - Phân tích chi tiết 2 luồng cốt lõi: Đăng ký tổ chức và Phê duyệt trạng thái đồng bộ quyền qua Kafka.
   - Minh họa các thiết kế nâng cao: **State Machine**, **Transactional Outbox Pattern** và **Saga Pattern** bằng sơ đồ tuần tự (Sequence Diagram).

2. **[Hướng dẫn tích hợp Frontend (fe_integration_guide.md)](file:///d:/thesis/BE/management/docs/fe_integration_guide.md)**:
   - Đặc tả các API nghiệp vụ (`POST /organizations`, `POST /verify`) kèm cấu trúc JSON dữ liệu đầu vào/đầu ra.
   - Bản đồ chuyển đổi trạng thái giao diện dựa trên Enum dữ liệu.
   - Hướng dẫn bắt và xử lý mã lỗi chuẩn hóa (`400`, `409`, `401`, `403`).
   - Lưu ý về UX khi đồng bộ quyền bất đồng bộ.

3. **[Luồng tạo Staff cho FE (staff_account_kafka_flow_fe.md)](file:///d:/thesis/BE/management/docs/staff_account_kafka_flow_fe.md)**:
   - Đặc tả riêng luồng Organizer tạo Staff account cho tổ chức.
   - Mô tả trạng thái `QUEUED` khi xử lý bất đồng bộ qua Kafka/outbox.
   - Chỉ rõ API tạo Staff và API refresh danh sách staff của event để FE đồng bộ dữ liệu.

4. **[Đánh giá mã nguồn (code_review.md)](file:///d:/thesis/BE/management/docs/code_review.md)**:
   - Đánh giá chất lượng thiết kế mã nguồn của các lớp Controller, Service, Repository, Scheduler và Consumer.
   - Các điểm mạnh kỹ thuật (phòng rò rỉ bộ nhớ, truy vấn tối ưu).
   - Đề xuất cải tiến kỹ thuật (Email bất đồng bộ, quản lý Retry Outbox).

5. **[Hướng dẫn phát triển cho AI (developer_guidelines.md)](file:///d:/thesis/BE/management/docs/developer_guidelines.md)**:
   - Các quy tắc bảo vệ cấu hình an ninh, nghiệp vụ, và bắt lỗi của luồng hiện tại.
   - Hướng dẫn các mô hình AI khác cách phát triển các luồng mới sử dụng Outbox, Saga, và quy tắc ngôn ngữ code mà không làm ảnh hưởng/phá vỡ luồng cũ.

6. **[Đầu việc phát triển (tasks.md)](file:///d:/thesis/BE/management/docs/tasks.md)**:
   - Danh sách theo dõi tiến độ phát triển các cấu phần của dịch vụ quản lý.
