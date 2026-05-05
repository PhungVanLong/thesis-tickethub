# Hệ thống quản lý đặt vé — REQUIREMENT.md

Phiên bản: 1.0
Ngày: 2026-04-20
Tác giả: (tự động tạo)

Mục đích: Tài liệu này phân tích, sửa lỗi và chuẩn hóa các yêu cầu (functional và non-functional) do khách hàng cung cấp, bổ sung acceptance criteria, các chỉ số đo lường, mô tả dữ liệu cơ bản và danh sách API cần thiết để bắt đầu thiết kế và triển khai.

---

TOC
- Tóm tắt và thay đổi chính
- Vấn đề tìm thấy (Issue Log - tóm tắt)
- Yêu cầu chức năng (chuẩn hóa)
- Yêu cầu không chức năng (NFR) — đo lường được
- Luồng khóa ghế & consistency
- Kiến trúc & Messaging
- Mô hình dữ liệu (cơ bản)
- Danh sách API & sự kiện Kafka (tóm tắt)
- Các câu hỏi còn mở và giả định
- Traceability & Acceptance Criteria

---

1) Tóm tắt và thay đổi chính
- Đã sửa các lỗi cấu trúc/đánh số (ví dụ phần 1.7 xuất hiện sau 1.5, thiếu 1.6). Đã chuẩn hóa tên vai trò và các tính năng tương ứng.
- Bổ sung/chi tiết hoá các thông số phi chức năng thiếu: mức concurrency, TTL khóa ghế cố định, SLA latency, cách xử lý idempotency, chiến lược distributed lock, cơ chế bypass cho load test.
- Ghi rõ các topic Kafka, payload events cơ bản và API endpoints tối thiểu để phát triển.

2) Vấn đề tìm thấy (tóm tắt)
- Đánh số/structure: Số mục nhảy (1.5 → 1.7). Có khả năng thiếu mục.
- Thiếu ràng buộc, giá trị mặc định: Thời gian khóa ghế viết là "5-10 phút" (không rõ mặc định và có thể khác nhau theo event).
- "High Concurrency" ghi "2.000 - 5.000 user (mức localhost)" — mơ hồ: "localhost" không phải chỉ tiêu sản xuất; cần chỉ rõ concurrent seat locks/sec, hoặc concurrent users/session.
- Response Time: "< 200ms" nhưng không nêu percentile (P50/P95/P99) hoặc endpoint cụ thể.
- Redis locking: nêu "Sử dụng Redis" nhưng không chỉ rõ thuật toán (Redlock?) và cách xử lý split-brain.
- Idempotency: nêu nhưng không nói cách tạo idempotency key hay phạm vi (order vs payment webhook).
- Anti-bot Bypass: Cơ chế bypass qua header đặc biệt nguy hiểm nếu không giới hạn IP/ chứng thực.
- Kafka/WebSockets: thiếu tên topic/event schema, thiếu định nghĩa payload và chế độ bảo đảm delivery (at-least-once/at-most-once).
- Thanh toán: ghi các cổng (VNPAY/MoMo) nhưng không nêu luồng webhook, refund policy, hoặc flow khi thanh toán bị lỗi.
- E-ticket: "tạo mã QR duy nhất" nhưng không nêu dữ liệu trong QR (ticketId + signature?) và expiry/validation rules.
- Typo/truncated: có câu cuối bị cắt "tran".

3) Yêu cầu chức năng (chuẩn hoá theo vai trò)

3.1 Guest (Người qua đường)
- FR-G1: Duyệt danh sách sự kiện và xem chi tiết (mô tả, thời gian, địa điểm, giá vé). Acceptance: có API trả events với pagination.
- FR-G2: Tìm kiếm cơ bản theo tên sự kiện hoặc địa điểm. Acceptance: tìm theo tên và địa điểm trả kết quả khớp theo fuzzy/substring.
- FR-G3: Giới hạn quyền: không thể đặt vé trước khi đăng nhập. (Hoặc cho phép bắt đầu giỏ nhưng require login khi thanh toán.)

