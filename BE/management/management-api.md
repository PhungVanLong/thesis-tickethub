# Tài liệu API - Management Service

Tài liệu này mô tả chi tiết các quy tắc nghiệp vụ, mô hình dữ liệu (Database Entities) và danh sách các API của **Management Service** (Dịch vụ quản lý sự kiện và tổ chức).

---

## 1. Thông tin chung (Overview)

- **Service Name**: `management`
- **Base URL local**: `http://localhost:8082` (Hoặc gọi qua Gateway: `http://localhost:8080`)
- **API Style**: REST API
- **Xác thực**:
  - Người dùng gửi yêu cầu sẽ được API Gateway xác thực JWT và đính kèm các thông tin định danh qua header (`X-User-Id`, `X-User-Role`, `X-User-Email`).
  - Phía microservice sẽ lấy thông tin thông qua [UserContextHolder.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/security/UserContextHolder.java).

---

## 2. Quy tắc nghiệp vụ (Domain Rules)

### 2.1. Quản lý tổ chức (Organization) và thành viên (OrganizationMember)
Thay vì sử dụng các tham chiếu trực tiếp dạng người dùng cũ, dịch vụ quản lý tổ chức thông qua hai thực thể:
- [Organization.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/entity/Organization.java): Lưu thông tin hồ sơ tổ chức (tên, mã số thuế, hotline, email, mô tả, trạng thái).
- [OrganizationMember.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/entity/OrganizationMember.java): Lưu thông tin thành viên thuộc tổ chức và vai trò của họ (`OWNER` hoặc `STAFF`).

#### Quy trình duyệt hồ sơ tổ chức:
1. Người dùng (`CUSTOMER`) gửi hồ sơ tổ chức qua API đăng ký. Hồ sơ được tạo mới với trạng thái mặc định là `PENDING`.
2. Người nộp đơn được tự động gán vai trò **`OWNER`** (Chủ sở hữu) của tổ chức đó.
3. Admin thực hiện duyệt hồ sơ tổ chức:
   - **Chấp thuận (`ACTIVE`)**: Tổ chức chuyển sang `ACTIVE`. Hệ thống tự động tạo một `OutboxEvent` dạng `USER_ROLE_PROMOTE` để gửi tín hiệu bất đồng bộ nâng quyền của user (chủ sở hữu) từ `CUSTOMER` thành `ORGANIZER` bên `identity-service`.
   - **Từ chối (`REJECTED`)**: Tổ chức chuyển sang `REJECTED` (không thể thay đổi trạng thái sau đó). Hệ thống gửi email thông báo lý do từ chối đến email chính thức của tổ chức.
   - **Khóa hoạt động (`BANNED`)**: Một tổ chức đang `ACTIVE` có thể bị khóa thành `BANNED`. Khi khóa, hệ thống kiểm tra xem chủ sở hữu tổ chức đó có còn làm `OWNER` của bất kỳ tổ chức `ACTIVE` nào khác hay không. Nếu không, hệ thống sẽ tạo một `OutboxEvent` dạng `USER_ROLE_DEMOTE` gửi tín hiệu hạ quyền user từ `ORGANIZER` thành `CUSTOMER` bên `identity-service`.

### 2.2. Quy tắc tạo sự kiện (Create Event)
- Người gửi yêu cầu tạo sự kiện phải là thành viên của tổ chức tương ứng và giữ vai trò **`OWNER`**.
- Tổ chức sở hữu sự kiện phải đang ở trạng thái **`ACTIVE`**.
- Thời gian bắt đầu sự kiện (`startTime`) phải nằm trong tương lai (lớn hơn thời điểm hiện tại).
- Thời gian kết thúc (`endTime`) phải lớn hơn thời gian bắt đầu (`startTime`).
- Địa điểm tổ chức (`venue` và `city`) không được để trống.
- Sự kiện mới tạo sẽ có trạng thái mặc định là **`PENDING`** (chờ duyệt) và **`isPublished = false`** (chưa xuất bản).
- Sau khi tạo sự kiện thành công, hệ thống sinh ra một `OutboxEvent` dạng `EVENT_PENDING` gửi thông báo cho Admin.

