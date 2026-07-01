# API Response 與 DTO 架構

更新日期：2026-06-29

目前 `demo` 的 Controller 直接回傳 `ApiResponse<T>`，其中 `T` 會是對應的 Response DTO。
Request body 使用 `dto/request` 內的 Request DTO，Response data 使用 `dto/response` 內的 Response DTO。

## 統一回傳格式

所有 API 回傳格式如下：

```json
{
  "statusCode": 200,
  "message": "Login successful",
  "messageDetails": null,
  "data": {}
}
```

## 2026-07-01 更新：Organizer 報名審核 API

主辦方報名審核拆成兩支 API：

```http
POST /api/organizer/applications/{id}/approve
POST /api/organizer/applications/{id}/reject
```

### 審核通過

`POST /api/organizer/applications/{id}/approve`

- 需要 `Authorization` header。
- 不需要 request body。
- 成功後會將 `event_applications.review_status` 更新為 `APPROVED`。
- `status_logs` 會記錄 `target_type = EVENT_APPLICATION`、`status_field = event_applications.review_status`、`new_status = APPROVED`。

### 審核不通過

`POST /api/organizer/applications/{id}/reject`

- 需要 `Authorization` header。
- `reviewNote`、`reviewNoteDetail` 皆非必填。
- 成功後會將 `event_applications.review_status` 更新為 `REJECTED`。
- 不變更 SQL 結構，退件原因會以 JSON 字串存入 `event_applications.review_note`。

Request body:

```json
{
  "reviewNote": "資料不完整",
  "reviewNoteDetail": "請補上商品照片"
}
```

DB `event_applications.review_note` 儲存格式：

```json
{"reviewNote":"資料不完整","reviewNoteDetail":"請補上商品照片"}
```

Response data:

```json
{
  "applicationId": 101,
  "applicationNo": "T2-ORDER-3",
  "reviewStatus": "REJECTED",
  "reviewNote": "資料不完整",
  "reviewNoteDetail": "請補上商品照片"
}
```

`GET /api/organizer/applications/{id}` 的 `data.application` 會解析 `event_applications.review_note`，回傳：

```json
{
  "applicationId": 101,
  "applicationNo": "T2-ORDER-3",
  "applicationStatus": "審核未通過",
  "reviewNote": "資料不完整",
  "reviewNoteDetail": "請補上商品照片"
}
```

若 `review_note` 是舊版純文字資料，API 會將純文字放在 `reviewNote`，並讓 `reviewNoteDetail` 為 `null`。

| 欄位 | 型別 | 說明 |
| --- | --- | --- |
| `statusCode` | number | 成功通常為 `200`，一般錯誤為 `400`，JWT 驗證失敗為 `401`。 |
| `message` | string | API 結果訊息。錯誤訊息會透過 `ApiResponse.fail(...)` 統一轉成中文。 |
| `messageDetails` | string / null | 目前多數 Controller 直接回傳 `ApiResponse<T>`，因此通常為 `null`。 |
| `data` | object / array / null | 成功時放 Response DTO；失敗時通常為 `null`。 |

## DTO 分區

| 類型 | 位置 | 用途 |
| --- | --- | --- |
| Request DTO | `src/main/java/com/example/demo/dto/request` | 接收 request body，例如登入、註冊、驗證碼、重設密碼、攤位選擇。 |
| Response DTO | `src/main/java/com/example/demo/dto/response` | API 成功時放入 `ApiResponse<T>.data` 的資料結構。 |
| API wrapper | `src/main/java/com/example/demo/dto/response/ApiResponse.java` | 統一包裝 `statusCode`、`message`、`messageDetails`、`data`。 |

目前常用 Response DTO 包含：

| Response DTO | 主要使用 API |
| --- | --- |
| `LoginResponse` | local / Google login |
| `LoginUserResponse` | `LoginResponse.user` |
| `UserProfileResponse` | `/api/auth/me`、`/api/users/me` |
| `UserResponse` | `/usersall` |
| `VendorAccountResponse` | `/api/vendor/account` |
| `VendorStallMapResponse` | `/api/vendor/stall-map/{applicationNo}` |
| `OrganizerAccountResponse` | `/api/organizer/account` |
| `OrganizerApplicationSummaryResponse` | `/api/organizer/applications/search` |
| `OrganizerApplicationDetailResponse` | `/api/organizer/applications/{id}` |
| `EventStallStatusResponse` | `/api/events/{eventId}/stallsStatus` |
| `StallSelectionResponse` | `/api/stalls/select` |
| `PasswordResetVerificationResponse` | `/api/auth/resetPassword/emailVerify` |

