import os
import re

base_dir = r'd:\thesis\BE\management\docs'
main_file = os.path.join(base_dir, 'chapter_4_4_usecase_implementation.md')

event_file = os.path.join(base_dir, 'academic_report_event_en.md')
booking_file = os.path.join(base_dir, 'academic_report_booking_en.md')
checkin_file = os.path.join(base_dir, 'academic_report_checkin_en.md')

def get_content(fpath):
    with open(fpath, 'r', encoding='utf-8') as f:
        return f.read()

# Read main file
with open(main_file, 'r', encoding='utf-8') as f:
    main_content = f.read()

# 1. Fix main file headers
main_content = main_content.replace(
    '## 4.4 Core Usecase Implementation: Organization Registration and Verification\n\nThis section details',
    '## 4.4 Core Usecase Implementations\n\nThis section details'
)
main_content = main_content.replace('### 4.4.1 Flow 1:', '#### 1. Flow 1:')
main_content = main_content.replace('### 4.4.2 Flow 2:', '#### 2. Flow 2:')
main_content = main_content.replace('### 4.4.3 Notable Architectural', '#### 3. Notable Architectural')
# Add 4.4.1 header
main_content = main_content.replace(
    '## 4.4 Core Usecase Implementations\n\nThis section details the implementation of the core business flows related to Organizer onboarding. It demonstrates how the system handles user registration, administrative verification, and complex distributed transactions across multiple microservices.',
    '## 4.4 Core Usecase Implementations\n\nThis section details the implementation of the core business flows of the system, including organization registration, event management, booking, and check-in processes.\n\n### 4.4.1 Organization Registration and Verification\n\nThis subsection details the implementation of the core business flows related to Organizer onboarding. It demonstrates how the system handles user registration, administrative verification, and complex distributed transactions across multiple microservices.'
)

# 2. Extract and format Event content
event_content = get_content(event_file)
# Remove title and intro up to '## 2. Flow 1'
event_content = re.sub(r'^.*?## 2\. Flow 1', '#### 1. Flow 1', event_content, flags=re.DOTALL)
# Lower header levels
event_content = event_content.replace('## 3. Flow 2', '#### 2. Flow 2')
event_content = event_content.replace('## 4. Main Design Choices', '#### 3. Main Design Choices')
event_content = event_content.replace('### 2.1.', '##### 1.1.')
event_content = event_content.replace('### 2.2.', '##### 1.2.')
event_content = event_content.replace('### 3.1.', '##### 2.1.')
event_content = event_content.replace('### 3.2.', '##### 2.2.')
event_content = event_content.replace('### 3.3.', '##### 2.3.')
event_append = '\n\n### 4.4.2 Event Management (Creation & Publication)\n\n' + event_content

# 3. Extract and format Booking content
booking_content = get_content(booking_file)
booking_content = re.sub(r'^.*?## 2\. Flow 1', '#### 1. Flow 1', booking_content, flags=re.DOTALL)
booking_content = booking_content.replace('## 3. Flow 2', '#### 2. Flow 2')
booking_content = booking_content.replace('## 4. Cancellation Scanner', '#### 3. Cancellation Scanner')
booking_content = booking_content.replace('## 5. Main Design Choices', '#### 4. Main Design Choices')
booking_content = booking_content.replace('## 6. Fault Tolerance', '#### 5. Fault Tolerance')
booking_content = booking_content.replace('## 7. Future Enhancements', '#### 6. Future Enhancements')
# Demote level 3 to level 5
booking_content = re.sub(r'^### (\d)\.(\d)\.', r'##### \1.\2.', booking_content, flags=re.MULTILINE)
booking_append = '\n\n### 4.4.3 Ticket Booking & Payment\n\n' + booking_content

# 4. Extract and format Checkin content
checkin_content = get_content(checkin_file)
checkin_content = re.sub(r'^.*?## 2\. E-Ticket', '#### 1. E-Ticket', checkin_content, flags=re.DOTALL)
checkin_content = checkin_content.replace('## 3. Main Design Solutions', '#### 2. Main Design Solutions')
checkin_content = re.sub(r'^### 2\.(\d)\.', r'##### 1.\1.', checkin_content, flags=re.MULTILINE)
checkin_append = '\n\n### 4.4.4 E-Ticket Check-in\n\n' + checkin_content

# 5. Write everything
final_content = main_content + event_append + booking_append + checkin_append

with open(main_file, 'w', encoding='utf-8') as f:
    f.write(final_content)

print("Done merging 4.4!")