### 2.3. Quy tắc duyệt sự kiện (Approve Event)
- Admin duyệt sự kiện dựa trên mã định danh sự kiện (`eventId`) và gửi quyết định duyệt (`ApprovalDecision` gồm `APPROVED` hoặc `REJECTED`).
- Nếu quyết định duyệt là **`APPROVED`**:
  - Trạng thái sự kiện cập nhật thành **`APPROVED`**.
  - `isPublished` vẫn giữ nguyên là `false` (chưa tự động xuất bản).
- Nếu quyết định duyệt là **`REJECTED`**:
  - Trạng thái sự kiện cập nhật thành **`CANCELLED`** (Hủy bỏ).

### 2.4. Quy tắc xuất bản sự kiện (Publish Event)
- Chỉ có chủ sở hữu tổ chức (**`OWNER`**) của sự kiện đó mới có quyền xuất bản sự kiện.
- Sự kiện phải đang ở trạng thái **`APPROVED`**.
- Khi xuất bản thành công:
  - Trạng thái sự kiện chuyển thành **`PUBLISHED`**.
  - Thuộc tính `isPublished` chuyển thành `true`.
  - Hệ thống tạo một `OutboxEvent` dạng **`EVENT_PUBLISHED`** chứa toàn bộ payload thông tin sự kiện, hạng vé (`TicketTier`), và sơ đồ ghế (`SeatMap`, `Seat`) để đồng bộ sang `booking-service` phục vụ khách hàng mua vé.

### 2.5. Quy tắc hủy sự kiện (Cancel Event)
- Chỉ có chủ sở hữu tổ chức (**`OWNER`**) của sự kiện đó mới được phép hủy sự kiện.
- Khi hủy thành công, trạng thái sự kiện cập nhật thành **`CANCELLED`**.

---

## 3. Các Enums dùng trong Hệ thống (Data Models & Enums)

### 3.1. `EventStatus`
Định nghĩa trong [EventStatus.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/entity/enums/EventStatus.java):
- `PENDING`: Sự kiện đang chờ admin phê duyệt.
- `APPROVED`: Sự kiện đã được admin phê duyệt và sẵn sàng xuất bản.
- `PUBLISHED`: Sự kiện đã xuất bản công khai, người dùng có thể mua vé.
- `CANCELLED`: Sự kiện đã bị hủy bỏ (hoặc bị từ chối phê duyệt).

### 3.2. `OrganizationStatus`
Định nghĩa trong [OrganizationStatus.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/entity/enums/OrganizationStatus.java):
- `PENDING`: Tổ chức mới đăng ký, chờ admin xác thực thông tin.
- `ACTIVE`: Tổ chức hoạt động bình thường, có quyền tạo sự kiện.
- `REJECTED`: Tổ chức bị từ chối phê duyệt.
- `BANNED`: Tổ chức vi phạm quy định và bị khóa quyền hoạt động.

### 3.3. `OrganizationRole`
Định nghĩa trong [OrganizationRole.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/entity/enums/OrganizationRole.java):
- `OWNER`: Người sở hữu / đại diện pháp lý cao nhất của tổ chức (có toàn quyền quản lý sự kiện).
- `STAFF`: Nhân viên hỗ trợ tổ chức (chỉ được thực hiện các tác vụ giới hạn).

### 3.4. `ApprovalDecision`
Định nghĩa trong [ApprovalDecision.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/entity/enums/ApprovalDecision.java):
- `APPROVED`: Đồng ý phê duyệt.
- `REJECTED`: Từ chối phê duyệt.

### 3.5. `TierType`
Định nghĩa trong [TierType.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/entity/enums/TierType.java):
- `SEATED`: Vé có vị trí ngồi cố định.
- `STANDING`: Vé đứng tự do (không chọn ghế).

