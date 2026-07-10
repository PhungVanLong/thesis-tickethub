# TicketHub API Contracts & Specifications

**Version:** 1.0  
**Date:** 2026-04-21  
**Status:** Draft

---

## Table of Contents

1. [API Overview](#api-overview)
2. [Authentication & Authorization](#authentication--authorization)
3. [Common Response Format](#common-response-format)
4. [Rate Limiting & Security Headers](#rate-limiting--security-headers)
5. [Auth Service APIs](#auth-service-apis)
6. [Event Service APIs](#event-service-apis)
7. [Booking Service APIs](#booking-service-apis)
8. [Payment Service APIs](#payment-service-apis)
9. [Ticket Service APIs](#ticket-service-apis)
10. [Check-in Service APIs](#check-in-service-apis)
11. [Notification Service APIs](#notification-service-apis)
12. [Admin Service APIs](#admin-service-apis)

---

## API Overview

### Base URL
```
Production: https://api.tickethub.io/v1
Staging: https://staging-api.tickethub.io/v1
Development: http://localhost:8080/api/v1
```

### API Gateway
- **Entry Point:** Kong API Gateway (or AWS API Gateway)
- **Protocol:** HTTPS/TLS 1.3
- **Authentication:** JWT Bearer Token
- **Content-Type:** application/json

### SLA Targets
| Endpoint | Target (P95) | Target (P99) | Timeout |
|----------|--------------|--------------|---------|
| GET /events | 200ms | 500ms | 5s |
| GET /seats | 200ms | 500ms | 5s |
| POST /lock-seat | 300ms | 800ms | 10s |
| POST /checkout | 500ms | 1500ms | 15s |
| POST /checkin | 100ms | 300ms | 5s |

---

## Authentication & Authorization

### JWT Token Structure

```json
{
  "headers": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user_id_12345",
    "email": "user@example.com",
    "role": "CUSTOMER",
    "permissions": ["booking:read", "booking:create", "payment:read"],
    "iat": 1703001234,
    "exp": 1703004834,
    "iss": "tickethub.io"
  }
}
```

### Token Endpoints

#### POST /auth/register
Register new user account

**Request:**
```json
{
  "email": "customer@example.com",
  "password": "SecurePassword123!",
  "first_name": "John",
  "last_name": "Doe",
  "phone": "+84912345678",
  "role": "CUSTOMER"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "user_id": 12345,
    "email": "customer@example.com",
    "role": "CUSTOMER",
    "created_at": "2026-04-21T10:00:00Z"
  }
}
```

#### POST /auth/login
Generate access and refresh tokens

**Request:**
```json
{
  "email": "customer@example.com",
  "password": "SecurePassword123!"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIs...",
    "refresh_token": "eyJhbGciOiJIUzI1NiIs...",
    "token_type": "Bearer",
    "expires_in": 900,
    "user": {
      "user_id": 12345,
      "email": "customer@example.com",
      "role": "CUSTOMER"
    }
  }
}
```

#### POST /auth/refresh
Refresh access token using refresh token

**Request:**
```json
{
  "refresh_token": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIs...",
    "expires_in": 900
  }
}
```

#### POST /auth/logout
Invalidate refresh token

**Response (204 No Content)**

---

## Common Response Format

### Success Response
```json
{
  "success": true,
  "code": "SUCCESS_CODE",
  "message": "Operation completed successfully",
  "data": {
    "id": 12345,
    "name": "Event Name"
  },
  "timestamp": "2026-04-21T10:00:00Z",
  "path": "/api/v1/events/1",
  "trace_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Error Response
```json
{
  "success": false,
  "code": "ERROR_CODE",
  "message": "Error description",
  "errors": [
    {
      "field": "email",
      "message": "Email is invalid"
    }
  ],
  "timestamp": "2026-04-21T10:00:00Z",
  "path": "/api/v1/events/1",
  "trace_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Pagination Response
```json
{
  "success": true,
  "data": [
    { "id": 1, "name": "Event 1" },
    { "id": 2, "name": "Event 2" }
  ],
  "pagination": {
    "current_page": 1,
    "page_size": 20,
    "total_items": 150,
    "total_pages": 8,
    "has_next": true,
    "has_previous": false
  }
}
```

---

## Rate Limiting & Security Headers

### Rate Limiting Headers
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1626381600
X-RateLimit-Retry-After: 5
```

### Rate Limit Rules
- **Per IP Address:** 100 requests/minute
- **Per User Account:** 1000 requests/hour
- **Per API Method:**
  - Booking endpoints: 50 requests/minute per user
  - Payment endpoints: 10 requests/minute per user
  - Check-in endpoints: Unlimited (internal staff)

### Security Headers
```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000; includeSubDomains
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'
```

### Anti-Bot Headers
```
X-Request-ID: <uuid>
X-Client-Fingerprint: <device_fingerprint>
X-Load-Test: <HMAC_TOKEN> (for load testing only, requires whitelist IP)
```

---

## Auth Service APIs

### POST /auth/register
- **Description:** Register new user
- **Auth:** None
- **Rate Limit:** 5/hour per IP
- **Roles:** Public

### POST /auth/login
- **Description:** User login
- **Auth:** None
- **Rate Limit:** 20/hour per IP
- **Roles:** Public

### POST /auth/logout
- **Description:** Logout user
- **Auth:** Required (JWT)
- **Rate Limit:** 100/hour
- **Roles:** CUSTOMER, ORGANIZER, STAFF, ADMIN

### POST /auth/verify-otp
- **Description:** Verify OTP for 2FA
- **Auth:** Required (JWT)
- **Request:**
  ```json
  {
    "otp": "123456"
  }
  ```
- **Response:** Same as login response

---

## Event Service APIs

### GET /events
List all events with filters

**Query Parameters:**
```
page: int (default: 1)
page_size: int (default: 20, max: 100)
search: string (search by title, location)
category: string (filter by category)
price_min: decimal
price_max: decimal
status: enum (APPROVED, ACTIVE, CANCELLED)
start_date: ISO8601 (filter by start date from)
end_date: ISO8601 (filter by start date to)
location: string (filter by location)
sort: string (startDate, title, popularity, price)
sort_direction: asc|desc
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "event_id": 1,
      "title": "Concert 2026",
      "description": "Grand concert event",
      "poster_url": "https://cdn.tickethub.io/posters/1.jpg",
      "category": "MUSIC",
      "status": "ACTIVE",
      "start_at": "2026-05-15T20:00:00Z",
      "end_at": "2026-05-16T02:00:00Z",
      "location": {
        "name": "National Stadium",
        "city": "Ho Chi Minh",
        "country": "Vietnam"
      },
      "ticket_types": [
        {
          "ticket_type_id": 1,
          "name": "VIP",
          "price": 100.00,
          "quantity_available": 50
        },
        {
          "ticket_type_id": 2,
          "name": "Standard",
          "price": 50.00,
          "quantity_available": 200
        }
      ]
    }
  ],
  "pagination": {
    "current_page": 1,
    "total_pages": 5,
    "total_items": 100
  }
}
```

### GET /events/{event_id}
Get event details

**Path Parameters:**
- `event_id`: int

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "event_id": 1,
    "title": "Concert 2026",
    "description": "Grand concert event",
    "poster_url": "https://cdn.tickethub.io/posters/1.jpg",
    "category": "MUSIC",
    "status": "ACTIVE",
    "start_at": "2026-05-15T20:00:00Z",
    "end_at": "2026-05-16T02:00:00Z",
    "location": {
      "location_id": 1,
      "name": "National Stadium",
      "city": "Ho Chi Minh",
      "country": "Vietnam",
      "latitude": 10.7769,
      "longitude": 106.6955
    },
    "organizer": {
      "organizer_id": 5,
      "company_name": "Dream Events",
      "rating": 4.8
    },
    "ticket_types": [
      {
        "ticket_type_id": 1,
        "name": "VIP",
        "price": 100.00,
        "quantity_total": 100,
        "quantity_available": 50,
        "quantity_sold": 40,
        "quantity_locked": 10,
        "start_sale_at": "2026-04-01T00:00:00Z",
        "end_sale_at": "2026-05-14T23:59:59Z"
      }
    ],
    "total_capacity": 5000,
    "current_occupancy": 2450,
    "occupancy_percentage": 49
  }
}
```

### GET /events/{event_id}/seats
Get real-time seat map (WebSocket support)

**HTTP Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "seat_map_id": 1,
    "event_id": 1,
    "total_seats": 1000,
    "rows": 20,
    "cols": 50,
    "seats": [
      {
        "seat_id": 1001,
        "seat_number": "A1",
        "seat_type": "NORMAL",
        "status": "AVAILABLE",
        "ticket_type_id": 2,
        "price": 50.00
      },
      {
        "seat_id": 1002,
        "seat_number": "A2",
        "seat_type": "NORMAL",
        "status": "LOCKED",
        "locked_until": "2026-04-21T10:05:00Z"
      },
      {
        "seat_id": 1003,
        "seat_number": "A3",
        "seat_type": "VIP",
        "status": "BOOKED",
        "ticket_type_id": 1,
        "price": 100.00
      }
    ]
  }
}
```

**WebSocket Connection:**
```
wss://api.tickethub.io/ws/events/{event_id}/seats?token=<JWT>

Events published:
{
  "type": "seat.updated",
  "data": {
    "seat_id": 1002,
    "seat_number": "A2",
    "status": "AVAILABLE",
    "timestamp": "2026-04-21T10:05:15Z"
  }
}
```

### POST /events
Create new event (Organizer only)

**Auth:** Required (JWT, role=ORGANIZER)
**Request:**
```json
{
  "title": "Concert 2026",
  "description": "Grand concert event",
  "category": "MUSIC",
  "location_id": 1,
  "start_at": "2026-05-15T20:00:00Z",
  "end_at": "2026-05-16T02:00:00Z",
  "total_capacity": 5000,
  "poster_url": "https://cdn.example.com/poster.jpg"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "event_id": 1,
    "status": "DRAFT",
    "created_at": "2026-04-21T10:00:00Z"
  }
}
```

---

## Booking Service APIs

### POST /events/{event_id}/seats/{seat_id}/lock
Lock seat (customer booking flow)

**Auth:** Required (JWT, role=CUSTOMER)
**Request:**
```json
{
  "idempotency_key": "550e8400-e29b-41d4-a716-446655440000",
  "quantity": 1
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "reservation_id": "LOCK_12345_ABCDE",
    "seat_id": 101,
    "seat_number": "A1",
    "status": "LOCKED",
    "locked_until": "2026-04-21T10:10:00Z",
    "expires_in_seconds": 300
  }
}
```

**Error Response (409 Conflict):**
```json
{
  "success": false,
  "code": "SEAT_ALREADY_LOCKED",
  "message": "Seat is already locked by another customer",
  "data": {
    "seat_id": 101,
    "locked_until": "2026-04-21T10:05:00Z"
  }
}
```

### POST /bookings
Create booking (before payment)

**Auth:** Required (JWT, role=CUSTOMER)
**Request:**
```json
{
  "event_id": 1,
  "seats": [101, 102, 103],
  "discount_code": "EARLY_BIRD_20",
  "idempotency_key": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "booking_id": 5001,
    "booking_code": "BOOK_5001_ABCDE",
    "event_id": 1,
    "seats": [101, 102, 103],
    "quantity": 3,
    "original_price": 150.00,
    "discount_amount": 30.00,
    "total_price": 120.00,
    "commission": 6.00,
    "status": "PENDING",
    "expires_at": "2026-04-21T10:10:00Z",
    "created_at": "2026-04-21T10:00:00Z"
  }
}
```

### GET /bookings/{booking_id}
Get booking details

**Auth:** Required (JWT)
**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "booking_id": 5001,
    "booking_code": "BOOK_5001_ABCDE",
    "event_id": 1,
    "seats": [
      {
        "seat_id": 101,
        "seat_number": "A1",
        "price": 50.00
      }
    ],
    "status": "COMPLETED",
    "total_price": 120.00,
    "payment_status": "PAID",
    "created_at": "2026-04-21T10:00:00Z",
    "completed_at": "2026-04-21T10:05:00Z"
  }
}
```

### GET /users/bookings
Get user's booking history

**Auth:** Required (JWT)
**Query Parameters:**
```
page: int
status: enum (PENDING, COMPLETED, CANCELLED)
sort: createdAt|status
```

**Response:** Paginated list of bookings

---

## Payment Service APIs

### POST /payments/initiate
Initiate payment for booking

**Auth:** Required (JWT)
**Request:**
```json
{
  "booking_id": 5001,
  "payment_method": "VNPAY",
  "idempotency_key": "550e8400-e29b-41d4-a716-446655440000",
  "redirect_url": "https://tickethub.io/payment/success"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "payment_id": 9001,
    "gateway_url": "https://payment-gateway.vnpay.vn/pay?transaction=...",
    "transaction_id": "VNP_12345_ABCDE",
    "status": "INITIATED",
    "expires_at": "2026-04-21T10:15:00Z"
  }
}
```

### POST /payments/webhook/{provider}
Payment gateway webhook (VNPAY, MoMo)

**Auth:** HMAC signature verification
**Request:**
```json
{
  "transaction_id": "VNP_12345_ABCDE",
  "booking_id": 5001,
  "amount": 120.00,
  "status": "SUCCESS",
  "timestamp": "2026-04-21T10:05:30Z",
  "signature": "HMAC_SHA256_SIGNATURE"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Webhook processed successfully"
}
```

### GET /payments/{payment_id}
Get payment status

**Auth:** Required (JWT)
**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "payment_id": 9001,
    "booking_id": 5001,
    "amount": 120.00,
    "currency": "USD",
    "payment_method": "VNPAY",
    "status": "CAPTURED",
    "transaction_id": "VNP_12345_ABCDE",
    "paid_at": "2026-04-21T10:05:30Z",
    "created_at": "2026-04-21T10:00:00Z"
  }
}
```

### POST /refunds
Request refund

**Auth:** Required (JWT)
**Request:**
```json
{
  "payment_id": 9001,
  "reason": "CUSTOMER_REQUEST",
  "idempotency_key": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "refund_id": 8001,
    "payment_id": 9001,
    "amount": 96.00,
    "refund_policy_percent": 80,
    "status": "PENDING",
    "requested_at": "2026-04-21T10:00:00Z"
  }
}
```

---

## Ticket Service APIs

### GET /tickets/{ticket_id}
Get ticket details

**Auth:** Required (JWT)
**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "ticket_id": 10001,
    "booking_id": 5001,
    "ticket_code": "TKT_10001_XYZ",
    "seat_number": "A1",
    "event": {
      "event_id": 1,
      "title": "Concert 2026",
      "start_at": "2026-05-15T20:00:00Z"
    },
    "status": "ACTIVE",
    "qr_code_url": "https://cdn.tickethub.io/qr/10001.png",
    "qr_code_data": "{...signed_data...}",
    "created_at": "2026-04-21T10:05:00Z"
  }
}
```

### GET /users/tickets
Get user's tickets (paginated)

**Auth:** Required (JWT)
**Query Parameters:**
```
page: int
status: enum (ACTIVE, CHECKED_IN, CANCELLED)
event_id: int
```

**Response:** Paginated list of tickets

### POST /tickets/{ticket_id}/download
Download e-ticket as PDF

**Auth:** Required (JWT)
**Response:** PDF file

---

## Check-in Service APIs

### POST /checkins
Perform check-in (staff only)

**Auth:** Required (JWT, role=STAFF)
**Request:**
```json
{
  "ticket_code": "TKT_10001_XYZ",
  "device_id": "device_001",
  "location": {
    "latitude": 10.7769,
    "longitude": 106.6955
  },
  "notes": "Customer arrived"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "checkin_id": 7001,
    "ticket_id": 10001,
    "customer_name": "John Doe",
    "seat_number": "A1",
    "event_title": "Concert 2026",
    "status": "CHECKED_IN",
    "checked_in_at": "2026-05-15T20:15:30Z"
  }
}
```

### POST /checkins/bulk
Bulk check-in via file upload

**Auth:** Required (JWT, role=STAFF)
**Request:** Multipart form data with CSV file

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "total": 100,
    "successful": 98,
    "failed": 2,
    "failures": [
      {
        "ticket_code": "TKT_10002_XYZ",
        "error": "Ticket not found"
      }
    ]
  }
}
```

### GET /checkins/stats
Get event check-in statistics

**Auth:** Required (JWT)
**Query Parameters:**
```
event_id: int (required)
date: ISO8601
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "event_id": 1,
    "total_tickets": 2500,
    "checked_in": 1850,
    "not_checked_in": 650,
    "check_in_rate": 74.0,
    "no_show_rate": 26.0
  }
}
```

---

## Notification Service APIs

### GET /notifications
Get user notifications

**Auth:** Required (JWT)
**Query Parameters:**
```
page: int
type: enum (EMAIL, PUSH, SMS)
status: enum (PENDING, SENT, FAILED)
```

**Response:** Paginated list of notifications

### POST /notifications/subscribe
Subscribe to push notifications

**Auth:** Required (JWT)
**Request:**
```json
{
  "subscription": {
    "endpoint": "https://fcm.googleapis.com/...",
    "keys": {
      "p256dh": "...",
      "auth": "..."
    }
  }
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "subscription_id": 6001,
    "created_at": "2026-04-21T10:00:00Z"
  }
}
```

---

## Admin Service APIs

### POST /admin/events/{event_id}/approve
Approve event (admin only)

**Auth:** Required (JWT, role=ADMIN)
**Request:**
```json
{
  "status": "APPROVED",
  "notes": "Event approved successfully"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "event_id": 1,
    "status": "APPROVED",
    "approved_at": "2026-04-21T10:00:00Z"
  }
}
```

### GET /admin/reports/events
Get system events analytics

**Auth:** Required (JWT, role=ADMIN)
**Query Parameters:**
```
start_date: ISO8601
end_date: ISO8601
group_by: day|week|month
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "date_range": {
      "start": "2026-04-01",
      "end": "2026-04-30"
    },
    "total_events": 150,
    "total_revenue": 50000.00,
    "avg_occupancy": 75.5,
    "top_events": [...]
  }
}
```

---

## Error Codes Reference

| Code | HTTP Status | Description |
|------|------------|-------------|
| INVALID_REQUEST | 400 | Request parameters are invalid |
| UNAUTHORIZED | 401 | Authentication failed |
| FORBIDDEN | 403 | User lacks permission |
| NOT_FOUND | 404 | Resource not found |
| CONFLICT | 409 | Resource conflict (e.g., seat locked) |
| RATE_LIMIT_EXCEEDED | 429 | Rate limit exceeded |
| INTERNAL_ERROR | 500 | Server error |
| SERVICE_UNAVAILABLE | 503 | Service temporarily unavailable |
| SEAT_NOT_AVAILABLE | 409 | Seat is not available |
| BOOKING_EXPIRED | 410 | Booking has expired |
| PAYMENT_FAILED | 402 | Payment transaction failed |
| DUPLICATE_REQUEST | 409 | Duplicate request (idempotency) |

---

**END OF API CONTRACTS**

