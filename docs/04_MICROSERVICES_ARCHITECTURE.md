# TicketHub Microservices Architecture & Implementation Guide

**Version:** 1.0  
**Date:** 2026-04-21  
**Deployment Model:** Kubernetes (containerized microservices)

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Service Boundaries](#service-boundaries)
3. [Technology Stack](#technology-stack)
4. [Service-to-Service Communication](#service-to-service-communication)
5. [Data Management](#data-management)
6. [Deployment & Scaling](#deployment--scaling)
7. [Monitoring & Observability](#monitoring--observability)
8. [Security & API Gateway](#security--api-gateway)

---

## Architecture Overview

### Monolithic to Microservices Evolution

**Phase 1 (Initial - Months 1-2):** Monolithic Spring Boot application with clear package boundaries
**Phase 2 (Mid - Months 3-6):** Progressive extraction to microservices
**Phase 3 (Advanced - Months 7-12):** Full microservices with service mesh

### Current Architecture (Phase 1-2)

```
┌─────────────────────────────────────────────────────────────────┐
│                         API GATEWAY (Kong)                       │
│  - Authentication (JWT validation)                               │
│  - Rate Limiting                                                 │
│  - Request/Response logging                                      │
│  - SSL Termination                                               │
└────────────────────────────────────────────────────────────────┬┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
    ┌───▼──────┐  ┌────────▼────────┐  ┌──────▼─────┐
    │  Auth    │  │ Event Service   │  │  Booking   │
    │ Service  │  │                 │  │  Service   │
    │          │  │ - CRUD Events   │  │            │
    │ - Login  │  │ - Seat Map      │  │ - Create   │
    │ - JWT    │  │ - Status Mgmt   │  │ - Lock     │
    │ - RBAC   │  │                 │  │ - History  │
    └────┬─────┘  └────────┬────────┘  └──────┬─────┘
         │                 │                   │
         │      ┌──────────┼──────────┬────────┼──────────┐
         │      │          │         │        │          │
         │  ┌───▼──────┐  │   ┌─────▼──┐  ┌──▼─────┐ ┌──▼──────┐
         │  │ Payment  │  │   │Ticket  │  │Check- │ │Notifi-  │
         │  │ Service  │  │   │Service  │  │in     │ │cation   │
         │  │          │  │   │         │  │Service│ │Service  │
         │  └──────────┘  │   └────────┘  └───────┘ └─────────┘
         │                │
         └────────┬───────┴──────────────────────────┐
                  │                                  │
                  ▼                                  ▼
            ┌──────────────┐              ┌──────────────────┐
            │ PostgreSQL   │              │ Kafka Message    │
            │ (Shared DB)  │              │ Queue (Events)   │
            └──────────────┘              └──────────────────┘
                  ▲                                  │
                  │           ┌─────────────────────┘
                  │           │
                  └─────┬─────▼────────┐
                        │              │
                    ┌───▼──────┐  ┌───▼──────┐
                    │ Redis    │  │ Elasticsearch│
                    │ (Cache)  │  │ (Logging)    │
                    └──────────┘  └──────────┘
```

---

## Service Boundaries

### 1. Auth Service
**Responsibility:** User identity, authentication, authorization

**Database Tables:**
- users
- roles
- user_sessions

**APIs:**
- POST /auth/register
- POST /auth/login
- POST /auth/logout
- POST /auth/verify-otp

**Dependencies:** None (independent)

**Deployment:**
- Replicas: 2-3
- Memory: 512MB
- CPU: 250m

---

### 2. Event Service
**Responsibility:** Event lifecycle, seat management, event discovery

**Database Tables:**
- events
- event_locations
- event_ticket_types
- event_discounts
- seat_maps
- seats

**APIs:**
- GET /events (search, filter, paginate)
- GET /events/{id}
- GET /events/{id}/seats (WebSocket support)
- POST /events (create, organizer only)
- PUT /events/{id} (edit, organizer only)
- POST /events/{id}/approve (admin only)

**Internal APIs (for other services):**
- GET /internal/events/{id}/details
- GET /internal/seats/{seatId}/status
- PUT /internal/seats/{seatId}/lock
- PUT /internal/seats/{seatId}/unlock

**Events Published:**
- event.created
- event.approved
- event.cancelled

**Events Consumed:**
- payment.refunded (to unlock seats)
- booking.cancelled (to unlock seats)

**Dependencies:**
- Auth Service (verify JWT)
- PostgreSQL (event data)
- Redis (seat lock cache)

**Deployment:**
- Replicas: 3-5 (high traffic)
- Memory: 1GB
- CPU: 500m
- Horizontal scaling: CPU > 70%

---

### 3. Booking Service
**Responsibility:** Booking creation, seat locking, booking lifecycle

**Database Tables:**
- bookings
- booking_seats

**APIs:**
- POST /bookings (create booking)
- GET /bookings/{id}
- GET /users/bookings (list user's bookings)
- POST /bookings/{id}/cancel

**Internal APIs:**
- POST /internal/bookings/{id}/lock-seats
- POST /internal/bookings/{id}/release-seats
- GET /internal/bookings/{id}/seats-status

**Events Published:**
- booking.created
- booking.payment_initiated
- booking.completed
- booking.cancelled

**Events Consumed:**
- payment.captured (for completing booking)
- payment.failed (for releasing locks)

**Dependencies:**
- Auth Service
- Event Service
- Seat Service (indirect via events)
- PostgreSQL
- Redis (distributed locks)
- Kafka

**Deployment:**
- Replicas: 3-5
- Memory: 1.5GB
- CPU: 500m
- Sticky sessions: Yes (for lock contention)

---

### 4. Seat Service (Phase 2)
**Responsibility:** Seat locking, availability, real-time updates

**Data Source:** Event Service (shared table access initially)

**Internal APIs:**
- POST /internal/lock-seat
- POST /internal/unlock-seat
- GET /internal/seat-status
- POST /internal/bulk-unlock

**Events Published:**
- seat.locked
- seat.unlocked
- seat.booked

**Events Consumed:**
- booking.created
- payment.failed

**WebSocket Connections:**
- wss://api.tickethub.io/ws/events/{id}/seats
- Update payload: {seat_id, status, locked_until}

**Implementation Details:**
- Redlock for distributed locks (3-node Redis cluster)
- TTL: 300 seconds (configurable per event)
- Auto-unlock job every 60 seconds
- Fallback to optimistic locking if Redis down

**Deployment:**
- Replicas: 3
- Memory: 2GB (for WebSocket connections)
- CPU: 1000m

---

### 5. Payment Service
**Responsibility:** Payment processing, refund management, gateway integration

**Database Tables:**
- payments
- refunds

**APIs:**
- POST /payments/initiate
- GET /payments/{id}
- POST /refunds
- GET /refunds/{id}

**Webhook Endpoints:**
- POST /payments/webhook/vnpay
- POST /payments/webhook/momo
- POST /payments/webhook/stripe

**Internal APIs:**
- POST /internal/payments/{id}/capture
- POST /internal/payments/{id}/refund
- GET /internal/payments/{id}/status

**Events Published:**
- payment.initiated
- payment.authorized
- payment.captured
- payment.failed
- payment.refunded

**Events Consumed:**
- booking.created
- booking.cancelled

**Payment Gateway Integration:**
```
VNPAY:
  - API Endpoint: https://sandbox.vnpayment.vn/paymentv3/querydr/
  - Webhook Signature: HMAC-SHA512
  - Timeout: 30 seconds
  - Retry: 3x with exponential backoff

MoMo:
  - API Endpoint: https://test-payment.momo.vn/v3/gateway/api/create
  - Webhook Signature: HMAC-SHA256
  - Timeout: 30 seconds
```

**PCI Compliance:**
- Never store card numbers (use tokenization)
- SSL pinning for card data transmission
- 3D Secure authentication

**Deployment:**
- Replicas: 2-3
- Memory: 1GB
- CPU: 500m
- Network: Isolated VPC for payment gateway communication

---

### 6. Ticket Service
**Responsibility:** E-ticket generation, QR code management

**Database Tables:**
- tickets
- qr_code_generations

**APIs:**
- GET /tickets/{id}
- GET /users/tickets
- POST /tickets/{id}/download (PDF)

**Internal APIs:**
- POST /internal/generate-ticket
- POST /internal/cancel-ticket
- GET /internal/tickets/batch

**Events Published:**
- ticket.generated
- ticket.checked_in
- ticket.cancelled

**Events Consumed:**
- payment.captured
- booking.cancelled
- refund.completed

**QR Code Generation:**
```
QR Data Structure:
{
  "ticket_id": 10001,
  "ticket_code": "TKT_10001_XYZ",
  "booking_code": "BOOK_5001_ABCDE",
  "event_id": 1,
  "seat_number": "A1",
  "customer_id": 123,
  "check_in_url": "https://checkin.tickethub.io/verify/...",
  "timestamp": "2026-04-21T10:05:30Z",
  "expires_at": "2026-05-16T02:00:00Z",
  "signature": "HMAC_SIGNATURE"
}

Encoding: Base64(JSON) -> ZXhMvK...
```

**Deployment:**
- Replicas: 2
- Memory: 512MB
- CPU: 250m

---

### 7. Check-in Service
**Responsibility:** Event check-in, staff management, attendance tracking

**Database Tables:**
- staff_assignments
- check_ins
- audit_logs

**APIs:**
- POST /checkins
- POST /checkins/bulk
- GET /checkins/stats
- GET /checkins/history

**Internal APIs:**
- POST /internal/validate-ticket
- POST /internal/mark-checked-in

**Events Published:**
- ticket.checked_in
- checkin.failed

**Events Consumed:**
- ticket.generated
- event.cancelled

**Offline Mode:**
- SQLite cache on staff mobile device
- Sync queue when reconnected
- Conflict resolution (manual review)

**Deployment:**
- Replicas: 2-3
- Memory: 512MB
- CPU: 250m
- Mobile App: Native (iOS/Android)

---

### 8. Notification Service
**Responsibility:** Email, SMS, push notifications

**Database Tables:**
- email_notifications
- web_push_subscriptions

**APIs:**
- POST /notifications/subscribe (Web Push)
- GET /notifications
- DELETE /notifications/{id}

**Event Consumers:**
- booking.created → send confirmation email
- ticket.generated → send ticket email
- ticket.checked_in → send confirmation
- payment.failed → send retry notification
- event.cancelled → send refund notification

**Notification Channels:**
- Email (SMTP, SendGrid, or SES)
- Push Notifications (FCM, APNs)
- SMS (Twilio or local provider)

**Configuration:**
```
Email Templates:
  - booking_confirmation
  - ticket_issued
  - event_reminder_48h
  - event_reminder_24h
  - event_reminder_1h
  - checkin_confirmation
  - payment_failed
  - refund_notification
  - event_cancelled

Retry Policy:
  - Max 3 retries
  - Exponential backoff: 1m, 5m, 15m
  - Logging on all failures
```

**Deployment:**
- Replicas: 2
- Memory: 512MB
- CPU: 250m
- External connections: Email SMTP, FCM, Twilio

---

### 9. Analytics Service
**Responsibility:** Event analytics, user behavior tracking, reporting

**Database Tables:**
- event_analytics
- system_analytics
- user_activity_log

**APIs:**
- GET /analytics/events-summary
- GET /analytics/event/{id}/stats
- GET /analytics/system-health
- GET /analytics/reports (admin)

**Event Consumers:**
- booking.created
- payment.captured
- ticket.checked_in
- event.view (from mobile/web)
- bot.detected

**Real-Time Dashboards:**
```
Event Dashboard:
  - Live occupancy %
  - Revenue stream
  - Booking rate (tickets/hour)
  - Cancellation rate
  - Top seats (most sold)

System Dashboard:
  - Active users
  - Total bookings
  - Payment success rate
  - Bot detections
  - Server health
```

**Deployment:**
- Replicas: 1-2
- Memory: 1GB
- CPU: 500m
- Data storage: Time-series DB (InfluxDB or TimescaleDB)

---

### 10. Admin Service
**Responsibility:** System administration, event approval, organizer management

**Database Access:** All tables (read/write)

**APIs:**
- POST /admin/events/{id}/approve-reject
- PUT /admin/organizers/{id}/status
- GET /admin/reports/events
- GET /admin/reports/payments
- GET /admin/reports/users
- POST /admin/system/config

**Internal APIs:**
- POST /internal/approve-event
- POST /internal/suspend-organizer
- POST /internal/flag-fraud

**Deployment:**
- Replicas: 1
- Memory: 512MB
- CPU: 250m
- Access: Admin personnel only

---

## Technology Stack

### Backend Framework
- **Language:** Java 17+
- **Framework:** Spring Boot 3.x
- **Build Tool:** Maven
- **Package Management:** Maven Central

### Databases
| Service | Primary | Cache | Search |
|---------|---------|-------|--------|
| Auth | PostgreSQL 15 | Redis | - |
| Event | PostgreSQL 15 | Redis | Elasticsearch 8 |
| Booking | PostgreSQL 15 | Redis | - |
| Payment | PostgreSQL 15 | Redis | - |
| Ticket | PostgreSQL 15 | Redis | - |
| Check-in | PostgreSQL 15 | Local SQLite (mobile) | - |
| Analytics | TimescaleDB 2 | Redis | - |

### Message Queue
- **Primary:** Apache Kafka 3.x
- **Configuration:** 3-node cluster, replication factor 3, min.insync.replicas=2

### Caching
- **Primary:** Redis 7.x
- **Type:** Standalone for dev, Sentinel for staging, Cluster for prod
- **Eviction Policy:** allkeys-lru
- **Persistence:** AOF (Append-Only File)

### Logging & Monitoring
- **Log Aggregation:** ELK Stack (Elasticsearch, Logstash, Kibana)
- **Metrics:** Prometheus 2.x scraping every 15s
- **Visualization:** Grafana 9.x
- **Distributed Tracing:** Jaeger 1.x (optional Phase 2)
- **Alerting:** AlertManager or PagerDuty

### Container & Orchestration
- **Containerization:** Docker 20.x
- **Orchestration:** Kubernetes 1.24+
- **Registry:** Docker Hub or ECR
- **Deployment:** ArgoCD or Flux (GitOps)

### API Gateway
- **Primary:** Kong 3.x or AWS API Gateway
- **Authentication:** OAuth2 / OpenID Connect (integrated with Auth Service)
- **Rate Limiting:** Token bucket algorithm
- **Plugins:** CORS, Logging, Authentication

### Development Tools
- **IDE:** JetBrains IntelliJ IDEA
- **Version Control:** Git
- **CI/CD:** GitHub Actions or Jenkins
- **Code Quality:** SonarQube
- **API Documentation:** Swagger/OpenAPI

---

## Service-to-Service Communication

### Synchronous (REST/HTTP)

#### Auth Service → All Services
```
Headers:
  Authorization: Bearer {JWT}
  X-Request-ID: {UUID}
  X-Trace-ID: {UUID}

Common Endpoints:
  GET /auth/verify-token (internal)
  GET /auth/check-permission (internal)
```

#### Event Service → Booking Service
```
Event Service calling Booking Service:
  GET /internal/bookings/{id}/seats-status
  Response indicates which seats are locked
```

### Asynchronous (Kafka)

#### Message Flow Examples

**Booking Flow:**
```
1. Customer creates booking
   → BookingService publishes event: booking.created
   
2. TicketService listens to booking.created
   → Generates ticket
   → Publishes: ticket.generated
   
3. NotificationService listens to booking.created
   → Sends confirmation email
   
4. AnalyticsService listens to booking.created
   → Updates real-time occupancy
```

**Payment Flow:**
```
1. Customer initiates payment
   → PaymentService publishes: payment.initiated
   
2. Customer completes payment (webhook from gateway)
   → PaymentService publishes: payment.captured
   
3. BookingService listens to payment.captured
   → Updates booking status to COMPLETED
   → Publishes: booking.completed
   
4. TicketService listens to booking.completed
   → Issues e-ticket
   
5. SeatService listens to booking.completed
   → Marks seats as SOLD permanently
```

### Shared Database (for now, within monolith)
- All services share PostgreSQL instance
- No direct DB connections between services
- Services access only "their" tables
- Phase 2: Each service gets own DB + event-based sync

---

## Data Management

### Database Schemas

**Approach:** Single PostgreSQL with separate schemas per service

```sql
-- Auth Service
CREATE SCHEMA auth;
CREATE TABLE auth.users (...);
CREATE TABLE auth.roles (...);
CREATE TABLE auth.user_sessions (...);

-- Event Service
CREATE SCHEMA events;
CREATE TABLE events.events (...);
CREATE TABLE events.locations (...);
CREATE TABLE events.seats (...);

-- Booking Service
CREATE SCHEMA bookings;
CREATE TABLE bookings.bookings (...);
CREATE TABLE bookings.booking_seats (...);

-- Payment Service
CREATE SCHEMA payments;
CREATE TABLE payments.payments (...);
CREATE TABLE payments.refunds (...);

-- Ticket Service
CREATE SCHEMA tickets;
CREATE TABLE tickets.tickets (...);
CREATE TABLE tickets.qr_code_generations (...);

-- Check-in Service
CREATE SCHEMA checkins;
CREATE TABLE checkins.staff_assignments (...);
CREATE TABLE checkins.check_ins (...);

-- Analytics Service
CREATE SCHEMA analytics;
CREATE TABLE analytics.event_analytics (...);
CREATE TABLE analytics.system_analytics (...);
```

### Transactions & Consistency

**Local Transactions (single service):**
```java
@Transactional
public void createBooking(BookingRequest request) {
    Booking booking = new Booking(...);
    bookingRepository.save(booking);
    
    for (SeatId seat : request.getSeats()) {
        bookingSeatRepository.save(new BookingSeat(booking, seat));
    }
    // Single ACID transaction
}
```

**Distributed Transactions (saga pattern):**
```
Booking Saga:

1. Start: BookingService creates booking (status=PENDING)
   → COMMITTED to DB

2. Call PaymentService: process payment
   → If SUCCESS: continue
   → If FAIL: compensate (step 5)

3. Call SeatService: mark seats BOOKED
   → If SUCCESS: continue
   → If FAIL: compensate (step 4)

4. Call TicketService: issue ticket
   → If SUCCESS: BookingService sets status=COMPLETED
   → If FAIL: compensate (step 3)

5. Compensations:
   - SeatService: revert seats to AVAILABLE
   - PaymentService: refund customer
```

### Distributed Locking (Seat Locks)

```java
// Redis Redlock implementation
public LockResult lockSeat(long seatId, int durationSeconds) {
    String lockKey = "seat:" + seatId;
    String lockValue = UUID.randomUUID().toString();
    
    // Try to acquire lock
    boolean locked = redisOps.setIfAbsent(
        lockKey,
        lockValue,
        Duration.ofSeconds(durationSeconds)
    );
    
    if (locked) {
        return LockResult.success(lockKey, lockValue);
    } else {
        return LockResult.failed("Already locked");
    }
}

// Release lock (with ownership verification)
public void unlockSeat(String lockKey, String lockValue) {
    String script = """
        if redis.call('get', KEYS[1]) == ARGV[1] then
            return redis.call('del', KEYS[1])
        else
            return 0
        end
    """;
    
    redisTemplate.execute(
        RedisScript.of(script),
        List.of(lockKey),
        lockValue
    );
}
```

### Eventual Consistency

For analytics and reporting:
- Updates are published asynchronously to Kafka
- Consumed by analytics service
- Data eventual consistency acceptable (max 1 minute delay)
- Real-time booking data maintained separately

---

## Deployment & Scaling

### Kubernetes Resources

**Namespace:** `tickethub-production`

#### Auth Service Deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
  namespace: tickethub-production
spec:
  replicas: 2
  selector:
    matchLabels:
      app: auth-service
  template:
    metadata:
      labels:
        app: auth-service
    spec:
      containers:
      - name: auth-service
        image: tickethub/auth-service:1.0.0
        ports:
        - containerPort: 8081
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8081
          initialDelaySeconds: 20
          periodSeconds: 5
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: DB_HOST
          value: postgres.tickethub.svc.cluster.local
        - name: DB_PORT
          value: "5432"
        - name: REDIS_HOST
          value: redis-cluster.tickethub.svc.cluster.local
```

### Horizontal Pod Autoscaling

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: booking-service-hpa
  namespace: tickethub-production
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: booking-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 30
      policies:
      - type: Percent
        value: 100
        periodSeconds: 30
```

### Load Balancing

**AWS ELB / ALB Configuration:**
```
Listener: port 443 (HTTPS) → Target Group on port 8080
Health Check: GET /actuator/health every 10 seconds
Unhealthy Threshold: 3
Healthy Threshold: 2
Timeout: 5 seconds
Connection Draining: 30 seconds
Sticky Sessions: 1 day (for booking service only)
```

### Database Scaling

**PostgreSQL High Availability:**
```
Primary: db-primary.tickethub.rds.amazonaws.com:5432
Read Replicas:
  - db-replica-1.tickethub.rds.amazonaws.com
  - db-replica-2.tickethub.rds.amazonaws.com

Connection Pooling (per service):
  Max Connections: 20
  Min Connections: 5
  Connection Timeout: 30 seconds
  Idle Timeout: 10 minutes
```

### Backup & Disaster Recovery

```
Daily Backups: 02:00 UTC
Retention: 30 days
Cross-region Replication: Every 6 hours
RTO: 5 minutes
RPO: 5 minutes

Restore Procedure:
1. Identify recovery point
2. Restore snapshot to new RDS instance
3. Update application connection string
4. Verify data integrity
5. Cut over traffic
```

---

## Monitoring & Observability

### Metrics Collection

**Prometheus Scrape Configs:**
```yaml
scrape_configs:
  - job_name: 'tickethub-services'
    scrape_interval: 15s
    static_configs:
      - targets:
        - 'auth-service:8081/actuator/prometheus'
        - 'event-service:8082/actuator/prometheus'
        - 'booking-service:8083/actuator/prometheus'
        - 'payment-service:8084/actuator/prometheus'
        - 'ticket-service:8085/actuator/prometheus'
```

**Key Metrics:**
```
# HTTP Metrics
http_requests_total{method, status, endpoint}
http_request_duration_seconds{quantile, method, endpoint}
http_requests_in_progress{method, endpoint}

# Business Metrics
bookings_created_total
bookings_cancelled_total
payments_processed_total
tickets_generated_total
seat_locks_acquired_total

# System Metrics
jvm_memory_used_bytes
jvm_gc_pause_seconds_sum
process_cpu_seconds_total
process_resident_memory_bytes

# Database Metrics
db_connection_pool_active
db_connection_pool_pending_requests
db_query_duration_seconds
```

### Alerts

```yaml
groups:
- name: TicketHub Alerts
  rules:
  - alert: HighErrorRate
    expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.01
    for: 5m
    annotations:
      summary: "High error rate detected"
  
  - alert: BookingServiceDown
    expr: up{job="booking-service"} == 0
    for: 2m
    annotations:
      summary: "Booking Service is down"
  
  - alert: PaymentGatewayLatency
    expr: histogram_quantile(0.95, payment_gateway_latency_seconds) > 5
    for: 5m
    annotations:
      summary: "Payment gateway latency high"
  
  - alert: DatabaseConnectionPoolExhausted
    expr: db_connection_pool_active / db_connection_pool_max > 0.9
    for: 2m
    annotations:
      summary: "DB connection pool nearly exhausted"
```

### Logging

**ELK Stack Configuration:**
```
Logstash Pipeline:
  Input: Filebeat from containers
  Filter: JSON parsing, field extraction
  Output: Elasticsearch

Log Format (JSON):
{
  "timestamp": "2026-04-21T10:00:00Z",
  "level": "INFO",
  "service": "booking-service",
  "trace_id": "550e8400-e29b-41d4-a716-446655440000",
  "span_id": "a2f87c0b-1e8e-4f7f-8b2a",
  "message": "Booking created successfully",
  "user_id": 123,
  "booking_id": 5001,
  "duration_ms": 145
}
```

---

## Security & API Gateway

### API Gateway Setup (Kong)

```yaml
# Kong Configuration
---
_format_version: '2.1'
version: TicketHub v1

services:
  - name: auth-service
    url: http://auth-service:8081
    routes:
      - name: auth-login
        paths: ["/auth/login"]
        methods: ["POST"]
        plugins:
          - name: rate-limiting
            config:
              minute: 20
              policy: local
      - name: auth-register
        paths: ["/auth/register"]
        methods: ["POST"]
        plugins:
          - name: rate-limiting
            config:
              minute: 5
              policy: local

  - name: booking-service
    url: http://booking-service:8083
    routes:
      - name: create-booking
        paths: ["/bookings"]
        methods: ["POST"]
        plugins:
          - name: jwt
            config:
              header_names: ["Authorization"]
          - name: rate-limiting
            config:
              minute: 50
              policy: redis
              redis_host: redis
              redis_port: 6379
          - name: request-transformer
            config:
              add:
                headers:
                  - X-Consumer-ID:$(var.consumer_id)
```

### JWT Token Validation

```java
@Component
public class JwtTokenProvider {
    
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();
        return claims.getSubject();
    }
}
```

### Rate Limiting Rules

```
Per IP Address:
  - Global: 100 requests/minute
  - Auth endpoints: 20 requests/minute
  - Payment endpoints: 10 requests/minute

Per User Account:
  - Global: 1000 requests/hour
  - Booking endpoints: 50 requests/minute
  - Payment endpoints: 10 requests/minute
  - Check-in endpoints: Unlimited (staff)

Retry-After Header:
  - Value: Retry-After: 60 (seconds)
  - Applied when rate limit exceeded
```

---

## Production Readiness Checklist

- [ ] All services have health check endpoints
- [ ] Database migrations tested and versioned
- [ ] Secrets injected via environment variables (not in source)
- [ ] All services have graceful shutdown handlers
- [ ] Circuit breakers configured for external calls
- [ ] Distributed tracing integrated
- [ ] Log aggregation working
- [ ] Metrics collection verified
- [ ] Alerts configured and tested
- [ ] Disaster recovery plan documented
- [ ] Load testing completed (5000+ concurrent users)
- [ ] Security scan passed (OWASP Top 10)
- [ ] API documentation generated (Swagger/OpenAPI)
- [ ] Runbook created for operational team
- [ ] On-call rotation established

---

**END OF MICROSERVICES ARCHITECTURE DOCUMENT**