## 登入成功範例

`POST /api/vendor/local-login`

```json
{
  "statusCode": 200,
  "message": "Login successful",
  "messageDetails": null,
  "data": {
    "token": "<JWT_TOKEN>",
    "user": {
      "email": "vendor@example.com",
      "name": "vendor1",
      "role": "VENDOR",
      "provider": "LOCAL",
      "emailVerified": true
    }
  }
}
```

## 攤位選位 Response

`POST /api/stalls/select`

Request body 只需要 `applicationNo` 與 `stallNo`，`eventId` 由後端依申請單查出。
`applicationNo` 與 `stallNo` 目前只檢查必填，不限制編號格式。

```json
{
  "applicationNo": "MD001",
  "stallNo": "A01"
}
```

成功時 `data` 為 `StallSelectionResponse`：

```json
{
  "statusCode": 200,
  "message": "Stall selection successful",
  "messageDetails": null,
  "data": {
    "applicationNo": "MD001",
    "stallNo": "A01"
  }
}
```

錯誤訊息會拆分為申請單狀態問題與攤位選位機制問題。

申請單狀態問題：

- `找不到申請資料`
- `此申請不屬於目前登入帳號`
- `此申請已取消`
- `此申請尚在審核中`
- `此申請審核未通過`
- `此申請尚未通過審核`
- `此申請尚未付款`
- `此申請付款狀態不可選位`
- `此申請已完成選位`

攤位選位機制問題：

- `找不到攤位資料`
- `此攤位不可選擇`
- `此攤位已被選走`

並發搶位失敗時會回：

```json
{
  "statusCode": 400,
  "message": "此攤位已被選走",
  "messageDetails": null,
  "data": null
}
```

## 攤位地圖 Response

`GET /api/vendor/stall-map/{applicationNo}`

此 API 只允許目前登入攤主查詢自己的申請單，且申請單必須是 `待選位` 或已成功選位的有效狀態。已成功選位時可用來確認攤位資料。

```json
{
  "statusCode": 200,
  "message": "Vendor stall map retrieved successfully",
  "messageDetails": null,
  "data": {
    "application": {
      "applicationNo": "MD001",
      "applicationStatus": "報名完成",
      "vendorName": "vendor1 攤位",
      "selectedStallId": 123,
      "selectedStall": {
        "selectedStallId": 123,
        "stallNo": "A01",
        "zoneName": "A",
        "width": 3.00,
        "length": 3.00,
        "height": 2.50
      }
    },
    "event": {
      "eventTitle": "MD0101",
      "startAt": "2026-09-01T11:00:00",
      "endAt": "2026-09-03T19:00:00",
      "address": "台北市中正區市集路1-1號"
    },
    "stalls": []
  }
}
```

若申請單尚未進入可查看地圖的狀態，會回：

```json
{
  "statusCode": 400,
  "message": "此申請單目前不可查看攤位地圖",
  "messageDetails": null,
  "data": null
}
```

## Organizer 申請列表

`GET /api/organizer/applications/search`

目前此 API：

- 需要 `Authorization` header。
- 不接收 request body。
- 回傳目前登入主辦方的全部申請資料。
- 依 `event_applications.created_at DESC, event_applications.id DESC` 排序。
- `data` 型別為 `List<OrganizerApplicationSummaryResponse>`。

```json
{
  "statusCode": 200,
  "message": "Organizer applications retrieved successfully",
  "messageDetails": null,
  "data": [
    {
      "applicationId": 1,
      "applicationNo": "APP-MD0101-V01",
      "eventId": 1,
      "eventTitle": "MD0101",
      "eventTime": "2026-09-01 11:00 - 2026-09-03 19:00",
      "applyDates": "2026-09-01,2026-09-02",
      "vendorName": "vendor1",
      "vendorOwnerName": "vendor1",
      "brandType": "餐飲",
      "appliedAt": "2026-07-01T14:00:00",
      "applicationStatus": "報名完成"
    }
  ]
}
```

## Organizer 申請明細

