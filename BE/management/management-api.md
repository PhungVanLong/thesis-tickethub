# Management API Documentation

## 1) Overview
`management` là service quản lý sự kiện của hệ thống.

- **Service name:** `management`
- **Base URL local:** `http://localhost:8082`
- **API style:** REST
- **Security at service level:** các route API hiện tại đang `permitAll()` trong `SecurityConfig`.
- **Business usage:** service này nhận dữ liệu user từ `identity`, lưu bản ghi tham chiếu `RefUser`, cho phép `customer` apply event draft và xử lý luồng duyệt để promote lên `organizer` khi được duyệt.

> Ghi chú: Khi đi qua `api-gateway`, các rule JWT của gateway có thể áp dụng thêm. Tài liệu này mô tả API của service `management` theo code hiện tại.

---

## 2) Domain rules

### 2.1. User reference sync
`management` không lưu toàn bộ user gốc từ `identity`. Thay vào đó, service dùng bảng tham chiếu `RefUser` để:
- đồng bộ thông tin user từ `identity`
- làm organizer của event
- làm admin thực hiện duyệt event
- lưu role hiện tại của user tại thời điểm sync

### 2.2. Event creation rule
Khi customer/applicant tạo event draft:
- `organizerId` là bắt buộc
- `title` là bắt buộc
- `startTime` và `endTime` là bắt buộc
- `startTime` phải nhỏ hơn `endTime`
- nếu organizer chưa có trong `RefUser`, service sẽ gọi sang `identity` để lấy user và sync về `management`
- user phải `active=true` và `verified=true` mới được tạo event
- organizer phải có `OrganizerProfile.status=APPROVED`
- event mới tạo có trạng thái mặc định: `DRAFT`
- `isPublished=false`

> Ghi chú: field `organizerId` trong request đang đại diện cho user nộp đơn/apply event ở thời điểm tạo draft. Role thực tế của user có thể vẫn là `CUSTOMER`; chỉ sau khi event được duyệt thì user mới được promote thành `ORGANIZER`.

### 2.3. Event approval rule
Khi duyệt event:
- `adminUserId` là bắt buộc
- `decision` là bắt buộc
- admin phải tồn tại trong `RefUser`
- admin phải có role `ADMIN`
- event phải tồn tại
- organizer của event phải tồn tại
- nếu `decision=APPROVED`:
  - event đổi sang `APPROVED`
  - nếu organizer hiện tại là `CUSTOMER`, hệ thống sẽ nâng role user đó thành `ORGANIZER` ở `identity`, sau đó sync lại về `management`
  - `isPublished` vẫn để `false`
- nếu `decision=REJECTED`:
  - event đổi sang `CANCELLED`
  - `isPublished=false`

### 2.4. RefUser sync rule
Khi `identity` thay đổi user:
- `management` nhận payload user mới/cập nhật tại endpoint sync
- `RefUser` sẽ được upsert theo `id`
- nếu record đã tồn tại thì cập nhật lại thông tin
- nếu chưa tồn tại thì tạo mới

### 2.5. Organizer profile verification rule
Luồng duyệt tổ chức dựa trên JWT access token:
- user gửi hồ sơ tổ chức qua endpoint submit profile, hệ thống lấy `userId` từ JWT
- hệ thống lưu `OrganizerProfile` với `status=PENDING`
- admin duyệt hồ sơ bằng endpoint verify
- nếu `decision=APPROVED`:
  - profile đổi sang `APPROVED`
  - nếu user hiện tại là `CUSTOMER`, hệ thống sẽ nâng role user đó thành `ORGANIZER` ở `identity`, sau đó sync lại về `management`
- nếu `decision=REJECTED` hoặc `SUSPENDED`:
  - profile cập nhật lại status tương ứng
  - lưu thông tin admin duyệt, thời điểm duyệt và lý do

---

## 3) Base endpoints

### Events
- `POST /api/events/create`
- `POST /api/events/{eventId}/approve`

### User reference sync
- `POST /api/ref-users/sync`

### Organizer profiles
- `POST /api/organizers/profiles`
- `POST /api/organizers/{userId}/verify`

---

## 4) Data model / enums

### 4.1. `EventStatus`
Các trạng thái đang có trong code:
- `DRAFT`
- `PENDING`
- `APPROVED`
- `PUBLISHED`
- `CANCELLED`

> Lưu ý: Luồng create hiện tại đặt `PENDING` (chờ duyệt). Luồng approve sẽ set `APPROVED` hoặc `CANCELLED`.

### 4.2. `ApprovalDecision`
- `APPROVED`
- `REJECTED`

