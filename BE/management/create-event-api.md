# API Tạo Sự Kiện — Giao Tiếp BE ↔ FE

> **Base URL (qua API Gateway):** `http://localhost:8080`  
> **Service:** `management-service` → `/api/events`  
> **Auth:** JWT Bearer Token (Header: `Authorization: Bearer <token>`)

---

## Tổng Quan Luồng (Flow)

```
FE (Step 1-5 Wizard)
   │
   ├─ POST /api/events/create          → Tạo sự kiện (PENDING)
   │
   ├─ POST /api/events/{id}/publish    → Organizer publish sau khi được APPROVED
   │
   ├─ POST /api/events/{id}/cancel     → Organizer huỷ sự kiện
   │
   ├─ GET  /api/events/organizer/my-events → Organizer lấy danh sách sự kiện của tổ chức mình
   │
   └─ GET  /api/events?status=PENDING  → Admin lấy danh sách để duyệt
        └─ POST /api/events/{id}/approve → Admin duyệt / từ chối
```

### Trạng Thái Sự Kiện (EventStatus)
```
PENDING → APPROVED → PUBLISHED
        ↘ CANCELLED
```

---

## 1. Tạo Sự Kiện

**`POST /api/events/create`**

> Chỉ **OWNER** của tổ chức mới được gọi API này.  
> Sự kiện sẽ được tạo với trạng thái **`PENDING`** (chờ Admin duyệt).
> **Lưu ý bảo mật:** Mặc dù `organizationId` vẫn bắt buộc phải gửi trong body, Backend sẽ tự động lấy thông tin Tổ chức dựa trên `userId` trong Token để tránh tình trạng giả mạo `organizationId`.

### Request Headers
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

### Request Body
```json
{
  "organizationId": 1,
  "title": "Global Tech Summit 2024",
  "description": "Một hội nghị công nghệ quốc tế...",
  "venue": "Grand Convention Center",
  "city": "Ho Chi Minh City",
  "locationCoords": "10.7769,106.7009",
  "startTime": "2024-09-15T08:00:00Z",
  "endTime": "2024-09-15T17:00:00Z",
  "bannerUrl": "https://cdn.example.com/banners/event.jpg",
  "ticketTiers": [
    {
      "name": "VIP",
      "tierType": "SEATED",
      "price": 500000,
      "quantityTotal": 100,
      "colorCode": "#FFD700",
      "saleStart": "2024-08-01T00:00:00Z",
      "saleEnd": "2024-09-14T23:59:59Z"
    },
    {
      "name": "General",
      "tierType": "STANDING",
      "price": 200000,
      "quantityTotal": 500,
      "colorCode": "#2563EB",
      "saleStart": "2024-08-01T00:00:00Z",
      "saleEnd": "2024-09-14T23:59:59Z"
    }
  ],
  "seatMaps": [
    {
      "name": "Khu VIP A",
      "totalRows": 5,
      "totalCols": 10,
      "layoutJson": "{\"type\":\"grid\"}",
      "seats": [
        {
          "seatCode": "A1",
          "rowLabel": "A",
          "colNumber": 1,
          "ticketTierName": "VIP"
        },
        {
          "seatCode": "A2",
          "rowLabel": "A",
          "colNumber": 2,
          "ticketTierName": "VIP"
        }
      ]
    }
  ]
}
```

### Các Trường Bắt Buộc (Validation)

| Trường            | Bắt Buộc | Ghi Chú                                        |
|-------------------|----------|------------------------------------------------|
| `organizationId`  | ✅        | ID tổ chức gửi lên (Backend sẽ ghi đè bằng Id lấy từ token để bảo mật) |
| `title`           | ✅        | Tên sự kiện, không được để trống              |
| `venue`           | ✅        | Địa điểm tổ chức, không được để trống         |
| `city`            | ✅        | Thành phố, không được để trống                |
| `startTime`       | ✅        | ISO-8601, phải là thời điểm trong tương lai   |
| `endTime`         | ✅        | ISO-8601, phải sau `startTime`                |
| `description`     | ❌        | Mô tả chi tiết                                |
| `locationCoords`  | ❌        | Toạ độ GPS `"lat,lng"`                        |
| `bannerUrl`       | ❌        | URL ảnh poster/banner                         |
| `ticketTiers`     | ❌        | Danh sách hạng vé (có thể thêm sau)           |
| `seatMaps`        | ❌        | Danh sách sơ đồ ghế (chỉ cần nếu SEATED)     |

