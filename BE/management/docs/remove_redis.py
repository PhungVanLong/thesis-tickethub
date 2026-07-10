import os

replacements = [
    # chapter_4_4_usecase_implementation.md and academic_report_booking_en.md
    ("Distributed Lock using Redis Cache", "Application-level Distributed Lock using Spring Integration JDBC"),
    ("Redis Distributed Lock", "Spring Integration JdbcLockRegistry"),
    ("blocked at Redis", "blocked at the JDBC Lock layer"),
    ("bypass the Redis layer", "bypass the JDBC Lock layer"),
    ("7.2. Ticket Status Caching with Redis (Redis Lua Scripts)", "7.2. Ticket Inventory Concurrency Control with Optimistic Locking"),
    ("Querying the Management Service DB directly for every customer ticket claim will crash the DB. Instead, place ticket inventory into **Redis Cache**.", "Querying and locking the Management Service DB directly with pessimistic locks for every customer ticket claim can cause bottlenecks. Instead, implement **Optimistic Locking** at the database layer."),
    ("- When an event is published, the Management Service pushes the event's ticket quantity to Redis as Key-Value pairs (e.g., `Event:1:Tier:VIP:Available = 500`).", "- A `@Version` field is added to the `TicketTier` entity in Spring Data JPA."),
    ("- The Booking Service (or Management Service) will use **Redis Lua Scripts** to simultaneously check the balance and deduct the ticket in RAM. Lua Scripts guarantee Atomicity (no Race Conditions).", "- When multiple requests simultaneously attempt to deduct the ticket balance, JPA will include the version number in the UPDATE clause. The first transaction successfully updates the balance and increments the version."),
    ("- If Redis returns success, the Booking Service can safely create the order. Later, the Management Service will Asynchronously sync the remaining ticket status from Redis back to the actual DB. Ticket holding speed will increase from hundreds to tens of thousands of TPS.", "- Subsequent transactions will fail with an `OptimisticLockException`. The Booking Service catches this exception and safely creates a Refund, completely avoiding Database Deadlocks and improving throughput."),
    
    # academic_report_booking.md (Vietnamese)
    ("Khóa phân tán bằng Bộ nhớ đệm Redis", "Khóa phân tán cấp ứng dụng bằng Spring Integration JDBC"),
    ("Khóa phân tán trên Redis (Redis Distributed Lock)", "Spring Integration JdbcLockRegistry"),
    ("bị chặn lại ở Redis", "bị chặn lại ở tầng JDBC Lock"),
    ("lọt qua được tầng Redis", "lọt qua được tầng JDBC Lock"),
    ("7.2. Caching trạng thái vé bằng Redis (Redis Lua Scripts)", "7.2. Kiểm soát đồng thời bằng Optimistic Locking (Khóa lạc quan)"),
    ("Truy vấn trực tiếp CSDL của Management Service cho mỗi lượt khách lấy vé sẽ làm sập DB. Thay vào đó, hãy đưa số lượng vé vào **Redis Cache**.", "Truy vấn và sử dụng khóa bi quan (Pessimistic Lock) trực tiếp trên CSDL cho mỗi lượt khách mua vé có thể làm nghẽn DB. Thay vào đó, hãy sử dụng **Optimistic Locking** (Khóa lạc quan) ở tầng Database."),
    ("- Khi một sự kiện được công bố, Management Service đẩy số lượng vé của sự kiện đó lên Redis dưới dạng Key-Value (ví dụ: `Event:1:Tier:VIP:Available = 500`).", "- Thêm một trường `@Version` vào entity `TicketTier` trong Spring Data JPA."),
    ("- Booking Service (hoặc Management Service) sẽ sử dụng **Redis Lua Script** để thực hiện thao tác kiểm tra số dư và trừ vé cùng lúc trên RAM. Lua Script đảm bảo tính Atomic (không có Race Condition).", "- Khi nhiều request cùng lúc cố gắng trừ vé, lệnh UPDATE SQL sẽ được kèm theo số version. Giao dịch đầu tiên chạy thành công sẽ cập nhật số dư và tăng version lên 1."),
    ("- Nếu Redis báo thành công, Booking Service cứ yên tâm tạo đơn hàng. Sau đó Management Service sẽ dần dần (Asynchronously) đồng bộ trạng thái vé còn lại từ Redis xuống CSDL thực. Tốc độ giữ vé sẽ tăng từ hàng trăm lên hàng vạn TPS.", "- Các giao dịch sau sẽ thất bại do version không khớp và văng ra lỗi `OptimisticLockException`. Hệ thống bắt lỗi này và gọi luồng Hoàn tiền (Refund), giúp tránh tình trạng Deadlock và tối ưu throughput."),
    
    # chapter_3_usecases.md
    ("triggers a Kafka event to warm up the Redis cache", "triggers a Kafka event to prepare necessary event data")
]

files_to_update = [
    r'd:\thesis\BE\management\docs\chapter_4_4_usecase_implementation.md',
    r'd:\thesis\BE\management\docs\academic_report_booking_en.md',
    r'd:\thesis\BE\management\docs\academic_report_booking.md',
    r'd:\thesis\BE\management\docs\chapter_3_usecases.md'
]

for filepath in files_to_update:
    if not os.path.exists(filepath):
        continue
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    for old, new in replacements:
        content = content.replace(old, new)
        
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

print("Redis references removed and replaced with Spring/DB mechanisms!")