### 4.3. `UserRole`
- `CUSTOMER`
- `ORGANIZER`
- `STAFF`
- `ADMIN`

---

## 5) Endpoint: Create Event

### `POST /api/events/create`
Tạo một event draft do `customer`/applicant nộp.

### Request body
Class: `CreateEventRequest`

| Field | Type | Required | Validation / note |
|---|---|---:|---|
| `organizationId` | `Long` | Yes | `@NotNull(message = "organizationId is required")` |
| `title` | `String` | Yes | `@NotBlank(message = "title is required")` |
| `description` | `String` | No | mô tả event |
| `venue` | `String` | Yes | `@NotBlank(message = "venue is required")` |
| `city` | `String` | Yes | `@NotBlank(message = "city is required")` |
| `locationCoords` | `String` | No | tọa độ dạng text |
| `startTime` | `Instant` | Yes | `@NotNull(message = "startTime is required")` |
| `endTime` | `Instant` | Yes | `@NotNull(message = "endTime is required")` |
| `bannerUrl` | `String` | No | link banner |
| `ticketTiers` | `List<TicketTierRequest>` | No | `@Valid` danh sách các hạng vé |
| `seatMaps` | `List<SeatMapRequest>` | No | `@Valid` danh sách sơ đồ ghế |

#### Class: `TicketTierRequest`
| Field | Type | Required | Validation / note |
|---|---|---:|---|
| `name` | `String` | Yes | `@NotBlank(message = "Name is required")` |
| `tierType` | `TierType` | Yes | `@NotNull(message = "Tier type is required")` |
| `price` | `BigDecimal` | Yes | `@NotNull`, `@DecimalMin(value = "0.0")` |
| `quantityTotal` | `Integer` | Yes | `@NotNull`, `@Min(value = 1)` |
| `colorCode` | `String` | No | Mã màu HEX hiển thị |
| `saleStart` | `Instant` | No | Thời điểm bắt đầu bán vé |
| `saleEnd` | `Instant` | No | Thời điểm kết thúc bán vé |

#### Class: `SeatMapRequest`
| Field | Type | Required | Validation / note |
|---|---|---:|---|
| `name` | `String` | Yes | `@NotBlank(message = "Seat map name is required")` |
| `totalRows` | `Integer` | Yes | `@NotNull(message = "Total rows is required")` |
| `totalCols` | `Integer` | Yes | `@NotNull(message = "Total columns is required")` |
| `layoutJson` | `String` | No | Layout chi tiết sơ đồ ghế dạng JSON |
| `seats` | `List<SeatRequest>` | No | Danh sách ghế chi tiết của sơ đồ này |

#### Class: `SeatRequest`
| Field | Type | Required | Validation / note |
|---|---|---:|---|
| `seatCode` | `String` | Yes | `@NotBlank(message = "Seat code is required")` |
| `rowLabel` | `String` | Yes | `@NotBlank(message = "Row label is required")` |
| `colNumber` | `Integer` | Yes | `@NotNull(message = "Column number is required")` |
| `ticketTierName` | `String` | Yes | `@NotBlank(message = "Ticket tier name is required")` |

### Example request
```http
POST /api/events/create
Content-Type: application/json
```

```json
{
  "organizationId": 1,
  "title": "Tech Conference 2026",
  "description": "Annual conference for developers",
  "venue": "Convention Center",
  "city": "Ho Chi Minh City",
  "locationCoords": "10.7769,106.7009",
  "startTime": "2026-08-01T09:00:00Z",
  "endTime": "2026-08-01T17:00:00Z",
  "bannerUrl": "https://example.com/banner.png",
  "ticketTiers": [
    {
      "name": "VIP",
      "tierType": "SEATED",
      "price": 1500000.0,
      "quantityTotal": 100,
      "colorCode": "#FFD700",
      "saleStart": "2026-07-10T00:00:00Z",
      "saleEnd": "2026-07-31T23:59:59Z"
    },
    {
      "name": "Standard",
      "tierType": "SEATED",
      "price": 500000.0,
      "quantityTotal": 500,
      "colorCode": "#C0C0C0",
      "saleStart": "2026-07-10T00:00:00Z",
      "saleEnd": "2026-07-31T23:59:59Z"
    }
  ],
  "seatMaps": [
    {
      "name": "Main Hall",
      "totalRows": 10,
      "totalCols": 10,
      "layoutJson": "{\"rows\": 10, \"cols\": 10}",
      "seats": [
        {
          "seatCode": "A-1",
          "rowLabel": "A",
          "colNumber": 1,
          "ticketTierName": "VIP"
        },
        {
          "seatCode": "A-2",
          "rowLabel": "A",
          "colNumber": 2,
          "ticketTierName": "VIP"
        },
        {
          "seatCode": "B-1",
          "rowLabel": "B",
          "colNumber": 1,
          "ticketTierName": "Standard"
        }
      ]
    }
  ]
}
```

