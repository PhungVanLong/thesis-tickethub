import re

filepath = r'd:\thesis\BE\management\docs\chapter_4_2_system_architecture.md'

with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Update the mermaid diagram
# Add PaymentGateway node
content = content.replace(
    "SMTP[SMTP Server]:::db",
    "SMTP[SMTP Server]:::db\n        PaymentGateway[Payment Gateway]:::db"
)

# Add Payment Gateway connections
content = content.replace(
    "Booking -->|\"Email\"| SMTP",
    "Booking -->|\"Email\"| SMTP\n\n    %% Payment Integration\n    Booking --> PaymentGateway\n    PaymentGateway --> Booking"
)

# 2. Add bullet point in section 4.2.4
content = content.replace(
    "*   **SMTP Server:** The Management and Booking services integrate with an external SMTP Server to dispatch asynchronous emails (e.g., sending congratulatory emails to approved organizers or PDF E-Tickets to customers).",
    "*   **SMTP Server:** The Management and Booking services integrate with an external SMTP Server to dispatch asynchronous emails (e.g., sending congratulatory emails to approved organizers or PDF E-Tickets to customers).\n*   **Payment Gateway:** A critical external integration for the Booking Service. It handles transaction processing and communicates back with the system via IPN Webhooks to finalize asynchronous ticket allocations."
)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("Architecture diagram and description updated successfully!")