`GET /api/organizer/applications/{id}`

需要 `Authorization` header。
`data` 型別為 `OrganizerApplicationDetailResponse`，主要包含：

| 區塊 | 說明 |
| --- | --- |
| `applicationId` / `applicationNo` / `applicationStatus` | 申請基本資訊。 |
| `event` | 活動名稱、時間、地點、封面圖等資訊。 |
| `application` | 審核、付款、保證金、退款與申請備註等狀態。 |
| `statusTimeline` | 申請、付款、退款相關時間。 |
| `vendor` | 攤主聯絡資料。 |
| `brand` | 品牌資料。 |
| `registration` | 報名日期與已選攤位資料。 |
| `fee` | 費用、付款與退款資料。 |

```json
{
  "statusCode": 200,
  "message": "Organizer application detail retrieved successfully",
  "messageDetails": null,
  "data": {
    "applicationId": 1,
    "applicationNo": "APP-MD0101-V01",
    "applicationStatus": "退款處理中",
    "event": {
      "eventId": 1,
      "eventTitle": "MD0101",
      "eventTime": "2026-09-01 11:00 - 2026-09-03 19:00",
      "eventStartAt": "2026-09-01T11:00:00",
      "eventEndAt": "2026-09-03T19:00:00",
      "locationName": "市集廣場",
      "city": "台北市",
      "district": "中正區",
      "address": "市集路 1 號",
      "coverImageUrl": "/images/event.jpg"
    },
    "application": {
      "reviewStatus": "APPROVED",
      "paymentStatus": "PAID",
      "depositStatus": "NOT_RETURNED",
      "refundStatus": "REFUNDING",
      "appliedAt": "2026-07-01T14:00:00",
      "reviewNote": null,
      "applicantNote": "需要插座"
    },
    "statusTimeline": {
      "appliedAt": "2026-07-01T14:00:00",
      "paymentCreatedAt": "2026-07-02T10:00:00",
      "paidAt": "2026-07-02T10:10:00",
      "refundedAt": null
    },
    "vendor": {
      "vendorName": "vendor1",
      "vendorOwnerName": "vendor1",
      "vendorPhone": "0912345678",
      "vendorEmail": "vendor1@example.com",
      "address": "台北市中正區市集路 1 號"
    },
    "brand": {
      "brandName": "vendor1",
      "brandType": "餐飲",
      "categoryName": "甜點",
      "brandDescription": "手作甜點",
      "avatarUrl": "/images/vendor-avatar.jpg"
    },
    "registration": {
      "applyDates": "2026-09-01,2026-09-02",
      "stall": {
        "stallNo": "A01",
        "zoneName": "A 區",
        "width": 2,
        "length": 3,
        "height": 2
      }
    },
    "fee": {
      "baseFee": 2500,
      "depositAmount": 1000,
      "otherFeeAmount": 100,
      "totalAmount": 3600,
      "payment": {
        "paymentNo": "PAY-001",
        "paymentStatus": "PAID",
        "paidAt": "2026-07-02T10:10:00"
      },
      "refund": {
        "refundNo": "RF-001",
        "refundAmount": 3600,
        "refundStatus": "REFUNDING",
        "refundedAt": null
      }
    }
  }
}
```

## 錯誤訊息

錯誤訊息會直接回傳中文。
Service 或 Filter 即使傳入英文 key，也會透過 `ApiResponse.fail(...)` 轉成中文。

### Email 重複註冊

```json
{
  "statusCode": 400,
  "message": "此 Email 已被註冊",
  "messageDetails": null,
  "data": null
}
```

### 登入失敗

```json
{
  "statusCode": 400,
  "message": "Email 或密碼錯誤",
  "messageDetails": null,
  "data": null
}
```

### DTO 驗證失敗

```json
{
  "statusCode": 400,
  "message": "資料驗證失敗：電子信箱：電子信箱格式不正確; 密碼：密碼為必填",
  "messageDetails": null,
  "data": null
}
```

### JWT 未提供

```json
{
  "statusCode": 401,
  "message": "請提供授權 token",
  "messageDetails": null,
  "data": null
}
```

### JWT 無效或過期

```json
{
  "statusCode": 401,
  "message": "Token 無效或已過期",
  "messageDetails": null,
  "data": null
}
```

### Session 過期

