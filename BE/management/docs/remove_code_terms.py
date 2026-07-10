import os

files_to_update = [
    r'd:\thesis\BE\management\docs\chapter_4_4_usecase_implementation.md',
    r'd:\thesis\BE\management\docs\academic_report_booking_en.md',
    r'd:\thesis\BE\management\docs\academic_report_booking.md'
]

replacements = [
    # English replacements
    ("Spring Integration JdbcLockRegistry", "a central database lock registry"),
    ("A `@Version` field is added to the `TicketTier` entity in Spring Data JPA", "A version tracking field is added to the ticket data model"),
    ("JPA will include the version number in the UPDATE clause", "the data access layer includes the current version identifier in the update request"),
    ("fail with an `OptimisticLockException`", "fail with a concurrent modification error"),
    ("Spring Integration JDBC", "a database-backed lock mechanism"),
    
    # Vietnamese replacements
    ("Spring Integration JdbcLockRegistry", "một bảng khóa tập trung trên cơ sở dữ liệu"),
    ("Thêm một trường `@Version` vào entity `TicketTier` trong Spring Data JPA", "Thêm một trường theo dõi phiên bản (version tracking) vào mô hình dữ liệu hạng vé"),
    ("lệnh UPDATE SQL sẽ được kèm theo số version", "yêu cầu cập nhật sẽ tự động đối chiếu số phiên bản hiện tại"),
    ("lỗi `OptimisticLockException`", "ngoại lệ xung đột dữ liệu đồng thời"),
    ("bằng Spring Integration JDBC", "bằng cơ chế khóa trên cơ sở dữ liệu")
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

print("Code terminology removed!")
