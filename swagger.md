# Swagger / OpenAPI 設計規範

本文件整理本專案 Swagger / OpenAPI 的使用方式。後續新增 API 時，請同步更新 Controller annotation、Request DTO schema 與本文件 API 清單。

## 1. Swagger UI

啟動 Spring Boot 後可開啟：

```text
http://localhost:8081/swagger-ui/marketDay/index.html
```

OpenAPI JSON：

```text
http://localhost:8081/v3/api-docs
```

Swagger UI 路徑由 `application.properties` 控制：

```properties
springdoc.swagger-ui.path=/swagger-ui/marketDay/index.html
```

## 2. Swagger 與 DTO 位置

OpenAPI 設定：

```text
src/main/java/com/example/demo/swagger/OpenApiConfig.java
```

Request DTO：

```text
src/main/java/com/example/demo/dto
```

規則：

- `swagger` package 只放 Swagger/OpenAPI 設定。
- Request body schema DTO 放在 `dto` package。
- Controller 若需要 request body，優先建立 DTO，不要直接使用 `Map<String, String>`。

## 3. OpenAPI 設定

`OpenApiConfig.java` 目前包含：

- API 標題
- API 描述
- JWT Bearer security scheme

JWT security scheme 名稱：

```text
bearerAuth
```

需要 JWT 的 Controller method 請加：

```java
@SecurityRequirement(name = "bearerAuth")
```

## 4. Controller 標註

Controller class 使用 `@Tag`：

```java
@Tag(name = "主辦方 API", description = "主辦方資料與活動報名管理")
```

API method 使用 `@Operation`：

```java
@Operation(
    summary = "取得主辦方報名列表",
    description = "根據 Authorization JWT 取得目前登入主辦方，查詢該主辦方所有已發布活動的報名資料。"
)
```

撰寫原則：

- `summary` 用一句話描述 API 目的。
- `description` 補充驗證方式、主要查詢條件與回傳內容。
- 需要 JWT 的 API 加上 `@SecurityRequirement(name = "bearerAuth")`。
- Swagger 註解請使用中文，且與目前實作同步。

## 5. Request DTO 標註

Request DTO 範例：

```java
package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "本地端登入請求")
public class LocalLoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "登入 Email", example = "user@example.com")
    private String email;
}
```

Controller 使用 DTO 時需加 `@Valid`：

```java
@PostMapping("/api/vendor/local-login")
public Map<String, Object> vendorLogin(@Valid @RequestBody LocalLoginRequest body) {
    return userService.loginLocal(body, "VENDOR");
}
```

常見 DTO 驗證：

| 欄位 | 驗證 |
| --- | --- |
| email | `@Email` |
| code | 6 位數字 |
| password | 至少 8 碼，需包含英文字母與數字 |
| phone | 10 位數字，且以 `09` 開頭 |
| name | 最多 20 字 |
| applicationNo | 報名編號 |
| stallNo | 攤位編號，例如 `A01` |
| credential | Google token |

DTO 驗證錯誤由 `GlobalExceptionHandler` 統一回傳 `400 Bad Request`。

## 6. 統一 API Response

Controller 可以直接回傳 service 結果：

```java
public Map<String, Object> me(...) {
    return userService.getCurrentUser(authorizationHeader);
}
```

`GlobalResponseAdvice` 會在 response body 寫出前統一包裝：

```json
{
  "statusCode": 200,
  "message": "User info retrieved successfully",
  "messageDetails": "Executed API: GET /api/auth/me",
  "data": {
    "user": {}
  }
}
```

若 Controller 回傳已經是 `ApiResponse`，不會重複包裝。成功回覆會由 `GlobalResponseAdvice` 自動補上 `messageDetails`：

```text
Executed API: <HTTP_METHOD> <API_PATH>
```

詳細 response 格式請看：

```text
api-response.md
```

## 7. JWT API

Swagger UI 測試 JWT API：

1. 呼叫登入 API 取得 JWT。
2. 點 Swagger UI 右上角 `Authorize`。
3. 輸入：

```text
Bearer <JWT_TOKEN>
```

目前 `JwtAuthenticationFilter` 保護的 API：

| Method | API |
| --- | --- |
| POST | `/api/auth/logout` |
| GET | `/api/auth/me` |
| POST | `/api/users/me` |
| POST | `/api/account/deactivate` |
| GET | `/api/vendor/account` |
| GET | `/api/organizer/account` |

注意：`GET /api/organizer/applications/search` 目前由 `OrganizerService` 解析 `Authorization`，但尚未加入 `JwtAuthenticationFilter.protectedApis`。

## 8. 新增 API 檢查清單

