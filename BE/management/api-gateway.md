# API Gateway Documentation

## 1) Scope
This workspace currently contains the `management` Spring Boot service and **not** a dedicated API Gateway module.

What this document covers:
- The HTTP API currently exposed by the management service
- How the service is discovered through Eureka
- A suggested gateway route mapping if you place Spring Cloud Gateway in front of this service

## 2) Service Overview
- **Service name:** `management`
- **Port:** `8082`
- **Discovery:** Eureka client enabled via `@EnableDiscoveryClient`
- **Base security rule:** `/api/events/**` is publicly accessible (`permitAll`)

## 3) Current API Surface

### Create Event
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

## 4) Validation Rules
The API uses Jakarta Validation on the request body:
- `organizerId` must be provided
- `title` must not be blank
- `startTime` must be provided
- `endTime` must be provided

Typical invalid payload cases:
- Missing required field -> `400 Bad Request`
- Invalid JSON date format for `Instant` -> `400 Bad Request`
- Unexpected server failure -> `500 Internal Server Error`

## 5) Eureka / Gateway Integration
The service is registered to Eureka, so a gateway can route to it by service name.

### Eureka client settings currently used
- `eureka.client.register-with-eureka=true`
- `eureka.client.fetch-registry=true`
- `eureka.client.service-url.defaultZone=http://localhost:8761/eureka/`
- `eureka.instance.prefer-ip-address=true`

### Suggested Spring Cloud Gateway route
If you add a gateway module, a typical route for this service would be:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: management-service
          uri: lb://management
          predicates:
            - Path=/api/events/**
```

This means:
- Requests to `/api/events/**` are forwarded to the `management` service
- Load balancing is handled through Eureka discovery

## 6) Local Run Checklist
1. Start Eureka Server on `http://localhost:8761`
2. Start the `management` service on port `8082`
3. Verify the service registers itself in Eureka
4. Call `POST /api/events/create` either directly or through the gateway

## 7) Notes
- There is currently no dedicated gateway implementation in this workspace.
- This document describes the API that a gateway would expose for the existing management service.
- If you later add a real gateway module, update this file with the actual route IDs, predicates, filters, and auth rules.