### 3.6. `SeatStatus`
Định nghĩa trong [SeatStatus.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/entity/enums/SeatStatus.java):
- `AVAILABLE`: Ghế trống, có thể đặt.
- `HELD`: Ghế đang được giữ tạm thời.
- `BOOKED`: Ghế đã được đặt mua thành công.

---

## 4. Chi tiết các API Endpoints (Endpoint Details)

### 4.1. Tạo hồ sơ đăng ký tổ chức (Submit Organization)

Dành cho người dùng nộp đơn đăng ký thông tin tổ chức của họ để nâng cấp quyền.

- **URL**: `/api/organizations`
- **Method**: `POST`
- **Headers**:
  - `Content-Type: application/json`
  - `X-User-Id` (Long, bắt buộc): ID của người dùng đăng ký.
- **Request Body** (Chi tiết tại [OrganizationRequest.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/dto/request/OrganizationRequest.java)):

| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc | Mô tả |
| :--- | :--- | :---: | :--- | :--- |
| `name` | String | Có | Tối đa 255 ký tự | Tên tổ chức |
| `abbreviationName` | String | Không | Tối đa 50 ký tự | Tên viết tắt |
| `taxCode` | String | Không | 10 hoặc 13 chữ số | Mã số thuế (Kiểm tra duy nhất trùng lặp) |
| `representativeName` | String | Không | Tối đa 255 ký tự | Tên người đại diện pháp luật |
| `representativePosition` | String | Không | Tối đa 255 ký tự | Chức vụ của người đại diện |
| `hotline` | String | Không | Số từ 9 đến 15 ký tự | Số điện thoại liên hệ chính thức |
| `officialEmail` | String | Không | Định dạng Email | Email liên hệ chính thức |
| `provinceCity` | String | Không | Tối đa 100 ký tự | Tỉnh / Thành phố trụ sở |
| `district` | String | Không | Tối đa 100 ký tự | Quận / Huyện trụ sở |
| `wardCommune` | String | Không | Tối đa 100 ký tự | Phường / Xã trụ sở |
| `headquarterAddress` | String | Không | Tối đa 1000 ký tự | Địa chỉ chi tiết trụ sở |
| `websiteUrl` | String | Không | Tối đa 255 ký tự | Website tổ chức |
| `fanpageUrl` | String | Không | Tối đa 255 ký tự | Fanpage Facebook |
| `description` | String | Không | Tối đa 5000 ký tự | Thông tin giới thiệu tổ chức |

*Ví dụ Request:*
```json
{
  "name": "Công ty Giải trí TicketHub",
  "abbreviationName": "TicketHub Ent",
  "taxCode": "0102030405",
  "representativeName": "Nguyễn Văn A",
  "representativePosition": "Giám đốc",
  "hotline": "0988777666",
  "officialEmail": "contact@tickethub.com",
  "provinceCity": "Hà Nội",
  "district": "Cầu Giấy",
  "wardCommune": "Dịch Vọng",
  "headquarterAddress": "Số 1 Duy Tân",
  "websiteUrl": "https://tickethub.vn",
  "fanpageUrl": "https://facebook.com/tickethub",
  "description": "Đơn vị chuyên tổ chức sự kiện âm nhạc quy mô lớn."
}
```

- **Response thành công (HTTP 201 Created)** (Chi tiết tại [OrganizationResponse.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/dto/response/OrganizationResponse.java)):
  *Trả về thông tin hồ sơ tổ chức đã lưu cùng trạng thái mặc định `PENDING`.*
  ```json
  {
    "id": 10,
    "name": "Công ty Giải trí TicketHub",
    "abbreviationName": "TicketHub Ent",
    "taxCode": "0102030405",
    "representativeName": "Nguyễn Văn A",
    "representativePosition": "Giám đốc",
    "hotline": "0988777666",
    "officialEmail": "contact@tickethub.com",
    "provinceCity": "Hà Nội",
    "district": "Cầu Giấy",
    "wardCommune": "Dịch Vọng",
    "headquarterAddress": "Số 1 Duy Tân",
    "websiteUrl": "https://tickethub.vn",
    "fanpageUrl": "https://facebook.com/tickethub",
    "description": "Đơn vị chuyên tổ chức sự kiện âm nhạc quy mô lớn.",
    "status": "PENDING",
    "verifiedByAdminId": null,
    "verifiedAt": null,
    "verificationReason": null,
    "syncedAt": "2026-07-08T03:45:00Z"
  }
  ```