```json
{
  "statusCode": 401,
  "message": "登入狀態已過期，請重新登入",
  "messageDetails": null,
  "data": null
}
```

## 實作注意事項

- Controller 應直接回傳 `ApiResponse<T>`，不要再回傳 `Map<String, Object>` 作為正式 response。
- 成功資料請使用 `dto/response` 內的 Response DTO。
- Request body 請使用 `dto/request` 內的 Request DTO。
- 錯誤請使用 `ApiResponse.fail(...)`，讓錯誤訊息可統一轉成中文。
- JWT Filter 失敗也會透過 `ApiResponse.fail(statusCode, message)` 回傳中文錯誤。
- `messageDetails` 目前通常為 `null`；舊版由 `GlobalResponseAdvice` 自動補 `Executed API: ...` 的設計已不是主要資料傳遞方式。

## 2026-07-01 更新：Organizer 申請詳情目前版

`GET /api/organizer/applications/{id}`

此 API 目前回傳重點：

- `message` 會由 `ApiResponse.success(...)` 統一轉為中文。
- `status` 固定回傳完整狀態流清單，未到達的節點以 `value: null`、`createdAt: null` 表示。
- `fee` 只保留各項金額與 note，不再回傳 `items`。
- `equipmentRentals` 回傳申請單已租借設備；若為電力租借，會包含 `appliances` 與 `totalWattage`。

```json
{
  "statusCode": 200,
  "message": "主辦方申請詳情取得成功",
  "messageDetails": null,
  "data": {
    "application": {
      "applicationId": 101,
      "applicationNo": "T2-ORDER-0",
      "applicationStatus": "報名完成"
    },
    "event": {
      "eventTitle": "T2 Organizer1 Timeline Event",
      "eventTime": "2026-08-10 - 2026-08-12",
      "address": "Taipei CityXinyi DistrictTimeline Test Address 1"
    },
    "vendor": {
      "vendorOwnerName": "test2 vendor 0 owner",
      "vendorPhone": "0933000000",
      "vendorEmail": "test2vendor0@example.test",
      "address": "Taipei CityXinyi DistrictTest Vendor Road 0"
    },
    "brand": {
      "brandName": "test2 vendor 0 brand",
      "categoryName": "Food",
      "brandDescription": "test2 vendor 0 brand description"
    },
    "stall": {
      "selectedStallId": 201,
      "stallNo": "A01",
      "zoneName": "A",
      "width": 3.00,
      "length": 3.00,
      "height": 2.50
    },
    "fee": {
      "stallFee": 1200,
      "stallFeeNote": "3 公尺 x 3 公尺 攤位 (1天)",
      "rentalFee": 100,
      "rentalFeeNote": "桌子 NT$100/天 x 1天",
      "equipmentRentalFee": 100,
      "depositAmount": 500,
      "depositNote": null,
      "totalAmount": 1800
    },
    "equipmentRentals": [
      {
        "equipmentRentalId": 301,
        "eventEquipmentId": 401,
        "equipmentName": "桌子",
        "rentalFee": 100,
        "pricingUnit": "DAY",
        "quantity": 1,
        "rentalUnits": 1,
        "subtotal": 100,
        "appliances": [],
        "totalWattage": null
      }
    ],
    "status": [
      { "key": "APPLIED", "label": "報名日期", "value": "已報名", "createdAt": "2026-07-01 09:00" },
      { "key": "REVIEW", "label": "審核時間", "value": "審核通過", "createdAt": "2026-07-02 10:00" },
      { "key": "CANCELLED", "label": "取消時間", "value": null, "createdAt": null },
      { "key": "PAYMENT", "label": "付款時間", "value": "付款成功", "createdAt": "2026-07-03 11:00" },
      { "key": "REFUND_REQUESTED", "label": "退款申請時間", "value": null, "createdAt": null },
      { "key": "REFUND_REVIEW", "label": "退款審核時間", "value": null, "createdAt": null },
      { "key": "REFUNDED", "label": "已退款時間", "value": null, "createdAt": null },
      { "key": "STALL_SELECTED", "label": "選位時間", "value": "已選位", "createdAt": "2026-07-04 12:00" },
      { "key": "DEPOSIT_RETURNED", "label": "保證金退還時間", "value": null, "createdAt": null }
    ]
  }
}
```
