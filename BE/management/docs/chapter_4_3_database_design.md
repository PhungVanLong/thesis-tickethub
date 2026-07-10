## 4.3 Database Design

### 4.3.1 Database-per-Service Paradigm

To ensure loose coupling and fault tolerance, TicketHub adheres to the **Database-per-Service** architectural pattern. Instead of relying on a single monolithic database, the system is physically partitioned into three distinct PostgreSQL databases: Identity DB, Management DB, and Booking DB. 

This design guarantees that an overload or failure in one database (e.g., high traffic during ticket sales in the Booking DB) does not impact the availability of other services (e.g., user authentication in the Identity DB). It also enforces strict data boundaries, preventing services from executing direct, tangled SQL queries across domains.

### 4.3.2 The Transactional Outbox Pattern

A major challenge in a Database-per-Service architecture is maintaining data consistency when a service needs to update its local database and publish an event to Kafka simultaneously (the Dual-Write problem). To solve this, all three databases in TicketHub implement the **Transactional Outbox Pattern**.

**Main entities for Event Publishing:**
*   **outbox_events (All Databases):** Acts as a temporary queue for outbound messages. Instead of publishing directly to Kafka, services write events to this table within the same local database transaction. A separate background process then safely reads this table and publishes the events to Kafka, guaranteeing "at-least-once" delivery.
*   **processed_events (Booking DB):** Prevents duplicate event processing (Idempotency). It records the IDs of Kafka events that have already been successfully consumed, ensuring that the Booking Service does not process the same payment or ticket event twice.

### 4.3.3 Identity Service Database

This database acts as the access control and security logging system, responsible for identity verification, user authorization, and centralized platform monitoring.

**Main entities**
*   **users:** Stores core user information, authentication credentials (password hashes), and role definitions (Customer, Organizer, Admin) to manage access rights.
*   **notifications:** Manages the queue and delivery status of in-app or system notifications targeted at specific users.
*   **system_logs:** Records critical security and operational events (audit trail) for monitoring user actions and system health.
*   **captcha_logs:** Tracks reCAPTCHA verification attempts to prevent automated bot attacks during authentication flows.
*   **platform_config:** Stores global, dynamically updatable system configuration keys and values that govern platform behavior.

### 4.3.4 Management Service Database

This database serves as the core operational hub, managing the foundational data for events, organizational profiles, seating arrangements, and overall platform analytics.

**Main entities**
*   **organizations:** Stores profiles, legal verification documents, and contact details of event organizers.
*   **organization_members:** The junction table that assigns users (from the Identity Service) to specific roles within an organization.
*   **events:** Stores comprehensive details about the events, including scheduling, location, and publication status.
*   **event_approvals:** Tracks the administrative decisions (approve/reject) and reasoning for events submitted by organizers.
*   **event_staff:** Assigns specific staff members to assist with operations at particular events.
*   **seat_maps & seats:** Defines the physical layout of the venue. `seat_maps` stores the grid dimensions and JSON layout, while `seats` manages individual seat coordinates and their real-time availability status.
*   **ticket_tiers:** Categorizes tickets (e.g., VIP, Standard) for an event, managing pricing and total inventory available for sale.
*   **analytics_events:** Stores aggregated snapshots of event performance, including total revenue and check-in counts for reporting purposes.

### 4.3.5 Booking Service Database

This database is highly transactional, dedicated solely to processing customer orders, securing payments, and generating tickets without blocking core management operations.

**Main entities**
*   **orders:** Tracks the lifecycle of a customer's purchase, storing the total amount, currency, and overall transaction status (e.g., Pending, Completed).
*   **order_items:** Details the specific tickets purchased within an order, linking the transaction to a specific seat and price tier.
*   **payments:** Manages payment gateway transactions, storing the amount, gateway references, idempotency keys, and refund details.
*   **tickets:** The final digital asset generated after a successful payment. It stores the QR code URL, expiration date, and current validity status.
*   **checkins:** Records the timestamp and staff member details when a ticket is successfully scanned at the event venue.
*   **ticket_tiers_ref:** A replicated reference table (Materialized View). It synchronizes essential event and pricing data from the Management Service via Kafka. This allows the Booking Service to rapidly validate ticket prices and availability locally without needing to query the Management DB during high-traffic sales.