### Processing rule
1. Tìm thành viên tổ chức dựa trên `organizationId` và `userId` (lấy từ Token).
2. Nếu không tìm thấy, trả về lỗi `400 Bad Request`.
3. Kiểm tra xem tổ chức đó có đang hoạt động hay không (`status = ACTIVE`). Nếu không hoạt động, trả về lỗi `403 Forbidden`.
4. Kiểm tra quyền của thành viên tổ chức. Chỉ `OWNER` mới có quyền tạo sự kiện. Nếu không phải `OWNER`, trả về lỗi `403 Forbidden`.
5. Validate thời gian: `startTime` phải ở tương lai, và `endTime` phải diễn ra sau `startTime`. Nếu sai trả về lỗi `400 Bad Request`.
6. Validate địa điểm: `venue` và `city` không được null hay trống.
7. Tạo thực thể `Events` với trạng thái mặc định là `PENDING` và `isPublished = false`.
8. Lưu event vào Database.
9. Lưu các hạng vé (`TicketTier`) liên kết với event.
10. Lưu sơ đồ ghế (`SeatMap`) liên kết với event và các ghế (`Seat`) liên kết với sơ đồ ghế tương ứng.
11. Tạo `OutboxEvent` dạng `EVENT_PENDING` để thông báo cho Admin.
12. Trả về thông tin event mới tạo thành công.

### Success response
HTTP status: `201 Created`

Class: `CreateEventResponse`

| Field | Type |
|---|---|
| `id` | `Long` |
| `status` | `EventStatus` |
| `createdAt` | `Instant` |
| `updatedAt` | `Instant` |

### Example response
```json
{
  "id": 101,
  "status": "PENDING",
  "createdAt": "2026-07-07T08:30:00Z",
  "updatedAt": "2026-07-07T08:30:00Z"
}
```

### Failure cases
- `400 Bad Request`
  - `organizationId` null
  - `title` null hoặc rỗng
  - `venue` hoặc `city` null hoặc rỗng
  - `startTime` ở quá khứ hoặc `startTime` >= `endTime`
  - Sai thông tin ticketTier liên kết với ghế
- `403 Forbidden`
  - User không phải là thành viên của tổ chức
  - Tổ chức không ở trạng thái ACTIVE
  - User không có vai trò OWNER trong tổ chức
- `404 Not Found`
  - Không tìm thấy tài nguyên tương ứng

---

## 6) Endpoint: Approve / Reject Event

### `POST /api/events/{eventId}/approve`
Dùng để admin duyệt hoặc từ chối event.

### Path parameter
| Field | Type | Required |
|---|---|---:|
| `eventId` | `Long` | Yes |

### Request body
Class: `ApprovalRequest`

| Field | Type | Required | Note |
|---|---|---:|---|
| `decision` | `ApprovalDecision` | Yes | `APPROVED` hoặc `REJECTED` |
| `reason` | `String` | No | lý do duyệt / từ chối |

### Example request
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

### Processing rule
1. Lấy `adminUserId` từ token thông qua Security Context.
2. Kiểm tra `adminUserId` và `decision` (nếu null -> lỗi `400 Bad Request`).
2. Tìm event theo `eventId`
3. Tìm admin trong `RefUser`
4. Kiểm tra role admin:
   - nếu không phải `ADMIN` -> `403 Forbidden`
5. Lấy organizer của event
6. Tạo record `EventApprovals`
7. Nếu `APPROVED`:
   - event.status = `APPROVED`
   - event.isPublished = `false`
   - nếu applicant/organizer hiện tại là `CUSTOMER`:
     - gọi sang `identity` để đổi role user đó thành `ORGANIZER`
     - sync lại `RefUser` từ identity
8. Nếu `REJECTED`:
   - event.status = `CANCELLED`
   - event.isPublished = `false`
9. Lưu event + approval
10. Trả response approval

### Success response
HTTP status: `200 OK`

Class: `EventApprovalResponse`

| Field | Type |
|---|---|
| `approvalId` | `Long` |
| `eventId` | `Long` |
| `organizerId` | `Long` |
| `organizerRole` | `String` |
| `adminUserId` | `Long` |
| `decision` | `ApprovalDecision` |
| `eventStatus` | `EventStatus` |
| `reason` | `String` |
| `decidedAt` | `Instant` |