---

### 4.2. Phê duyệt hồ sơ tổ chức (Verify Organization)

Dành cho Admin duyệt, từ chối hoặc khóa tổ chức đăng ký.

- **URL**: `/api/organizations/{id}/verify`
- **Method**: `POST`
- **Headers**:
  - `Content-Type: application/json`
  - `X-User-Id` (Long, bắt buộc): ID của Admin thực hiện phê duyệt.
- **Path Parameters**:
  - `id` (Long, bắt buộc): ID của tổ chức cần duyệt.
- **Request Body** (Chi tiết tại [OrganizationVerificationRequest.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/dto/request/OrganizationVerificationRequest.java)):

| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc | Mô tả |
| :--- | :--- | :---: | :--- | :--- |
| `decision` | String (Enum) | Có | `ACTIVE` / `REJECTED` / `BANNED` | Quyết định duyệt |
| `reason` | String | Không | Không | Lý do duyệt / từ chối / khóa |

*Ví dụ Request:*
```json
{
  "decision": "ACTIVE",
  "reason": "Hồ sơ đầy đủ, thông tin doanh nghiệp chính xác."
}
```

- **Response thành công (HTTP 200 OK)** (Chi tiết tại [OrganizationResponse.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/dto/response/OrganizationResponse.java)):
  *Trả về thông tin hồ sơ tổ chức sau khi duyệt thành công.*
  ```json
  {
    "id": 10,
    "name": "Công ty Giải trí TicketHub",
    "status": "ACTIVE",
    "verifiedByAdminId": 1,
    "verifiedAt": "2026-07-08T03:50:00Z",
    "verificationReason": "Hồ sơ đầy đủ, thông tin doanh nghiệp chính xác.",
    "syncedAt": "2026-07-08T03:50:00Z"
  }
  ```

---

### 4.3. Lấy danh sách đăng ký tổ chức (Get All Organizations)

Dành cho Admin (hoặc giao diện quản lý) lấy danh sách các tổ chức đăng ký trong hệ thống, hỗ trợ lọc theo trạng thái.

- **URL**: `/api/organizations`
- **Method**: `GET`
- **Xác thực**: Cần đăng nhập (Admin)
- **Request Parameters**:
  - `status` (String, Tùy chọn): Lọc theo trạng thái tổ chức (`PENDING`, `ACTIVE`, `REJECTED`, `BANNED`). Nếu không truyền, API trả về tất cả tổ chức.

- **Response thành công (HTTP 200 OK)**:
  *Trả về danh sách các JSON Object dạng `OrganizationResponse`.*
  ```json
  [
    {
      "id": 10,
      "name": "Công ty Giải trí TicketHub",
      "abbreviationName": "TicketHub Ent",
      "taxCode": "0102030405",
      "representativeName": "Nguyễn Văn A",
      "representativePosition": "Giám đốc",
      "hotline": "0988777666",
      "officialEmail": "contact@tickethub.com",
      "provinceCity": "Hà Nội",
      "district": "Cầu Giấy",
      "wardCommune": "Dịch Vọng",
      "headquarterAddress": "Số 1 Duy Tân",
      "websiteUrl": "https://tickethub.vn",
      "fanpageUrl": "https://facebook.com/tickethub",
      "description": "Đơn vị chuyên tổ chức sự kiện âm nhạc quy mô lớn.",
      "status": "ACTIVE",
      "verifiedByAdminId": 1,
      "verifiedAt": "2026-07-08T03:50:00Z",
      "verificationReason": "Hồ sơ hợp lệ",
      "syncedAt": "2026-07-08T03:50:00Z"
    }
  ]
  ```

