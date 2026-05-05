# TicketHub Kafka Events & Messaging Schema

**Version:** 1.0  
**Date:** 2026-04-21  
**Message Format:** JSON over Kafka (Avro optional for schema evolution)

---

## Kafka Cluster Configuration

```yaml
bootstrap_servers: kafka-1:9092,kafka-2:9092,kafka-3:9092
replication_factor: 3
min_insync_replicas: 2
retention_ms: 7776000000  # 90 days
compression_type: snappy
```

---

## Topic Structure & Naming Convention

**Pattern:** `{environment}.{domain}.{entity}.{event_type}`

**Examples:**
- `prod.ticketing.booking.created`
- `prod.ticketing.payment.succeeded`
- `prod.ticketing.seat.locked`
- `staging.ticketing.booking.created`

---

## Event Topics

### 1. SEAT LIFECYCLE EVENTS
**Topic:** `ticketing.seat.events`
**Partition Key:** `seat_id`
**Replication Factor:** 3

#### 1.1 seat.locked
Seat is temporarily locked for booking

```json
{
  "event_id": "550e8400-e29b-41d4-a716-441e6e6a",
  "event_type": "seat.locked",
  "timestamp": "2026-04-21T10:00:00Z",
  "version": 1,
  "data": {
    "seat_id": 101,
    "seat_number": "A1",
    "event_id": 1,
    "booking_id": 5001,
    "customer_id": 123,
    "lock_duration_seconds": 300,
    "locked_until": "2026-04-21T10:05:00Z",
    "ticket_type_id": 1
  }
}
```

#### 1.2 seat.unlocked
Seat lock is released (TTL expired or customer canceled)

```json
{
  "event_id": "550e8400-e29b-41d4-a716-441e6e6b",
  "event_type": "seat.unlocked",
  "timestamp": "2026-04-21T10:05:00Z",
  "version": 1,
  "data": {
    "seat_id": 101,
    "seat_number": "A1",
    "event_id": 1,
    "booking_id": 5001,
    "reason": "TTL_EXPIRED",
    "unlocked_at": "2026-04-21T10:05:00Z"
  }
}
```

**Reason Values:** `TTL_EXPIRED`, `CUSTOMER_CANCELED`, `PAYMENT_FAILED`, `REFUND_PROCESSED`

#### 1.3 seat.booked
Seat is permanently booked (payment successful)

```json
{
  "event_id": "550e8400-e29b-41d4-a716-441e6e6c",
  "event_type": "seat.booked",
  "timestamp": "2026-04-21T10:05:30Z",
  "version": 1,
  "data": {
    "seat_id": 101,
    "seat_number": "A1",
    "event_id": 1,
    "booking_id": 5001,
    "ticket_id": 10001,
    "customer_id": 123,
    "payment_id": 9001,
    "price": 50.00,
    "booked_at": "2026-04-21T10:05:30Z"
  }
}
```

---

### 2. BOOKING LIFECYCLE EVENTS
**Topic:** `ticketing.booking.events`
**Partition Key:** `booking_id`
**Replication Factor:** 3

#### 2.1 booking.created
Customer creates a booking

