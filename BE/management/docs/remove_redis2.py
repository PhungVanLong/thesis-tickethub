import os

files_to_update = [
    r'd:\thesis\BE\management\docs\chapter_4_4_usecase_implementation.md',
    r'd:\thesis\BE\management\docs\academic_report_booking_en.md',
    r'd:\thesis\BE\management\docs\academic_report_booking.md',
    r'd:\thesis\BE\management\docs\chapter_3_usecases.md'
]

replacements = [
    ("Khóa phân tán trên Redis", "Spring Integration JdbcLockRegistry"),
    ("Redis", "JDBC"), # any remaining Redis could just be replaced if it's safe, but maybe not globally.
]

for filepath in files_to_update:
    if not os.path.exists(filepath):
        continue
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # manual fix for that specific string
    content = content.replace("Khóa phân tán trên Redis (Spring Integration JdbcLockRegistry)", "Khóa phân tán cấp ứng dụng bằng Spring Integration JdbcLockRegistry")
    content = content.replace("Khóa phân tán trên Redis", "Spring Integration JdbcLockRegistry")
        
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

print("Fixed remaining Redis strings")