---

### 4.4. Tạo sự kiện mới (Create Event)

Dành cho chủ sở hữu tổ chức (`OWNER`) tạo một sự kiện nháp.

- **URL**: `/api/events/create`
- **Method**: `POST`
- **Xác thực**: Cần đăng nhập (Lấy thông tin `userId` từ SecurityContext)
- **Headers**:
  - `Content-Type: application/json`
- **Request Body** (Chi tiết tại [CreateEventRequest.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/dto/request/CreateEventRequest.java)):

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| :--- | :--- | :---: | :--- |
| `organizationId` | Long | Có | ID của tổ chức sở hữu sự kiện (User gửi yêu cầu phải là Owner của tổ chức này) |
| `title` | String | Có | Tiêu đề của sự kiện |
| `description` | String | Không | Nội dung mô tả sự kiện |
| `venue` | String | Có | Địa điểm tổ chức (ví dụ: Sân vận động Mỹ Đình) |
| `city` | String | Có | Thành phố tổ chức |
| `locationCoords` | String | Không | Tọa độ GPS của địa điểm (dạng: `"10.7769,106.7009"`) |
| `startTime` | String (ISO-8601) | Có | Thời gian bắt đầu sự kiện (Phải trong tương lai) |
| `endTime` | String (ISO-8601) | Có | Thời gian kết thúc sự kiện (Phải sau startTime) |
| `bannerUrl` | String | Không | Đường dẫn ảnh banner sự kiện |
| `ticketTiers` | List | Không | Danh sách các hạng vé cung cấp (Xem cấu trúc TicketTierRequest) |
| `seatMaps` | List | Không | Danh sách sơ đồ ghế (Xem cấu trúc SeatMapRequest) |

#### Đối tượng con: `TicketTierRequest` ([TicketTierRequest.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/dto/request/TicketTierRequest.java))
- `name` (String, Bắt buộc): Tên hạng vé (Ví dụ: "VVIP", "Standard").
- `tierType` (String, Bắt buộc): Loại hạng vé (`SEATED` - Có vị trí ngồi, hoặc `STANDING` - Vé đứng tự do).
- `price` (BigDecimal, Bắt buộc): Giá vé (phải >= 0).
- `quantityTotal` (Integer, Bắt buộc): Tổng số lượng vé phát hành (phải >= 1).
- `colorCode` (String, Tùy chọn): Mã màu hiển thị dạng Hex.
- `saleStart` (ISO-8601, Tùy chọn): Thời gian bắt đầu mở bán.
- `saleEnd` (ISO-8601, Tùy chọn): Thời gian kết thúc bán vé.

#### Đối tượng con: `SeatMapRequest` ([SeatMapRequest.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/dto/request/SeatMapRequest.java))
- `name` (String, Bắt buộc): Tên sơ đồ ghế (ví dụ: "Khu vực khán đài A").
- `totalRows` (Integer, Bắt buộc): Tổng số hàng ghế.
- `totalCols` (Integer, Bắt buộc): Tổng số cột ghế.
- `layoutJson` (String, Tùy chọn): Dữ liệu cấu hình layout dạng JSON.
- `seats` (List, Tùy chọn): Danh sách chi tiết ghế ngồi thuộc sơ đồ này.
  - Cấu trúc `SeatRequest` ([SeatRequest.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/dto/request/SeatRequest.java)):
    - `seatCode` (String, Bắt buộc): Mã ghế định danh (Ví dụ: "A-01").
    - `rowLabel` (String, Bắt buộc): Nhãn hàng ghế (Ví dụ: "A").
    - `colNumber` (Integer, Bắt buộc): Số thứ tự cột.
    - `ticketTierName` (String, Bắt buộc): Tên hạng vé áp dụng cho ghế này (Phải trùng khớp với tên một hạng vé định nghĩa trong `ticketTiers`).