### TicketTier — Các Trường

| Trường           | Bắt Buộc | Ghi Chú                              |
|------------------|----------|--------------------------------------|
| `name`           | ✅        | Tên hạng vé (VIP, General, v.v.)    |
| `tierType`       | ✅        | `SEATED` hoặc `STANDING`            |
| `price`          | ✅        | Giá vé (>= 0)                       |
| `quantityTotal`  | ✅        | Tổng số lượng vé (>= 1)             |
| `colorCode`      | ❌        | Màu hex hiển thị trên sơ đồ         |
| `saleStart`      | ❌        | Thời gian bắt đầu bán vé            |
| `saleEnd`        | ❌        | Thời gian kết thúc bán vé           |

### TierType Enum

| Giá Trị    | Mô Tả                          |
|------------|-------------------------------|
| `SEATED`   | Ghế có đánh số, cần SeatMap   |
| `STANDING` | Vé đứng, không cần sơ đồ ghế |

### Response — 201 Created
```json
{
  "id": 42,
  "status": "PENDING",
  "createdAt": "2024-07-08T10:30:00Z",
  "updatedAt": "2024-07-08T10:30:00Z"
}
```

### Error Responses

| HTTP Code | Message                                                     | Nguyên Nhân                           |
|-----------|-------------------------------------------------------------|---------------------------------------|
| `400`     | `organizationId is required`                                | Thiếu trường bắt buộc                 |
| `400`     | `Event start time must be in the future`                    | startTime đã qua                      |
| `400`     | `Event end time must be after start time`                   | endTime <= startTime                  |
| `400`     | `User is not a member of any organization`                  | userId không thuộc bất cứ tổ chức nào |
| `400`     | `Ticket tier [name] not found in the ticket tiers list`     | seatCode tham chiếu tier không tồn tại|
| `403`     | `Only the organization owner is allowed to create events`   | User không phải OWNER                 |
| `403`     | `Organization is not active`                                | Tổ chức chưa được kích hoạt           |

---

## 2. Lấy Danh Sách Sự Kiện (Chung / Admin)

**`GET /api/events`**
**`GET /api/events?status=PENDING`**

### Query Params (Tùy Chọn)

| Param    | Giá Trị Hợp Lệ                                    |
|----------|---------------------------------------------------|
| `status` | `PENDING`, `APPROVED`, `PUBLISHED`, `CANCELLED`   |

### Response — 200 OK
```json
[
  {
    "id": 42,
    "organizationId": 1,
    "organizationName": "Tech Org",
    "title": "Global Tech Summit 2024",
    "description": "...",
    "venue": "Grand Convention Center",
    "city": "Ho Chi Minh City",
    "locationCoords": "10.7769,106.7009",
    "startTime": "2024-09-15T08:00:00Z",
    "endTime": "2024-09-15T17:00:00Z",
    "bannerUrl": "https://cdn.example.com/banners/event.jpg",
    "status": "PENDING",
    "published": false,
    "createdAt": "2024-07-08T10:30:00Z",
    "updatedAt": "2024-07-08T10:30:00Z"
  }
]
```

---

## 2b. Organizer Lấy Danh Sách Sự Kiện Của Mình

**`GET /api/events/organizer/my-events`**

> Lấy danh sách các event của tổ chức mà người dùng hiện tại đang quản lý. Lấy trực tiếp từ Token nên không cần truyền parameter.
> **Yêu cầu phân quyền (Authorization):** Người dùng phải có quyền **`ORGANIZER`** sau khi đã được xác thực (Authentication).

### Request Headers
```
Authorization: Bearer <jwt_token>
```

### Response — 200 OK
```json
[
  {
    "id": 42,
    "organizationId": 1,
    "organizationName": "Tech Org",
    "title": "Global Tech Summit 2024",
    "description": "...",
    "venue": "Grand Convention Center",
    "city": "Ho Chi Minh City",
    "locationCoords": "10.7769,106.7009",
    "startTime": "2024-09-15T08:00:00Z",
    "endTime": "2024-09-15T17:00:00Z",
    "bannerUrl": "https://cdn.example.com/banners/event.jpg",
    "status": "PENDING",
    "published": false,
    "createdAt": "2024-07-08T10:30:00Z",
    "updatedAt": "2024-07-08T10:30:00Z"
  }
]
```

---

## 2c. Lấy Chi Tiết Sự Kiện

