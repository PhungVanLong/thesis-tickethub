import os
import re

filepath = r'd:\thesis\BE\management\docs\chapter_4_4_usecase_implementation.md'

with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace specific headers to match the template
replacements = [
    # 4.4.2 Event Management
    (r'##### 1\.1\. Sequence Diagram - Create Event Flow', r'#### Sequence Diagram'),
    (r'##### 1\.2\. Detailed Process Description', r'#### Detailed Process Description'),
    (r'##### 2\.1\. Sequence Diagram - Phase 1: Event Verification', r'#### Sequence Diagram - Phase 1: Event Verification'),
    (r'##### 2\.2\. Sequence Diagram - Phase 2: Event Publication', r'#### Sequence Diagram - Phase 2: Event Publication'),
    (r'##### 2\.3\. Detailed Process Description', r'#### Detailed Process Description'),
    (r'#### 3\. Main Design Choices', r'#### 3. Notable Architectural Design Solutions'),
    
    # 4.4.3 Ticket Booking & Payment
    (r'##### 2\.1\. Sequence Diagram - Booking and Returning Results via SSE', r'#### Sequence Diagram'),
    (r'##### 2\.2\. Detailed Process Description', r'#### Detailed Process Description'),
    (r'##### 3\.1\. Sequence Diagram - Phase 1: Create Payment Link', r'#### Sequence Diagram - Phase 1: Create Payment Link'),
    (r'##### 3\.2\. Sequence Diagram - Phase 2: Process Background Webhook \(Claim Ticket & Refund\)', r'#### Sequence Diagram - Phase 2: Process Background Webhook'),
    (r'##### 3\.3\. Sequence Diagram - Phase 3: Customer Returns to System', r'#### Sequence Diagram - Phase 3: Customer Returns to System'),
    (r'##### 3\.4\. Detailed Process Description', r'#### Detailed Process Description'),
    
    (r'## 4\. Flow 3: TTL Order Cancellation Flow', r'#### 3. Flow 3: TTL Order Cancellation Flow'),
    (r'##### 4\.1\. Sequence Diagram - Scan and Cancel Expired Orders', r'#### Sequence Diagram'),
    (r'##### 4\.2\. Detailed Process Description', r'#### Detailed Process Description'),
    
    (r'## 5\. Solving High Concurrency IPN & Refund for a Single Seat', r'#### 4. Solving High Concurrency IPN & Refund for a Single Seat'),
    (r'##### 5\.1\. Tier 1: Serialization via Event Queue Partitioning', r'**Tier 1: Serialization via Event Queue Partitioning**'),
    (r'##### 5\.2\. Tier 2: Distributed Lock using Redis Cache', r'**Tier 2: Distributed Lock using Redis Cache**'),
    (r'##### 5\.3\. Tier 3: Pessimistic Lock at Database Layer', r'**Tier 3: Pessimistic Lock at Database Layer**'),
    
    (r'## 6\. Notable Architectural Design Solutions', r'#### 5. Notable Architectural Design Solutions'),
    (r'## 7\. Performance Optimization Solutions', r'#### 6. Performance Optimization Solutions'),
    (r'##### 7\.1\. Internal Communication via gRPC instead of REST API', r'**7.1. Internal Communication via gRPC instead of REST API**'),
    (r'##### 7\.2\. Ticket Status Caching with Redis \(Redis Lua Scripts\)', r'**7.2. Ticket Status Caching with Redis (Redis Lua Scripts)**'),
    (r'##### 7\.3\. Saga Pattern \(Choreography\)', r'**7.3. Saga Pattern (Choreography)**'),
    
    # 4.4.4 E-Ticket Check-in
    (r'#### 1\. E-Ticket Check-in Flow', r'#### 1. Flow 1: E-Ticket Check-in Flow'),
    (r'##### 1\.1\. Sequence Diagram - Check-in Flow', r'#### Sequence Diagram'),
    (r'##### 1\.2\. Detailed Process Description', r'#### Detailed Process Description'),
    (r'#### 2\. Main Design Solutions at Check-in', r'#### 2. Notable Architectural Design Solutions'),
]

for old, new in replacements:
    content = re.sub(old, new, content)

# Also fix the list styles in "Notable Architectural Design Solutions" to match the `*   **Name:**` format.
# We will find blocks under `Notable Architectural Design Solutions` and replace `- **` with `*   **`
def replace_bullets(match):
    text = match.group(0)
    return text.replace('\n- **', '\n*   **')

content = re.sub(r'#### \d+\. Notable Architectural Design Solutions.*?(?=#### \d+\. |### 4\.4\.|#### \d+\. Flow|$)', replace_bullets, content, flags=re.DOTALL)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("Formatted successfully!")