*Ví dụ Request:*
```json
{
  "organizationId": 10,
  "title": "Liveshow Âm Nhạc TicketHub 2026",
  "description": "Đêm nhạc đặc sắc quy tụ các ngôi sao hàng đầu.",
  "venue": "Trung tâm Hội nghị Quốc gia",
  "city": "Hà Nội",
  "locationCoords": "21.0069,105.7836",
  "startTime": "2026-09-01T12:00:00Z",
  "endTime": "2026-09-01T15:00:00Z",
  "bannerUrl": "https://example.com/liveshow-banner.png",
  "ticketTiers": [
    {
      "name": "VIP",
      "tierType": "SEATED",
      "price": 2000000.00,
      "quantityTotal": 10,
      "colorCode": "#FFD700"
    }
  ],
  "seatMaps": [
    {
      "name": "Khán đài VIP",
      "totalRows": 1,
      "totalCols": 10,
      "layoutJson": "{}",
      "seats": [
        {
          "seatCode": "VIP-01",
          "rowLabel": "A",
          "colNumber": 1,
          "ticketTierName": "VIP"
        }
      ]
    }
  ]
}
```

- **Response thành công (HTTP 201 Created)** (Chi tiết tại [CreateEventResponse.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/dto/response/CreateEventResponse.java)):
  *Trả về thông tin sự kiện với trạng thái mặc định `PENDING`.*
  ```json
  {
    "id": 101,
    "status": "PENDING",
    "createdAt": "2026-07-08T04:00:00Z",
    "updatedAt": "2026-07-08T04:00:00Z"
  }
  ```

---

### 4.5. Phê duyệt sự kiện (Approve Event)

Dành cho Admin duyệt hoặc từ chối sự kiện nháp.

- **URL**: `/api/events/{eventId}/approve`
- **Method**: `POST`
- **Xác thực**: Cần quyền Admin (Lấy thông tin `adminUserId` từ SecurityContext)
- **Path Parameters**:
  - `eventId` (Long, bắt buộc): ID của sự kiện cần duyệt.
- **Request Body** (Chi tiết tại [ApprovalRequest.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/dto/request/ApprovalRequest.java)):

| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc | Mô tả |
| :--- | :--- | :---: | :--- | :--- |
| `decision` | String (Enum) | Có | `APPROVED` / `REJECTED` | Quyết định duyệt |
| `reason` | String | Không | Không | Lý do phê duyệt hoặc từ chối |

*Ví dụ Request:*
```json
{
  "decision": "APPROVED",
  "reason": "Sự kiện đủ điều kiện tổ chức."
}
```

- **Response thành công (HTTP 200 OK)** (Chi tiết tại [EventApprovalResponse.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/dto/response/EventApprovalResponse.java)):
  *Trả về kết quả phê duyệt và trạng thái cập nhật của sự kiện.*
  ```json
  {
    "approvalId": 15,
    "eventId": 101,
    "organizerId": 2,
    "organizerRole": "OWNER",
    "adminUserId": 1,
    "decision": "APPROVED",
    "eventStatus": "APPROVED",
    "reason": "Sự kiện đủ điều kiện tổ chức.",
    "decidedAt": "2026-07-08T04:05:00Z"
  }
  ```

---

### 4.6. Lấy danh sách sự kiện (Get All Events)

Dành cho Admin hoặc giao diện quản trị xem danh sách tất cả các sự kiện đã tạo, hỗ trợ lọc theo trạng thái sự kiện.

- **URL**: `/api/events`
- **Method**: `GET`
- **Xác thực**: Cần đăng nhập
- **Request Parameters**:
  - `status` (String, Tùy chọn): Lọc theo trạng thái sự kiện (`PENDING`, `APPROVED`, `PUBLISHED`, `CANCELLED`). Nếu không truyền, trả về tất cả các sự kiện.

