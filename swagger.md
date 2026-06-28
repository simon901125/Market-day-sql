# Swagger / OpenAPI 文件

更新日期：2026-06-29

本文件說明目前 `demo` 專案的 Swagger / OpenAPI 設定、DTO 標註方式、JWT 使用方式，以及目前 API 清單。

## Swagger UI

啟動 Spring Boot 後可開啟：

```text
http://localhost:8081/swagger-ui/marketDay/index.html
```

OpenAPI JSON：

```text
http://localhost:8081/v3/api-docs
```

Swagger UI 路徑由 `application.properties` 設定：

```properties
springdoc.swagger-ui.path=/swagger-ui/marketDay/index.html
```

## OpenAPI 設定

OpenAPI 設定檔：

```text
src/main/java/com/example/demo/swagger/OpenApiConfig.java
```

目前設定內容包含 API 標題、版本資訊與 JWT Bearer security scheme。
Swagger UI 使用的授權名稱為：

```text
bearerAuth
```

需要在 Swagger 顯示 JWT 授權的 Controller method，可以使用：

```java
@SecurityRequirement(name = "bearerAuth")
```

## DTO 架構

| 類型 | 位置 | 用途 |
| --- | --- | --- |
| Request DTO | `src/main/java/com/example/demo/dto/request` | 接收 request body。 |
| Response DTO | `src/main/java/com/example/demo/dto/response` | 放入 `ApiResponse<T>.data`。 |
| API wrapper | `src/main/java/com/example/demo/dto/ApiResponse.java` | 統一回傳 `statusCode`、`message`、`messageDetails`、`data`。 |

Controller 應直接回傳 `ApiResponse<T>`，不再以 `Map<String, Object>` 作為正式 response。

```java
@PostMapping("/api/vendor/local-login")
public ApiResponse<LoginResponse> vendorLogin(@Valid @RequestBody LocalLoginRequest body) {
    return userService.loginLocal(body, "VENDOR");
}
```

## Request DTO 標註

Request DTO 使用 `@Schema` 與 Bean Validation 標註欄位。

```java
package com.example.demo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "本地登入請求")
public class LocalLoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "登入 Email", example = "user@example.com")
    private String email;
}
```

常見驗證規則：

| 欄位 | 規則 |
| --- | --- |
| `email` | 必填，需符合 Email 格式。 |
| `password` | 必填，至少 8 碼，需包含英文與數字。 |
| `phone` | 選填；若有提供，需符合 `09xxxxxxxx`。 |
| `code` | 必填，6 位數驗證碼。 |
| `resetToken` | 重設密碼時必填。 |
| `applicationNo` | 攤位選擇與攤位圖查詢使用。 |
| `stallNo` | 攤位選擇使用，例如 `A01`。 |
| `credential` | Google 登入 / 註冊 credential。 |

DTO 驗證錯誤會由 `GlobalExceptionHandler` 回傳中文訊息。

## 統一 API Response

目前所有 Controller 回傳 `ApiResponse<T>`。

```json
{
  "statusCode": 200,
  "message": "Login successful",
  "messageDetails": null,
  "data": {}
}
```

| 欄位 | 說明 |
| --- | --- |
| `statusCode` | 成功通常為 `200`，一般錯誤為 `400`，JWT 錯誤為 `401`。 |
| `message` | 成功或錯誤訊息；錯誤會統一轉為中文。 |
| `messageDetails` | 目前直接回傳 `ApiResponse<T>`，通常為 `null`。 |
| `data` | 成功時放 Response DTO；錯誤時通常為 `null`。 |

詳細 response 範例請看：

```text
api-response.md
```

## 中文錯誤訊息

錯誤請使用：

```java
return ApiResponse.fail("Email already registered");
```

實際回傳會由 `ApiResponse.fail(...)` 轉成中文：

```json
{
  "statusCode": 400,
  "message": "此 Email 已被註冊",
  "messageDetails": null,
  "data": null
}
```

常見錯誤：

| 原始 key | 實際回傳中文 |
| --- | --- |
| `Email already registered` | `此 Email 已被註冊` |
| `Invalid email or password` | `Email 或密碼錯誤` |
| `Authorization token is required` | `請提供授權 token` |
| `Invalid or expired token` | `Token 無效或已過期` |
| `Session expired` | `登入狀態已過期，請重新登入` |

## JWT 使用方式

Swagger UI 測試需要 JWT 的 API 時：

1. 先呼叫登入 API 取得 JWT。
2. 點選 Swagger UI 的 `Authorize`。
3. 輸入：

```text
Bearer <JWT_TOKEN>
```

## JWT Protected APIs

目前 `JwtAuthenticationFilter.protectedApis` 是需要 JWT 驗證 API 的單一清單來源。

| Method | API |
| --- | --- |
| POST | `/api/auth/logout` |
| GET | `/api/auth/me` |
| POST | `/api/users/me` |
| POST | `/api/account/deactivate` |
| GET | `/api/vendor/account` |
| GET | `/api/vendor/stall-map/{applicationNo}` |
| POST | `/api/events/{eventId}/stalls/select` |
| GET | `/api/organizer/account` |
| GET | `/api/organizer/applications/search` |
| GET | `/api/organizer/applications/{id}` |

公開 API 例如註冊、登入、Email 驗證、忘記密碼、重設密碼與攤位狀態查詢，不放在 `protectedApis`。

## API 清單

### 使用者與驗證 API