### Example response
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

### Failure cases
- `400 Bad Request`
  - token thiếu thông tin user / `adminUserId` null
  - `decision` null
  - event organizer missing
  - invalid business rule
- `403 Forbidden`
  - admin user không có role `ADMIN`
- `404 Not Found`
  - event không tồn tại
  - admin user không tồn tại

---

## 7) Endpoint: Sync RefUser from Identity

### `POST /api/ref-users/sync`
Upsert user reference từ `identity` sang `management`.

### Request body
Class: `IdentityUserResponse`

| Field | Type | Required |
|---|---|---:|
| `id` | `Long` | Yes |
| `email` | `String` | No |
| `fullName` | `String` | No |
| `role` | `String` | No |
| `verified` | `boolean` | Yes |
| `active` | `boolean` | Yes |

### Example request
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

### Processing rule
1. Tìm `RefUser` theo `id`
2. Nếu chưa có -> tạo mới
3. Nếu đã có -> cập nhật
4. Map `role`:
   - nếu role null/blank/không hợp lệ -> `CUSTOMER`
5. Set `syncedAt = now`

### Success response
HTTP status: `200 OK`

Response body là entity `RefUser` sau khi upsert.

### Example response
```json
{
  "id": 1,
  "fullName": "Nguyen Van A",
  "email": "user@example.com",
  "role": "CUSTOMER",
  "syncedAt": "2026-05-26T03:15:00Z"
}
```

---

## 8) Error handling
Service đang dùng `GlobalExceptionHandler` để chuyển lỗi thành response rõ ràng.

### 8.1. `IllegalArgumentException`
HTTP status: `400 Bad Request`

Response format:
```json
{
  "error": "message lỗi"
}
```

### 8.2. Validation error
HTTP status: `400 Bad Request`

Response format:
```json
{
  "fieldName": "message validation"
}
```

Ví dụ:
```json
{
  "organizationId": "organizationId is required",
  "title": "title is required"
}
```

### 8.3. `ResponseStatusException`
- `404 Not Found`
- `403 Forbidden`
- `400 Bad Request`

tuỳ theo rule nghiệp vụ trong service.

---

## 9) Security rule hiện tại

### Tại service level
Trong `SecurityConfig`, API routes đang được permit all.

### Ở gateway level
Nếu hệ thống đi qua `api-gateway`, internal request có thể bị chặn nếu không có JWT hợp lệ.

### Ghi chú quan trọng
- Public API nên đi qua gateway mà không cần token.
- Internal API nên có token.
- Gateway sẽ gắn thêm user headers cho downstream service.

---

## 10) Tóm tắt luồng nghiệp vụ

### 10.1. Customer tạo draft event
1. Customer đăng ký / đăng nhập ở `identity`
2. `identity` sync user sang `management`
3. Customer gọi `POST /api/events/create`
4. `management` kiểm tra thành viên tổ chức dựa trên `organizationId` và `userId` từ token
5. Event được tạo với status `PENDING`
6. Role của user chưa đổi ngay; vẫn có thể là `CUSTOMER` cho tới khi admin duyệt

### 10.2. Admin duyệt event
1. Admin gọi `POST /api/events/{eventId}/approve`
2. Nếu `APPROVED`:
   - event chuyển sang `APPROVED`
   - nếu user đang là `CUSTOMER`, hệ thống nâng role thành `ORGANIZER`
3. `identity` cập nhật user
4. `management` sync lại `RefUser`

---

## 11) Best practices khi gọi API

- `organizationId` trong event phải là tổ chức hợp lệ có trạng thái ACTIVE
- User gửi yêu cầu tạo phải là OWNER của tổ chức đó
- Chỉ admin mới được gọi approve endpoint
- Nếu đổi role / active / verified ở `identity`, gọi sync để `management` cập nhật theo
- Dùng `Instant` chuẩn ISO-8601 trong request body

---

## 12) API checklist

- [x] Tạo event
- [x] Duyệt / từ chối event
- [x] Sync user reference
- [x] Validation errors trả về 400
- [x] Admin role check cho approval
- [x] Organizer promotion sau khi approved

---

## 13) Kết luận
`management` hiện cung cấp 3 nhóm API chính:
1. Event creation
2. Event approval
3. RefUser synchronization

Toàn bộ luồng được thiết kế để:
- nhận user từ `identity`
- tạo event draft cho customer/applicant
- duyệt event bởi admin
- nâng customer thành organizer khi event được duyệt
- đồng bộ user reference trong `management` khi user ở `identity` thay đổi