3.2 Customer (Khách hàng)
- FR-C1: Tìm kiếm nâng cao: theo tên, địa điểm, thời gian, thể loại, bộ lọc giá. Acceptance: API filterable.
- FR-C2: Xem sơ đồ ghế theo thời gian thực. Acceptance: WebSocket cập nhật trạng thái ghế (free/locked/sold) trong vòng < 1s sau thay đổi trên server.
- FR-C3: Chọn ghế và khóa tạm thời (Lock) mặc định 5 phút (300s) configurable per-event. Nếu muốn khác có thể cấu hình từ organizer. Acceptance: khóa có TTL và hiển thị countdown.
- FR-C4: Thanh toán tích hợp VNPAY/MoMo; xử lý webhook, idempotent processing và refund policy. Acceptance: payment API trả mã trạng thái, webhook xử lý idempotently.
- FR-C5: E-Ticket: sau thanh toán thành công, tạo vé với mã QR chứa {ticketId, signature, expiry}, và gửi qua email (asynchronous). Acceptance: QR hợp lệ, có thể validate offline với public key.
- FR-C6: Lịch sử & Thông báo: người dùng có thể xem danh sách vé đã mua; gửi email thông báo giao dịch thành công và nhắc trước event (configurable).

3.3 Event Organizer
- FR-O1: Tạo, chỉnh sửa, gửi yêu cầu phê duyệt event. Acceptance: chỉnh sửa lưu draft; khi submit -> status=PendingApproval.
- FR-O2: Cấu hình sơ đồ & giá: tạo các hạng vé, mapping ghế → zone/price. Acceptance: upload sơ đồ CSV/JSON hoặc WYSIWYG map.
- FR-O3: Quản lý nhân sự: tạo tài khoản Staff với role-based access (Staff limited to check-in). Acceptance: RBAC endpoints.
- FR-O4: Báo cáo: revenue, occupancy, sell-rate. Acceptance: có endpoint trả thời gian thực aggregate (cacheable).

3.4 Staff (Nhân viên soát vé)
- FR-S1: Check-in: quét QR bằng mobile app; mark ticket as used and log operatorId/timestamp. Acceptance: API to mark check-in with idempotency.
- FR-S2: Tra cứu trạng thái vé và lịch sử quét. Acceptance: trả ticket status và audit trail.

3.5 System Admin
- FR-A1: Phê duyệt event, quản lý organizer accounts (approve/suspend/delete). Acceptance: admin UI/API + audit logs.
- FR-A2: Cấu hình hệ thống: commission rate, anti-bot thresholds. Acceptance: config stored and versioned.
- FR-A3: Báo cáo tổng hợp: tổng dòng tiền, user traffic, health.

3.6 Monitoring / Developer (Dashboard)
- FR-M1: Dashboard giám sát (Prometheus/Grafana) exposes metrics: request/sec, error rate, latency histograms, service status.
- FR-M2: Distributed Tracing support (e.g., Jaeger) để truy vết request.

4) Yêu cầu không chức năng — đo lường được (NFR)
- NFR-1 (Concurrency): Hệ thống phải hỗ trợ 5.000 concurrent active users (điểm tham khảo) với khả năng xử lý ít nhất 200 seat-lock requests / sec trên toàn hệ thống. (Conservative target — phải load-test & điều chỉnh.)
- NFR-2 (Latency): APIs quan trọng (GET seat map, POST lock-seat, POST payment) P95 < 200ms; P99 < 500ms (thời gian phản hồi server-side, không bao gồm mạng di động chậm).
- NFR-3 (Availability): 99.95% uptime cho ticketing core trong mùa bán vé.
- NFR-4 (Lock TTL): DefaultLockTTL = 300s (5 phút), configurable per-event; nếu TTL expired, lock auto-release and notify via WebSocket.
- NFR-5 (Consistency): Sử dụng Redis-based distributed lock (Redlock pattern) cho seat-level locks; kèm fallback optimistic reservation + eventual reconciliation batch if conflict.
- NFR-6 (Idempotency): Mỗi giao dịch quan trọng (create-order, payment webhook) phải chấp nhận idempotency-key (UUID) để tránh double-charge.
- NFR-7 (Security): JWT + Spring Security RBAC; secrets rotated; reCAPTCHA v3 on critical endpoints; rate-limiting per IP & per account (e.g., 10 requests/sec with burst allowed), honeypot fields for forms.
- NFR-8 (Anti-bot bypass): Bypass header chỉ được cho phép từ IPs được whitelist của load test và kèm HMAC signature + short TTL; mọi bypass đều audit-logged.
- NFR-9 (Asynchronous ops): Heavy tasks (send email, generate PDF/QR) must be pushed to Kafka for background workers; payment flow must wait only for payment confirmation, not for post-processing.

