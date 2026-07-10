# Management API Gateway Documentation

## 1) Scope
This workspace currently contains the `management` Spring Boot service and **not** a dedicated API Gateway module.

This document covers:
- the HTTP API exposed by the management service
- the business flow for event creation, event approval, RefUser sync, and organizer verification
- how the service is discovered through Eureka
- a suggested Spring Cloud Gateway route mapping

## 2) Service Overview
- **Service name:** `management`
- **Port:** `8082`
- **Base URL local:** `http://localhost:8082`
- **Discovery:** Eureka client enabled via `@EnableDiscoveryClient`
- **Service-level security:** current routes are `permitAll()` in `SecurityConfig`

## 3) Current API Surface

### 3.1 Create Event
- **Method:** `POST`
- **Path:** `/api/events/create`
- **Controller:** `EventController`
- **Success status:** `201 Created`

#### Request body
`CreateEventRequest`

Required fields:
- `organizerId` `Long`
- `title` `String`
- `startTime` `Instant`
- `endTime` `Instant`

Optional fields:
- `description`
- `venue`
- `city`
- `locationCoords`
- `bannerUrl`

#### Example request
```http
POST /api/events/create
Content-Type: application/json
```

```json
{
  "organizerId": 1,
  "title": "Tech Conference 2026",
  "description": "Annual community tech conference",
  "venue": "Convention Center",
  "city": "Ho Chi Minh City",
  "locationCoords": "10.7769,106.7009",
  "startTime": "2026-06-01T09:00:00Z",
  "endTime": "2026-06-01T17:00:00Z",
  "bannerUrl": "https://example.com/banner.png"
}
```

#### Business rules
- `organizerId` is required
- `title` is required
- `startTime` and `endTime` are required
- `startTime` must be before `endTime`
- if the organizer is missing in `RefUser`, the service syncs from `identity`
- the user must be `active=true` and `verified=true`
- the organizer profile must have `status=APPROVED`
- new event status is `DRAFT`
- `isPublished=false`

#### Example response
`CreateEventResponse`

```json
{
  "id": 101,
  "status": "DRAFT",
  "createdAt": "2026-05-26T03:00:00Z",
  "updatedAt": "2026-05-26T03:00:00Z"
}
```

### 3.2 Approve / Reject Event
- **Method:** `POST`
- **Path:** `/api/events/{eventId}/approve`
- **Controller:** `EventController`
- **Success status:** `200 OK`

#### Request body
`ApprovalRequest`

Required fields:
- `decision` `ApprovalDecision` (`APPROVED` or `REJECTED`)

Optional field:
- `reason`

#### Example request
```http
POST /api/events/100/approve
Content-Type: application/json
```

```json
{
  "decision": "APPROVED",
  "reason": "Looks good"
}
```

#### Business rules
- `adminUserId` is extracted from the token context
- `decision` is required
- admin must exist in `RefUser`
- admin must have role `ADMIN`
- event must exist
- event organizer must exist
- if `APPROVED`:
  - event status becomes `APPROVED`
  - `isPublished=false`
  - if the organizer is currently `CUSTOMER`, the service promotes the user to `ORGANIZER` in `identity` and syncs back to `management`
- if `REJECTED`:
  - event status becomes `CANCELLED`
  - `isPublished=false`

#### Example response
`EventApprovalResponse`

```json
{
  "approvalId": 55,
  "eventId": 100,
  "organizerId": 1,
  "organizerRole": "ORGANIZER",
  "adminUserId": 10,
  "decision": "APPROVED",
  "eventStatus": "APPROVED",
  "reason": "Looks good",
  "decidedAt": "2026-05-26T03:10:00Z"
}
```

### 3.3 Sync RefUser from Identity
- **Method:** `POST`
- **Path:** `/api/ref-users/sync`
- **Controller:** `RefUserSyncController`
- **Success status:** `200 OK`

#### Request body
`IdentityUserResponse`

Fields:
- `id` `Long`
- `email` `String`
- `fullName` `String`
- `role` `String`
- `verified` `boolean`
- `active` `boolean`

#### Example request
```http
POST /api/ref-users/sync
Content-Type: application/json
```

```json
{
  "id": 1,
  "email": "user@example.com",
  "fullName": "Nguyen Van A",
  "role": "CUSTOMER",
  "verified": true,
  "active": true
}
```

### 3.4 Submit Organizer Profile
- **Method:** `POST`
- **Path:** `/api/organizers/profiles`
- **Controller:** `OrganizerProfileController`
- **Success status:** `201 Created`

