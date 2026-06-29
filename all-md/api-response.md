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

並發搶位失敗時會回：

```json
{
  "statusCode": 400,
  "message": "搶位失敗，該位置已被選擇",
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