**`GET /api/events/{eventId}`**

> Lấy chi tiết thông tin của một sự kiện, bao gồm thông tin chi tiết sự kiện, danh sách hạng vé (`ticketTiers`) và sơ đồ ghế ngồi (`seatMaps` kèm danh sách `seats`).

### Response — 200 OK
```json
{
  "id": 42,
  "organizationId": 1,
  "organizationName": "Tech Org",
  "title": "Global Tech Summit 2024",
  "description": "Một hội nghị công nghệ quốc tế...",
  "venue": "Grand Convention Center",
  "city": "Ho Chi Minh City",
  "locationCoords": "10.7769,106.7009",
  "startTime": "2024-09-15T08:00:00Z",
  "endTime": "2024-09-15T17:00:00Z",
  "bannerUrl": "https://cdn.example.com/banners/event.jpg",
  "status": "PENDING",
  "published": false,
  "createdAt": "2024-07-08T10:30:00Z",
  "updatedAt": "2024-07-08T10:30:00Z",
  "ticketTiers": [
    {
      "id": 1,
      "name": "VIP",
      "tierType": "SEATED",
      "price": 500000.00,
      "quantityTotal": 100,
      "quantityAvailable": 100,
      "quantitySold": 0,
      "colorCode": "#FFD700",
      "saleStart": "2024-08-01T00:00:00Z",
      "saleEnd": "2024-09-14T23:59:59Z",
      "seatMapId": 1
    }
  ],
  "seatMaps": [
    {
      "id": 1,
      "eventId": 42,
      "name": "Khu VIP A",
      "totalRows": 5,
      "totalCols": 10,
      "layoutJson": "{\"type\":\"grid\"}",
      "createdAt": "2024-07-08T10:30:00Z",
      "seats": [
        {
          "id": 1,
          "seatCode": "A1",
          "rowLabel": "A",
          "colNumber": 1,
          "status": "AVAILABLE",
          "ticketTierId": 1,
          "ticketTierName": "VIP",
          "colorCode": "#FFD700"
        }
      ]
    }
  ]
}
```

---

## 3. Admin Duyệt / Từ Chối Sự Kiện

**`POST /api/events/{eventId}/approve`**

> Admin duyệt qua Header `X-User-Role: ADMIN` từ Gateway.

### Request Body
```json
{
  "decision": "APPROVED",
  "reason": "Sự kiện đáp ứng đầy đủ các yêu cầu."
}
```

| Trường     | Giá Trị                      | Ghi Chú                       |
|------------|------------------------------|-------------------------------|
| `decision` | `APPROVED` hoặc `REJECTED`  | Quyết định của Admin          |
| `reason`   | string                       | Lý do duyệt / từ chối         |

### Response — 200 OK
```json
{
  "id": 1,
  "eventId": 42,
  "organizerId": 7,
  "organizerRole": "OWNER",
  "adminUserId": 3,
  "decision": "APPROVED",
  "eventStatus": "APPROVED",
  "reason": "Sự kiện đáp ứng đầy đủ các yêu cầu.",
  "decidedAt": "2024-07-08T11:00:00Z"
}
```

---

## 4. Organizer Publish Sự Kiện

**`POST /api/events/{eventId}/publish`**

> Chỉ được publish khi trạng thái là **`APPROVED`**.  
> Sau khi publish, sự kiện sẽ đồng bộ sang booking-service qua Outbox.

### Response — 200 OK
```json
{
  "message": "Event published successfully"
}
```

| HTTP Code | Message                                          | Nguyên Nhân               |
|-----------|--------------------------------------------------|---------------------------|
| `400`     | `Event must be approved before publishing`       | Sự kiện chưa được duyệt  |
| `403`     | `Only the organization owner can publish events` | Không phải OWNER          |

---

## 5. Organizer Huỷ Sự Kiện

**`POST /api/events/{eventId}/cancel`**

### Response — 200 OK
```json
{
  "message": "Event cancelled successfully"
}
```

---

## Hướng Dẫn Tích Hợp FE (Angular Service)

### Mapping Wizard Steps → API Fields

| Step | Tên Bước       | Các Trường Gửi Lên                                               |
|------|----------------|------------------------------------------------------------------|
| 1    | Event Info     | `title`, `description`, `startTime`, `endTime`                  |
| 2    | Venue          | `venue`, `city`, `locationCoords`, `bannerUrl`                  |
| 3    | Ticket Tiers   | `ticketTiers[]` (name, tierType, price, quantityTotal)          |
| 4    | Seat Map       | `seatMaps[]` (chỉ khi có vé SEATED)                            |
| 5    | Submit         | Gọi `POST /api/events/create` với toàn bộ payload đã thu thập  |