#### Request body
`OrganizerProfileRequest`

Required fields:
- `organizationName` `String`

Optional fields:
- `abbreviationName`
- `taxCode`
- `representativeName`
- `representativePosition`
- `hotline`
- `officialEmail`
- `provinceCity`
- `district`
- `wardCommune`
- `headquarterAddress`
- `websiteUrl`
- `fanpageUrl`
- `description`

#### Example request
```http
POST /api/organizers/profiles
Content-Type: application/json
Authorization: Bearer <jwt>
```

```json
{
  "organizationName": "Thesis Event Co.",
  "abbreviationName": "TEC",
  "taxCode": "0123456789012",
  "representativeName": "Nguyen Van A",
  "representativePosition": "Director",
  "officialEmail": "contact@tec.vn",
  "description": "Organizer profile submitted for verification"
}
```

#### Business rules
- the service reads the registering user id from the JWT access token
- the profile is stored with `status=PENDING`
- submitting the profile does **not** immediately make the user an organizer

### 3.5 Verify Organizer Profile
- **Method:** `POST`
- **Path:** `/api/organizers/{userId}/verify`
- **Controller:** `OrganizerProfileController`
- **Success status:** `200 OK`

#### Request body
`OrganizerVerificationRequest`

Required fields:
- `decision` `OrganizerStatus` (`APPROVED`, `REJECTED`, `SUSPENDED`)

Optional field:
- `reason`

#### Example request
```http
POST /api/organizers/1/verify
Content-Type: application/json
```

```json
{
  "decision": "APPROVED",
  "reason": "Tax code and organization details are valid"
}
```

#### Business rules
- `adminUserId` is extracted from the `X-User-Id` request header
- `decision` is required
- admin must exist in `RefUser`
- admin must have role `ADMIN`
- if `decision=APPROVED`:
  - profile status becomes `APPROVED`
  - the service stores verification audit data
  - if the user is currently `CUSTOMER`, the service promotes the user to `ORGANIZER` in `identity` and syncs back to `management`
- if `decision=REJECTED` or `SUSPENDED`:
  - profile status becomes the selected value
  - verification audit data is stored

#### Example response
`OrganizerProfileResponse`

```json
{
  "profileId": 1,
  "userId": 1,
  "organizationName": "Thesis Event Co.",
  "status": "APPROVED",
  "verifiedByAdminId": 10,
  "verifiedAt": "2026-06-03T02:00:00Z",
  "verificationReason": "Tax code and organization details are valid",
  "userRole": "ORGANIZER"
}
```

## 4) Validation Rules
The APIs use Jakarta Validation on request bodies:
- required fields must be provided
- blank strings are rejected where annotated with `@NotBlank`
- invalid `Instant` values return `400 Bad Request`

Typical invalid payload cases:
- missing required field -> `400 Bad Request`
- invalid JSON date format for `Instant` -> `400 Bad Request`
- unexpected server failure -> `500 Internal Server Error`

## 5) Eureka / Gateway Integration
The service is registered to Eureka, so a gateway can route to it by service name.

### Eureka client settings currently used
- `eureka.client.register-with-eureka=true`
- `eureka.client.fetch-registry=true`
- `eureka.client.service-url.defaultZone=http://localhost:8761/eureka/`
- `eureka.instance.prefer-ip-address=true`

### Suggested Spring Cloud Gateway routes
If you add a gateway module, a typical route set for this service would be:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: management-events
          uri: lb://management
          predicates:
            - Path=/api/events/**
        - id: management-ref-users
          uri: lb://management
          predicates:
            - Path=/api/ref-users/**
        - id: management-organizers
          uri: lb://management
          predicates:
            - Path=/api/organizers/**
```

This means:
- requests to `/api/events/**` are forwarded to the `management` service
- requests to `/api/ref-users/**` are forwarded to the `management` service
- requests to `/api/organizers/**` are forwarded to the `management` service
- load balancing is handled through Eureka discovery

## 6) Local Run Checklist
1. Start Eureka Server on `http://localhost:8761`
2. Start the `management` service on port `8082`
3. Verify the service registers itself in Eureka
4. Call the endpoints directly or through a gateway

## 7) Notes
- There is currently no dedicated gateway implementation in this workspace.
- This document describes the API that a gateway would expose for the existing management service.
- If you later add a real gateway module, update this file with the actual route IDs, predicates, filters, and auth rules.