```json
{
  "event_id": "550e8400-e29b-41d4-a716-441e6e7a",
  "event_type": "booking.created",
  "timestamp": "2026-04-21T10:00:00Z",
  "version": 1,
  "data": {
    "booking_id": 5001,
    "booking_code": "BOOK_5001_ABCDE",
    "customer_id": 123,
    "event_id": 1,
    "seats": [101, 102, 103],
    "total_price": 120.00,
    "discount_amount": 30.00,
    "commission": 6.00,
    "status": "PENDING",
    "expires_at": "2026-04-21T10:05:00Z",
    "idempotency_key": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

#### 2.2 booking.payment_initiated
Payment process started

```json
{
  "event_id": "550e8400-e29b-41d4-a716-441e6e7b",
  "event_type": "booking.payment_initiated",
  "timestamp": "2026-04-21T10:00:30Z",
  "version": 1,
  "data": {
    "booking_id": 5001,
    "payment_id": 9001,
    "customer_id": 123,
    "event_id": 1,
    "amount": 120.00,
    "payment_method": "VNPAY",
    "initiated_at": "2026-04-21T10:00:30Z"
  }
}
```

#### 2.3 booking.completed
Booking is confirmed after successful payment

```json
{
  "event_id": "550e8400-e29b-41d4-a716-441e6e7c",
  "event_type": "booking.completed",
  "timestamp": "2026-04-21T10:05:30Z",
  "version": 1,
  "data": {
    "booking_id": 5001,
    "booking_code": "BOOK_5001_ABCDE",
    "customer_id": 123,
    "event_id": 1,
    "tickets": [10001, 10002, 10003],
    "total_price": 120.00,
    "payment_id": 9001,
    "completed_at": "2026-04-21T10:05:30Z"
  }
}
```

#### 2.4 booking.cancelled
Customer cancels booking

```json
{
  "event_id": "550e8400-e29b-41d4-a716-441e6e7d",
  "event_type": "booking.cancelled",
  "timestamp": "2026-04-21T10:30:00Z",
  "version": 1,
  "data": {
    "booking_id": 5001,
    "customer_id": 123,
    "event_id": 1,
    "reason": "CUSTOMER_REQUEST",
    "tickets": [10001, 10002, 10003],
    "cancelled_at": "2026-04-21T10:30:00Z"
  }
}
```

---

### 3. PAYMENT LIFECYCLE EVENTS
**Topic:** `ticketing.payment.events`
**Partition Key:** `payment_id`
**Replication Factor:** 3

#### 3.1 payment.initiated
Payment gateway call initiated

```json
{
  "event_id": "550e8400-e29b-41d4-a716-441e6e8a",
  "event_type": "payment.initiated",
  "timestamp": "2026-04-21T10:00:30Z",
  "version": 1,
  "data": {
    "payment_id": 9001,
    "booking_id": 5001,
    "customer_id": 123,
    "amount": 120.00,
    "currency": "USD",
    "payment_method": "VNPAY",
    "gateway_url": "https://payment-gateway.vnpay.vn/pay?transaction=...",
    "initiated_at": "2026-04-21T10:00:30Z",
    "expires_at": "2026-04-21T10:15:30Z"
  }
}
```

#### 3.2 payment.authorized
Payment is authorized (customer confirmed, amount reserved)

```json
{
  "event_id": "550e8400-e29b-41d4-a716-441e6e8b",
  "event_type": "payment.authorized",
  "timestamp": "2026-04-21T10:01:00Z",
  "version": 1,
  "data": {
    "payment_id": 9001,
    "booking_id": 5001,
    "customer_id": 123,
    "amount": 120.00,
    "gateway_transaction_id": "VNP_12345_ABCDE",
    "authorization_code": "AUTH_CODE_12345",
    "authorized_at": "2026-04-21T10:01:00Z"
  }
}
```

#### 3.3 payment.captured
Amount is captured (final charge)

```json
{
  "event_id": "550e8400-e29b-41d4-a716-441e6e8c",
  "event_type": "payment.captured",
  "timestamp": "2026-04-21T10:02:00Z",
  "version": 1,
  "data": {
    "payment_id": 9001,
    "booking_id": 5001,
    "customer_id": 123,
    "amount": 120.00,
    "gateway_transaction_id": "VNP_12345_ABCDE",
    "captured_at": "2026-04-21T10:02:00Z",
    "receipt_number": "RCP_12345"
  }
}
```

#### 3.4 payment.failed
Payment transaction failed

```json
{
  "event_id": "550e8400-e29b-41d4-a716-441e6e8d",
  "event_type": "payment.failed",
  "timestamp": "2026-04-21T10:03:00Z",
  "version": 1,
  "data": {
    "payment_id": 9001,
    "booking_id": 5001,
    "customer_id": 123,
    "amount": 120.00,
    "payment_method": "VNPAY",
    "error_code": "INSUFFICIENT_FUNDS",
    "error_message": "Customer has insufficient funds",
    "gateway_transaction_id": "VNP_12345_ABCDE",
    "failed_at": "2026-04-21T10:03:00Z",
    "retry_count": 1,
    "max_retries": 3
  }
}
```

#### 3.5 payment.refunded
Payment is refunded

```json
{
  "event_id": "550e8400-e29b-41d4-a716-441e6e8e",
  "event_type": "payment.refunded",
  "timestamp": "2026-04-21T12:00:00Z",
  "version": 1,
  "data": {
    "payment_id": 9001,
    "refund_id": 8001,
    "booking_id": 5001,
    "customer_id": 123,
    "original_amount": 120.00,
    "refund_amount": 96.00,
    "refund_policy_percent": 80,
    "reason": "CUSTOMER_REQUEST",
    "gateway_refund_id": "REFUND_12345",
    "refunded_at": "2026-04-21T12:00:00Z"
  }
}
```

---

### 4. TICKET LIFECYCLE EVENTS
**Topic:** `ticketing.ticket.events`
**Partition Key:** `ticket_id`
**Replication Factor:** 3

#### 4.1 ticket.generated
E-ticket is generated with QR code

```json
{
  "event_id": "550e8400-e29b-41d4-a716-441e6e9a",
  "event_type": "ticket.generated",
  "timestamp": "2026-04-21T10:05:30Z",
  "version": 1,
  "data": {
    "ticket_id": 10001,
    "booking_id": 5001,
    "ticket_code": "TKT_10001_XYZ",
    "customer_id": 123,
    "event_id": 1,
    "seat_id": 101,
    "seat_number": "A1",
    "qr_code_url": "https://cdn.tickethub.io/qr/10001.png",
    "qr_code_data": "{...signed_json...}",
    "generated_at": "2026-04-21T10:05:30Z"
  }
}
```

#### 4.2 ticket.checked_in
Ticket is used during event check-in

```json
{
  "event_id": "550e8400-e29b-41d4-a716-441e6e9b",
  "event_type": "ticket.checked_in",
  "timestamp": "2026-05-15T20:15:30Z",
  "version": 1,
  "data": {
    "ticket_id": 10001,
    "ticket_code": "TKT_10001_XYZ",
    "booking_id": 5001,
    "customer_id": 123,
    "event_id": 1,
    "seat_id": 101,
    "seat_number": "A1",
    "staff_id": 456,
    "checked_in_at": "2026-05-15T20:15:30Z",
    "device_id": "device_001"
  }
}
```

#### 4.3 ticket.cancelled
Ticket is voided (refund processed or event cancelled)

```json
{
  "event_id": "550e8400-e29b-41d4-a716-441e6e9c",
  "event_type": "ticket.cancelled",
  "timestamp": "2026-04-21T12:00:00Z",
  "version": 1,
  "data": {
    "ticket_id": 10001,
    "ticket_code": "TKT_10001_XYZ",
    "booking_id": 5001,
    "customer_id": 123,
    "event_id": 1,
    "reason": "REFUND_PROCESSED",
    "cancelled_at": "2026-04-21T12:00:00Z"
  }
}
```

---

### 5. NOTIFICATION EVENTS
**Topic:** `ticketing.notification.events`
**Partition Key:** `customer_id`
**Replication Factor:** 2

#### 5.1 notification.booking_confirmed
Send booking confirmation email

```json
{
  "event_id": "550e8400-e29b-41d4-a716-441e6e0a",
  "event_type": "notification.booking_confirmed",
  "timestamp": "2026-04-21T10:05:30Z",
  "version": 1,
  "data": {
    "notification_id": 6001,
    "customer_id": 123,
    "booking_id": 5001,
    "customer_email": "john@example.com",
    "customer_name": "John Doe",
    "event_title": "Concert 2026",
    "booking_code": "BOOK_5001_ABCDE",
    "total_price": 120.00,
    "tickets_count": 3,
    "template": "booking_confirmed",
    "created_at": "2026-04-21T10:05:30Z"
  }
}
```

#### 5.2 notification.event_reminder
Send event reminder email (48h, 24h, or 1h before)

```json
{
  "event_id": "550e8400-e29b-41d4-a716-441e6e0b",
  "event_type": "notification.event_reminder",
  "timestamp": "2026-05-14T20:00:00Z",
  "version": 1,
  "data": {
    "notification_id": 6002,
    "customer_id": 123,
    "booking_id": 5001,
    "customer_email": "john@example.com",
    "event_id": 1,
    "event_title": "Concert 2026",
    "event_start_at": "2026-05-15T20:00:00Z",
    "event_location": "National Stadium",
    "template": "event_reminder_24h",
    "created_at": "2026-05-14T20:00:00Z"
  }
}
```

#### 5.3 notification.checkin_confirmation
Send check-in confirmation notification

```json
{
  "event_id": "550e8400-e29b-41d4-a716-441e6e0c",
  "event_type": "notification.checkin_confirmation",
  "timestamp": "2026-05-15T20:15:30Z",
  "version": 1,
  "data": {
    "notification_id": 6003,
    "customer_id": 123,
    "ticket_id": 10001,
    "customer_email": "john@example.com",
    "event_id": 1,
    "event_title": "Concert 2026",
    "seat_number": "A1",
    "checked_in_at": "2026-05-15T20:15:30Z",
    "template": "checkin_confirmation",
    "created_at": "2026-05-15T20:15:30Z"
  }
}
```

---

### 6. ANALYTICS EVENTS
**Topic:** `ticketing.analytics.events`
**Partition Key:** `event_id` or `customer_id`
**Replication Factor:** 2

#### 6.1 event.view
Customer views event details

```json
{
  "event_id": "550e8400-e29b-41d4-a716-441e6e1a",
  "event_type": "event.view",
  "timestamp": "2026-04-21T09:30:00Z",
  "version": 1,
  "data": {
    "event_id": 1,
    "customer_id": 123,
    "device_type": "MOBILE",
    "ip_address": "192.168.1.100",
    "session_id": "SESSION_XYZ",
    "viewed_at": "2026-04-21T09:30:00Z"
  }
}
```

#### 6.2 event.search
Customer searches for events

```json
{
  "event_id": "550e8400-e29b-41d4-a716-441e6e1b",
  "event_type": "event.search",
  "timestamp": "2026-04-21T09:30:00Z",
  "version": 1,
  "data": {
    "customer_id": 123,
    "search_query": "concert",
    "filters": {
      "category": "MUSIC",
      "price_max": 100
    },
    "results_count": 15,
    "device_type": "MOBILE",
    "session_id": "SESSION_XYZ",
    "searched_at": "2026-04-21T09:30:00Z"
  }
}
```

---

### 7. BOT DETECTION EVENTS
**Topic:** `ticketing.security.events`
**Partition Key:** `ip_address` or `user_id`
**Replication Factor:** 2

#### 7.1 bot.detected
Bot or anomalous behavior detected

```json
{
  "event_id": "550e8400-e29b-41d4-a716-441e6e2a",
  "event_type": "bot.detected",
  "timestamp": "2026-04-21T10:00:00Z",
  "version": 1,
  "data": {
    "detection_id": 7001,
    "user_id": 123,
    "ip_address": "192.168.1.100",
    "device_fingerprint": "fingerprint_hash",
    "detection_type": "RATE_LIMIT",
    "risk_score": 95,
    "description": "Too many lock requests in short time",
    "action_taken": "BLOCK",
    "detected_at": "2026-04-21T10:00:00Z"
  }
}
```

---

## Event Consumer Groups

| Consumer Group | Topics | Purpose | Rebalance Period |
|----------------|--------|---------|-----------------|
| `notification-email-consumer` | `ticketing.booking.events`, `ticketing.notification.events`, `ticketing.ticket.events` | Send emails | 60s |
| `analytics-consumer` | `ticketing.booking.events`, `ticketing.payment.events`, `ticketing.event.*` | Aggregate analytics | 120s |
| `websocket-consumer` | `ticketing.seat.events`, `ticketing.booking.events` | Push real-time updates | 30s |
| `seat-sync-consumer` | `ticketing.seat.events` | Update seat status | 60s |
| `reporting-consumer` | `ticketing.payment.events`, `ticketing.booking.events` | Generate reports | 300s |
| `audit-consumer` | All topics | Log all events for audit trail | 600s |

---

## Message Retention & Cleanup

```yaml
topics:
  ticketing.seat.events:
    retention_ms: 86400000  # 1 day
    cleanup_policy: delete
  
  ticketing.booking.events:
    retention_ms: 2592000000  # 30 days
    cleanup_policy: delete
  
  ticketing.payment.events:
    retention_ms: 7776000000  # 90 days
    cleanup_policy: delete
  
  ticketing.notification.events:
    retention_ms: 604800000  # 7 days
    cleanup_policy: delete
  
  ticketing.analytics.events:
    retention_ms: 2592000000  # 30 days
    cleanup_policy: delete
  
  ticketing.security.events:
    retention_ms: 7776000000  # 90 days
    cleanup_policy: delete
```

---

## Idempotency & Exactly-Once Semantics

### Idempotency Key Tracking
- Store `idempotency_key` in event payload
- Use as deduplication key in consumers
- Retention: 24 hours in database

### Delivery Guarantees
- **Booking Service:** At-least-once (idempotency keys prevent duplicates)
- **Payment Service:** Exactly-once (via saga pattern + DB transactions)
- **Notification Service:** At-least-once (retry with max 3 attempts)
- **Analytics Service:** At-least-once (eventual consistency acceptable)

---

## Kafka Configuration Best Practices

```properties
# Producer Configuration
acks=all
retries=3
retry.backoff.ms=100
compression.type=snappy
linger.ms=10

# Consumer Configuration
fetch.min.bytes=1024
fetch.max.wait.ms=500
max.poll.records=500
session.timeout.ms=30000
heartbeat.interval.ms=10000
```

---

**END OF KAFKA EVENTS SCHEMA**