### Angular Service Mẫu — `event.service.ts`

```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type TierType = 'SEATED' | 'STANDING';
export type EventStatus = 'PENDING' | 'APPROVED' | 'PUBLISHED' | 'CANCELLED';
export type ApprovalDecision = 'APPROVED' | 'REJECTED';

export interface TicketTierPayload {
  name: string;
  tierType: TierType;
  price: number;
  quantityTotal: number;
  colorCode?: string;
  saleStart?: string;   // ISO-8601
  saleEnd?: string;     // ISO-8601
}

export interface SeatPayload {
  seatCode: string;
  rowLabel: string;
  colNumber: number;
  ticketTierName: string;
}

export interface SeatMapPayload {
  name: string;
  totalRows: number;
  totalCols: number;
  layoutJson?: string;
  seats?: SeatPayload[];
}

export interface CreateEventPayload {
  organizationId: number;
  title: string;
  description?: string;
  venue: string;
  city: string;
  locationCoords?: string;
  startTime: string;    // ISO-8601 UTC
  endTime: string;      // ISO-8601 UTC
  bannerUrl?: string;
  ticketTiers?: TicketTierPayload[];
  seatMaps?: SeatMapPayload[];
}

export interface CreateEventResult {
  id: number;
  status: EventStatus;
  createdAt: string;
  updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class EventApiService {
  private readonly http = inject(HttpClient);
  private readonly API = 'http://localhost:8080/api/events';

  /** Tạo sự kiện mới (PENDING) */
  createEvent(payload: CreateEventPayload): Observable<CreateEventResult> {
    return this.http.post<CreateEventResult>(`${this.API}/create`, payload);
  }

  /** Lấy danh sách sự kiện, lọc theo status nếu cần */
  getEvents(status?: EventStatus): Observable<any[]> {
    const params: any = {};
    if (status) params['status'] = status;
    return this.http.get<any[]>(this.API, { params });
  }

  /** Organizer lấy danh sách sự kiện của mình */
  getOrganizerEvents(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API}/organizer/my-events`);
  }

  /** Lấy chi tiết thông tin của một sự kiện */
  getEventDetail(eventId: number): Observable<any> {
    return this.http.get<any>(`${this.API}/${eventId}`);
  }

  /** Organizer publish sự kiện (phải ở trạng thái APPROVED) */
  publishEvent(eventId: number): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.API}/${eventId}/publish`, {});
  }

  /** Organizer huỷ sự kiện */
  cancelEvent(eventId: number): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.API}/${eventId}/cancel`, {});
  }

  /** Admin duyệt / từ chối sự kiện */
  approveEvent(eventId: number, decision: ApprovalDecision, reason: string): Observable<any> {
    return this.http.post(`${this.API}/${eventId}/approve`, { decision, reason });
  }
}
```

### Chuyển Đổi Datetime (Quan Trọng!)

```typescript
// datetime-local input trả về "2024-09-15T08:00"
// BE yêu cầu ISO-8601 UTC: "2024-09-15T01:00:00Z"

function toISOString(datetimeLocal: string): string {
  return new Date(datetimeLocal).toISOString();
}

// Dùng trong component:
const payload: CreateEventPayload = {
  organizationId: this.orgId,
  title: form.value.title,
  venue: form.value.venue,
  city: form.value.city,
  startTime: toISOString(form.value.startTime),
  endTime: toISOString(form.value.endTime),
  // ...
};
```

---

## Ghi Chú Quan Trọng

> **datetime:** `startTime` / `endTime` phải gửi theo ISO-8601 UTC (`"2024-09-15T08:00:00Z"`).
> Angular `datetime-local` input trả về `"2024-09-15T08:00"` — cần convert: `new Date(value).toISOString()`

> **ticketTiers và seatMaps:** Là tuỳ chọn khi tạo. Có thể tạo event trước, sau đó thêm tier/seat qua API riêng.

> **seatMaps:** Chỉ cần thiết khi loại vé là `SEATED`. Với vé `STANDING` không cần sơ đồ ghế.

> **Sau khi tạo:** Sự kiện ở trạng thái `PENDING`. Admin sẽ nhận thông báo qua Outbox để tiến hành duyệt.