- **Response thành công (HTTP 200 OK)** (Chi tiết tại [EventResponse.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/dto/response/EventResponse.java)):
  *Trả về danh sách các JSON Object dạng `EventResponse`.*
  ```json
  [
    {
      "id": 101,
      "organizationId": 10,
      "organizationName": "Công ty Giải trí TicketHub",
      "title": "Liveshow Âm Nhạc TicketHub 2026",
      "description": "Đêm nhạc đặc sắc quy tụ các ngôi sao hàng đầu.",
      "venue": "Trung tâm Hội nghị Quốc gia",
      "city": "Hà Nội",
      "locationCoords": "21.0069,105.7836",
      "startTime": "2026-09-01T12:00:00Z",
      "endTime": "2026-09-01T15:00:00Z",
      "bannerUrl": "https://example.com/liveshow-banner.png",
      "status": "APPROVED",
      "isPublished": false,
      "createdAt": "2026-07-08T04:00:00Z",
      "updatedAt": "2026-07-08T04:05:00Z"
    }
  ]
  ```

---

### 4.7. Xuất bản sự kiện (Publish Event)

Dành cho chủ sở hữu tổ chức (`OWNER`) công bố sự kiện ra công chúng sau khi đã được phê duyệt.

- **URL**: `/api/events/{eventId}/publish`
- **Method**: `POST`
- **Xác thực**: Cần đăng nhập (Lấy thông tin `userId` từ SecurityContext)
- **Path Parameters**:
  - `eventId` (Long, bắt buộc): ID của sự kiện cần xuất bản.

- **Response thành công (HTTP 200 OK)**:
  ```json
  {
    "message": "Event published successfully"
  }
  ```
  > [!NOTE]
  > Khi xuất bản thành công, một Outbox Event `EVENT_PUBLISHED` được lưu xuống DB để Kafka tự động đẩy thông tin đồng bộ toàn bộ cấu trúc sự kiện, hạng vé, ghế ngồi sang `booking-service`.

---

### 4.8. Hủy sự kiện (Cancel Event)

Dành cho chủ sở hữu tổ chức (`OWNER`) hủy bỏ một sự kiện.

- **URL**: `/api/events/{eventId}/cancel`
- **Method**: `POST`
- **Xác thực**: Cần đăng nhập (Lấy thông tin `userId` từ SecurityContext)
- **Path Parameters**:
  - `eventId` (Long, bắt buộc): ID của sự kiện cần hủy.

- **Response thành công (HTTP 200 OK)**:
  ```json
  {
    "message": "Event cancelled successfully"
  }
  ```

---

## 5. Định dạng phản hồi lỗi (Error Response Format)

Khi có ngoại lệ xảy ra, [GlobalExceptionHandler.java](file:///d:/thesis/BE/management/src/main/java/ict/thesis/management/controller/GlobalExceptionHandler.java) sẽ xử lý và trả về:

### 5.1. Lỗi tham số không hợp lệ / Ràng buộc nghiệp vụ (`IllegalArgumentException` / `ResponseStatusException`)
HTTP Status: `400 Bad Request`, `403 Forbidden`, `404 Not Found` hoặc `500 Internal Server Error`.
```json
{
  "error": "Thông điệp mô tả chi tiết lỗi phát sinh từ hệ thống"
}
```

### 5.2. Lỗi Validation dữ liệu đầu vào (`MethodArgumentNotValidException`)
HTTP Status: `400 Bad Request`. Trả về map chứa danh sách các trường bị lỗi kèm lý do tương ứng.
```json
{
  "organizationId": "organizationId is required",
  "title": "title is required"
}
```

---

## 6. Kết luận

Service `management` hiện cung cấp các nhóm API chính sau:
1. Đăng ký, quản lý và phê duyệt tổ chức (`Organization` & `OrganizationMember`).
2. Quản lý vòng đời sự kiện (`Events`, `TicketTier`, `SeatMap`, `Seat`) bao gồm các bước: Tạo mới -> Phê duyệt -> Xuất bản / Hủy bỏ.
3. Cơ chế đồng bộ trạng thái tài khoản (`CUSTOMER` <-> `ORGANIZER`) thông qua Outbox Pattern và Kafka Broker.