1. 確認是否需要 request body。
2. 需要 request body 時，建立 DTO 到 `com.example.demo.dto`。
3. DTO 欄位加上 `@Schema` 與 Bean Validation。
4. Controller 使用 `@Valid @RequestBody DTO`。
5. Controller method 加上 `@Operation`。
6. Controller class 或 method 加上 `@Tag` / `@SecurityRequirement`。
7. 需要登入驗證時，確認是否要加入 `JwtAuthenticationFilter.protectedApis`。
8. Repository 回傳 `Map` 時，確認欄位命名已使用前端需要的 camelCase。
9. 補充或更新 `README.md`、`api-response.md`、`swagger.md`。
10. 執行：

```powershell
.\mvnw.cmd clean compile
```

## 9. API 清單

### 使用者與驗證 API

| Method | API | Request DTO | JWT | 說明 |
| --- | --- | --- | --- | --- |
| GET | `/usersall` | - | 否 | 取得所有 users |
| POST | `/api/vendor/local-register` | `LocalRegisterRequest` | 否 | 攤主本地端註冊 |
| POST | `/api/organizer/local-register` | `LocalRegisterRequest` | 否 | 主辦方本地端註冊 |
| POST | `/api/admin/local-register` | `LocalRegisterRequest` | 否 | 管理員本地端註冊 |
| POST | `/api/vendor/google-register` | `GoogleCredentialRequest` | 否 | 攤主 Google 註冊 |
| POST | `/api/organizer/google-register` | `GoogleCredentialRequest` | 否 | 主辦方 Google 註冊 |
| POST | `/api/admin/google-register` | `GoogleCredentialRequest` | 否 | 管理員 Google 註冊 |
| POST | `/api/vendor/local-login` | `LocalLoginRequest` | 否 | 攤主本地端登入 |
| POST | `/api/organizer/local-login` | `LocalLoginRequest` | 否 | 主辦方本地端登入 |
| POST | `/api/admin/local-login` | `LocalLoginRequest` | 否 | 管理員本地端登入 |
| POST | `/api/vendor/google-login` | `GoogleCredentialRequest` | 否 | 攤主 Google 登入 |
| POST | `/api/organizer/google-login` | `GoogleCredentialRequest` | 否 | 主辦方 Google 登入 |
| POST | `/api/admin/google-login` | `GoogleCredentialRequest` | 否 | 管理員 Google 登入 |
| POST | `/api/auth/createAccount/emailVerify` | `EmailVerificationRequest` | 否 | 建立帳號 email 驗證 |
| POST | `/api/auth/resetPassword/emailVerify` | `EmailVerificationRequest` | 否 | 忘記密碼 email 驗證 |
| POST | `/api/auth/resetPassword/reset` | `ResetPasswordRequest` | 否 | 重設密碼 |
| POST | `/api/auth/logout` | - | 是 | 登出 |
| GET | `/api/auth/me` | - | 是 | 取得目前登入者資料 |
| POST | `/api/users/me` | `UpdateUserProfileRequest` | 是 | 更新目前登入者資料 |
| POST | `/api/account/deactivate` | - | 是 | 停用目前登入帳號 |

### 攤主與攤位 API

| Method | API | Request DTO | JWT | 說明 |
| --- | --- | --- | --- | --- |
| POST | `/api/events/{eventId}/stalls/select` | `StallSelectionRequest` | 否 | 選擇活動攤位 |
| GET | `/api/events/{eventId}/stallsStatus` | - | 否 | 取得活動攤位狀態 |
| GET | `/api/vendor/account` | - | 是 | 取得目前登入攤主資料 |
| GET | `/api/vendor/stall-map/{applicationNo}` | - | 否 | 取得攤主報名的攤位圖資訊 |

### 主辦方 API

| Method | API | Request | JWT | 說明 |
| --- | --- | --- | --- | --- |
| GET | `/api/organizer/account` | Authorization header | 是 | 取得目前登入主辦方資料 |
| GET | `/api/organizer/applications/search` | Authorization header | 是 | 查詢目前主辦方 published 活動的所有報名資料 |

## 10. 主辦方報名列表回傳

`GET /api/organizer/applications/search` 回傳欄位：

| 欄位 | 說明 |
| --- | --- |
| `applicationId` | 報名 ID |
| `applicationNo` | 報名編號 |
| `eventId` | 活動 ID |
| `eventTitle` | 活動名稱 |
| `eventTime` | 活動時間文字 |
| `eventStartDate` | 活動開始日期 |
| `eventEndDate` | 活動結束日期 |
| `eventStartTime` | 活動開始時間 |
| `eventEndTime` | 活動結束時間 |
| `applyDates` | 報名參加日期，逗號分隔 |
| `vendorName` | 品牌名稱 / 攤位名稱 |
| `vendorOwnerName` | 攤主姓名 |
| `brandType` | 品牌類型 |
| `appliedAt` | 報名時間 |
| `applicationStatus` | 前端顯示的報名狀態 |

`applicationStatus` 由 `ApplicationStatusService` 依取消、退款、審核、付款、選位、活動結束與保證金狀態推導。