5) Luồng khóa ghế & đề xuất kỹ thuật
- Khi customer chọn ghế: client gọi POST /events/{id}/seats/{seatId}/lock với idempotency-key.
- Server: kiểm tra seat trạng thái, cố gắng acquire distributed lock (Redis Redlock) trên seatId; nếu success create temporary reservation record (status=LOCKED) with expiresAt = now + TTL.
- Đẩy event "seat.locked" lên Kafka và push WebSocket cập nhật tới các client đang xem map.
- Nếu user thanh toán thành công: đổi reservation -> ORDERED -> issue ticket(s) -> mark seats as SOLD, phát event "seat.sold".
- Nếu TTL hết hoặc user hủy: release lock, phát event "seat.released".
- Trong trường hợp race/conflict: sử dụng compare-and-set + reconciliation job (periodic) để xử lý locks stale.

6) Messaging & Topics (mẫu)
- kafka.topic.seat = ticketing.seat.events (events: seat.locked, seat.released, seat.sold)
- kafka.topic.payment = ticketing.payment.events (payment.initiated, payment.succeeded, payment.failed, refund.processed)
- kafka.topic.email = ticketing.email.tasks (send.eticket, send.notification)

7) Mô hình dữ liệu (tóm tắt)
- User { id, name, email, roles[] }
- Event { id, title, description, venueId, startAt, endAt, status }
- Venue { id, name, location, seatingMapId }
- Seat { id, venueId, row, number, zoneId }
- Ticket { id, orderId, seatId, qrPayload, status (ACTIVE/USED/CANCELLED), issuedAt }
- Order { id, userId, amount, currency, status (PENDING/PAID/FAILED/REFUNDED), createdAt }
- Payment { id, orderId, provider, providerPaymentId, status, processedAt }

8) API tối thiểu (tóm tắt)
- GET /events — list events (supports query parameters: q, page)
- GET /events/{id} — event details
- GET /events/{id}/seats — seat map (supports WebSocket subscription)
- POST /events/{id}/seats/{seatId}/lock — lock seat (idempotency-key)
- POST /orders — create order / start payment flow
- POST /payments/webhook/{provider} — payment webhook (idempotent)
- GET /users/{id}/orders — order history
- POST /tickets/{ticketId}/checkin — staff check-in (requires staff token)
- Admin: POST /events/{id}/approve, GET /admin/reports

9) Acceptance Criteria & Traceability (mẫu)
- FR-C3 Locking: Test case: 50 clients concurrently try lock same seat -> only 1 success, others receive 409/locked response; after TTL expired, seat becomes available.
- NFR-2 Latency: Run load test to verify P95 for GET seats and POST lock-seat < 200ms under target load (describe test harness: 5k virtual users, ramp 10m).

10) Open questions & Assumptions
- Q1: Mức concurrency mục tiêu chính xác (peak daily users, peak TPS) ? (assume 5k concurrent active users until confirmed)
- Q2: Refund policy chi tiết (full refund, fees, time windows)?
- Q3: Có yêu cầu KYC cho organizers/large sellers?
- Q4: Phương thức backup cho khi Redis mất? (assume fallback optimistic locking + reconciliation)
- Giả định: All providers (VNPAY/MoMo) hỗ trợ webhooks và test sandbox.

11) Appendix: Ghi chú về các lỗi câu chữ nguyên bản
- Phần cuối của tài liệu gốc có từ bị cắt "tran" — coi là lỗi gõ/nhập liệu.
- Cần thống nhất các thuật ngữ VN/EN (ví dụ: Staff = Nhân viên soát vé; Organizer = Đơn vị tổ chức).

---

Kết luận ngắn: Tôi đã chuẩn hóa các yêu cầu, chỉ rõ những chỗ cần quyết định thêm trước khi triển khai, và cung cấp acceptance criteria cùng các NFR đo lường được để lập kế hoạch testing/scale. Vui lòng xác nhận các giả định trên (concurrency target, refund policy, bypass header rules) hoặc cung cấp thông tin để cập nhật tài liệu.


