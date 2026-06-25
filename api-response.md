# 統一 API Response 格式

本專案由 `GlobalResponseAdvice` 統一包裝 Controller 回傳內容。目前套用在：

- `UserController`
- `StallController`
- `OrganizerController`

Controller / Service 可以先回傳原本資料、`String`、`Map` 或 `List`，最後會統一整理成以下格式。

## Response Body

```json
{
  "statusCode": 200,
  "message": "Success",
  "messageDetails": "Executed API: GET /api/organizer/account",
  "data": {}
}
```

## 欄位說明

| key | 型別 | 說明 |
| --- | --- | --- |
| `statusCode` | number | API 回傳狀態碼。一般成功為 `200`；DTO 驗證錯誤、JWT/filter 錯誤會依情境回傳 `400` 或 `401`。 |
| `message` | string | 統一回覆訊息。若 Service 回傳 `Map` 且包含 `message`，該值會被提升到外層 `message`。 |
| `messageDetails` | string / null | 成功時自動填入執行 API，例如 `Executed API: GET /api/vendor/account`。失敗時通常為 `null`。 |
| `data` | object / array / string / null | 實際回傳資料。若原始 `Map` 中有 `message`，會從 `data` 移除，避免重複。 |

## 成功範例

```json
{
  "statusCode": 200,
  "message": "Success",
  "messageDetails": "Executed API: GET /api/organizer/account",
  "data": {
    "organizerName": "organizer1",
    "contactName": "organizer1 contact",
    "contactPhone": "0922000001",
    "contactEmail": "organizer1@example.test"
  }
}
```

## Service 回傳 message 的處理

Service 回傳：

```java
return Map.of(
    "message", "Login successful",
    "token", token
);
```

最終 Response：

```json
{
  "statusCode": 200,
  "message": "Login successful",
  "messageDetails": "Executed API: POST /api/vendor/local-login",
  "data": {
    "token": "<JWT_TOKEN>"
  }
}
```

## 主辦方報名列表範例

`GET /api/organizer/applications/search` 只需要提供 `Authorization` header。後端會從 JWT 解析目前登入主辦方，查詢該主辦方所有 `publish_status = PUBLISHED` 活動的報名資料。

```json
{
  "statusCode": 200,
  "message": "Success",
  "messageDetails": "Executed API: GET /api/organizer/applications/search",
  "data": [
    {
      "applicationId": 1,
      "applicationNo": "APP-MD0101-V01",
      "eventId": 1,
      "eventTitle": "MD0101",
      "eventTime": "2026-09-01 11:00 - 2026-09-03 19:00",
      "eventStartDate": "2026-09-01",
      "eventEndDate": "2026-09-03",
      "applyDates": "2026-09-01,2026-09-02",
      "vendorName": "vendor1 品牌",
      "vendorOwnerName": "vendor1",
      "brandType": "餐飲",
      "appliedAt": "2026-07-01T14:00:00",
      "applicationStatus": "報名完成"
    }
  ]
}
```

`applicationStatus` 由 `ApplicationStatusService` 推導，目前可能值：

```text
待審核
審核未通過
待付款
待選位
報名完成
保證金已退還
退款申請中
退款處理中
已退款
已取消
```

保證金狀態需符合已付款、已選位、活動已結束且 `deposit_status = RETURNED`，才會顯示 `保證金已退還`。

## 錯誤範例

```json
{
  "statusCode": 400,
  "message": "Invalid email or password",
  "messageDetails": null,
  "data": null
}
```

DTO 驗證錯誤：

```json
{
  "statusCode": 400,
  "message": "Validation failed: email: Invalid email format; password: Password must contain letters and numbers",
  "messageDetails": null,
  "data": null
}
```

JWT/filter 錯誤：

```json
{
  "statusCode": 401,
  "message": "Session expired",
  "messageDetails": null,
  "data": null
}
```

## 開發注意事項

- 新增 Controller 時，若要套用統一 response，需確認 `GlobalResponseAdvice.supports()` 已包含該 Controller。
- 若 Controller 已直接回傳 `ApiResponse`，不會重複包裝；成功時仍會補上 `messageDetails`。
- `messageDetails` 只記錄 HTTP method 與 API path，不包含 query string、request body 或 JWT。
- Service 業務錯誤目前仍多數以 `Map.of("message", "...")` 回傳，HTTP status 尚未全部語意化。