| Method | API | Request DTO | JWT | 說明 |
| --- | --- | --- | --- | --- |
| GET | `/usersall` | - | 否 | 查詢所有使用者。 |
| POST | `/api/vendor/local-register` | `LocalRegisterRequest` | 否 | 攤主本地註冊。 |
| POST | `/api/organizer/local-register` | `LocalRegisterRequest` | 否 | 主辦方本地註冊。 |
| POST | `/api/vendor/google-register` | `GoogleCredentialRequest` | 否 | 攤主 Google 註冊。 |
| POST | `/api/organizer/google-register` | `GoogleCredentialRequest` | 否 | 主辦方 Google 註冊。 |
| POST | `/api/vendor/local-login` | `LocalLoginRequest` | 否 | 攤主本地登入。 |
| POST | `/api/organizer/local-login` | `LocalLoginRequest` | 否 | 主辦方本地登入。 |
| POST | `/api/admin/local-login` | `LocalLoginRequest` | 否 | 管理員本地登入。 |
| POST | `/api/vendor/google-login` | `GoogleCredentialRequest` | 否 | 攤主 Google 登入。 |
| POST | `/api/organizer/google-login` | `GoogleCredentialRequest` | 否 | 主辦方 Google 登入。 |
| POST | `/api/auth/createAccount/emailVerify` | `EmailVerificationRequest` | 否 | 註冊 Email 驗證。 |
| POST | `/api/auth/resetPassword/request` | `RequestPasswordResetRequest` | 否 | 申請重設密碼驗證碼。 |
| POST | `/api/auth/resetPassword/emailVerify` | `EmailVerificationRequest` | 否 | 驗證重設密碼 Email 驗證碼，成功後回傳 reset token。 |
| POST | `/api/auth/resetPassword/reset` | `ResetPasswordRequest` | 否 | 使用 reset token 重設密碼。 |
| POST | `/api/auth/logout` | - | 是 | 登出。 |
| GET | `/api/auth/me` | - | 是 | 取得目前登入使用者資料。 |
| POST | `/api/users/me` | `UpdateUserProfileRequest` | 是 | 更新目前登入使用者資料。 |
| POST | `/api/account/deactivate` | - | 是 | 停用目前登入帳號。 |

### 攤主與攤位 API

| Method | API | Request DTO | JWT | 說明 |
| --- | --- | --- | --- | --- |
| POST | `/api/events/{eventId}/stalls/select` | `StallSelectionRequest` | 是 | 選擇活動攤位。 |
| GET | `/api/events/{eventId}/stallsStatus` | - | 否 | 查詢活動攤位狀態。 |
| GET | `/api/vendor/account` | - | 是 | 取得目前登入攤主資料。 |
| GET | `/api/vendor/stall-map/{applicationNo}` | - | 是 | 查詢攤主申請對應的攤位圖資料。 |

### 主辦方 API

| Method | API | Request | JWT | 說明 |
| --- | --- | --- | --- | --- |
| GET | `/api/organizer/account` | Authorization header | 是 | 取得目前登入主辦方資料。 |
| GET | `/api/organizer/applications/search` | Authorization header | 是 | 查詢目前主辦方 published 活動的全部申請資料，依申請時間倒序。 |
| GET | `/api/organizer/applications/{id}` | Authorization header | 是 | 查詢主辦方申請明細。 |

## Organizer 申請列表

`GET /api/organizer/applications/search`

- 需要 `Authorization` header。
- 不接收 request body。
- 回傳目前登入主辦方的全部申請資料。
- 依 `event_applications.created_at DESC, event_applications.id DESC` 排序。
- `data` 型別為 `List<OrganizerApplicationSummaryResponse>`。

主要欄位：

| 欄位 | 說明 |
| --- | --- |
| `applicationId` | 申請 ID。 |
| `applicationNo` | 申請編號。 |
| `eventId` | 活動 ID。 |
| `eventTitle` | 活動名稱。 |
| `eventTime` | 活動時間文字。 |
| `eventStartDate` | 活動開始日期。 |
| `eventEndDate` | 活動結束日期。 |
| `eventStartTime` | 活動開始時間。 |
| `eventEndTime` | 活動結束時間。 |
| `applyDates` | 申請日期，逗號分隔。 |
| `vendorName` | 品牌 / 攤主名稱。 |
| `vendorOwnerName` | 攤主姓名。 |
| `brandType` | 品牌類型。 |
| `appliedAt` | 申請時間。 |
| `applicationStatus` | 後端計算後的顯示狀態。 |

## Organizer 申請明細

`GET /api/organizer/applications/{id}`

`data` 型別為 `OrganizerApplicationDetailResponse`。

| 區塊 | 說明 |
| --- | --- |
| `event` | 活動名稱、時間、地點、封面圖。 |
| `application` | 申請、審核、付款、保證金、退款與備註狀態。 |
| `statusTimeline` | 申請、付款、退款相關時間。 |
| `vendor` | 攤主聯絡資訊。 |
| `brand` | 品牌資訊。 |
| `registration` | 報名日期與攤位資訊。 |
| `fee` | 費用、付款與退款資訊。 |

## 文件維護規則

- 新增或調整 API 時，同步更新 `README.md`、`api-response.md`、`swagger.md`。
- `README.md` 的更新紀錄請放在「更新日誌」下方，並以日期分區。
- 新增需要 JWT 驗證的 API 時，請同步更新 `JwtAuthenticationFilter.protectedApis` 與本文件的 JWT Protected APIs 表格。
- 新增 request body 時，請建立或更新 `dto/request`。
- 新增 response data 時，請建立或更新 `dto/response`。
- 錯誤訊息請透過 `ApiResponse.fail(...)` 回傳，讓前端收到中文錯誤訊息。
