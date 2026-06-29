# Market Day API

Market Day 後端 API 專案，使用 Spring Boot 建置，包含帳號註冊、登入、Google 登入、JWT 驗證、攤位選擇、活動攤位狀態、主辦方資料與主辦方申請查詢。

## 更新日誌

> 更新日誌請依日期與 branch 分區：日期使用 `###`，branch 使用 `####`，避免不同分支的更動混在同一段。

### 2026-06-29

#### simon branch

- 選位 API 改為 `POST /api/stalls/select`，request body 只需 `applicationNo` 與 `stallNo`；後端會由 `applicationNo` 查出 `eventId`。
- 選位流程改為先搶攤位狀態，再綁定申請單，避免多人同時選到同一攤位時出現競爭問題。
- 選位 API 補上登入者必須為攤主，且申請單必須屬於目前登入攤主的檢查。
- `GET /api/vendor/stall-map/{applicationNo}` 僅允許 `待選位` 或已成功選位的有效申請單查看地圖；已成功選位時回傳 `selectedStall` 供攤主確認。
- `GET /api/vendor/stall-map/{applicationNo}` 的 `application` 回傳新增 `applicationStatus`。
- 回傳 JSON 中 `address` 欄位統一組成 `city + district + address`。
- `sql/test3.sql` 測資改為符合 request DTO：申請單號 `MD001` 到 `MD100`、攤位編號 `A01` 到 `A40`，資料量維持 20 攤主、2 主辦、10 活動、100 申請單。

- `GET /api/organizer/applications/search` 目前會回傳登入中主辦方的全部申請資料，不接收 request body。
- 保留 `GET /api/organizer/applications/{id}` 作為主辦方申請明細 API。
- `LocalRegisterRequest.phone` 已改為選填；若有提供，仍需符合 `09xxxxxxxx` 格式。
- `JwtAuthenticationFilter.protectedApis` 為目前需要 JWT 驗證的 API 清單來源。
- `isCurrentLoginSession` 不再額外檢查 `status = 'ACTIVE'`；登入流程本身已限制未啟用帳號。
- 錯誤訊息已透過 `ApiResponse.fail(...)` 統一轉為中文，例如 `Email already registered` 會回傳 `此 Email 已被註冊`。
- `api-response.md` 已更新為目前 `ApiResponse<T>` 與 DTO 架構版本。
- `swagger.md` 已同步目前 Swagger、DTO、JWT protected APIs 與中文錯誤訊息說明。
- `market_events` 活動時間欄位改為 `start_at`、`end_at`、`registration_start_at`、`registration_end_at` 四個 `DATETIME2(0)` 欄位；主辦方申請與攤主選位地圖 API 已同步改用 `eventStartAt/eventEndAt`、`startAt/endAt`。
- `users.status` 停用狀態改用 `DISABLED`；`POST /api/account/deactivate` 會將帳號狀態更新為 `DISABLED`。
- `market_events.publish_status` 新增 `UNPUBLISH_REQUESTED`，並新增 `event_unpublish_requests` 支援下架申請流程。
- 新增 `notifications.is_read/read_at` 支援通知中心未讀/已讀狀態。
- 新增 `admin_operation_logs` 作為管理員後台操作紀錄表。

### 2026-06-26

- 移除攤主選位地圖回傳中的底圖資訊，不再回傳 `mapImageUrl`。
- 移除專案對 `uploads` 靜態資源路徑的參考，包含 `app.upload-dir`、`app.upload-url-prefix` 與 `UploadResourceConfig`。
- 將 DB、Google OAuth、Mail 等本機私密設定改為透過環境變數提供。
- 改用 `run-local.cmd` 作為本機啟動入口，先設定環境變數，再啟動 Spring Boot。
- 將 `run-local.cmd` 加入 `.gitignore`，避免本機私密資料被提交。
- 將 `/api/organizer/applications/search` 調整為 GET 查詢，不接收 request body。
- 將 `GET /api/organizer/applications/search` 加入 `JwtAuthenticationFilter.protectedApis`。
- 新增 `GET /api/organizer/applications/{id}` 主辦方申請明細 API。

### 2026-06-25

- 新增主畫面相關 `MainScreenController`、`MainScreenService`、`MainScreenRepository` 與 `/api/main-screen` API。
- 新增主辦方查詢申請列表與申請狀態顯示邏輯。
- 調整 DB 狀態欄位與 `ApplicationStatusService` 顯示狀態。

## 技術

- Java 21
- Spring Boot 3.5.14
- Spring Web
- Spring Data JDBC
- SQL Server
- Bean Validation
- JWT：jjwt 0.12.6
- Swagger / OpenAPI：springdoc-openapi 2.8.17
- Maven Wrapper

## 專案結構

```text
demo/
├─ pom.xml
├─ mvnw / mvnw.cmd
├─ run-local.cmd
├─ README.md
├─ api-response.md
├─ swagger.md
├─ src/main/java/com/example/demo
│  ├─ Config/
│  ├─ Controller/
│  ├─ Filter/
│  ├─ Repository/
│  ├─ Service/
│  ├─ dto/
│  │  ├─ request/
│  │  └─ response/
│  └─ swagger/
└─ src/main/resources/
   ├─ application.properties
   └─ static/
```

## 設定

主要環境變數：

| 變數 | 說明 |
| --- | --- |
| `DB_URL` | SQL Server JDBC URL。 |
| `DB_USERNAME` | SQL Server 帳號。 |
| `DB_PASSWORD` | SQL Server 密碼。 |
| `GOOGLE_CLIENT_ID` | Google OAuth Client ID。 |
| `MAIL_USERNAME` | Gmail 帳號。 |
| `MAIL_PASSWORD` | Gmail App Password。 |
| `JWT_SECRET` | JWT secret。 |
| `JWT_EXPIRATION_MS` | JWT 有效時間，預設 1 小時。 |
| `ADMIN_EMAIL` | 系統初始化管理員 Email。 |
| `ADMIN_PASSWORD` | 系統初始化管理員密碼。 |
| `ADMIN_NAME` | 系統初始化管理員名稱。 |

本機可透過 `run-local.cmd` 設定環境變數並啟動 Spring Boot。

## 執行

```powershell
cd demo
.\mvnw.cmd spring-boot:run
```

測試：

```powershell
cd demo
.\mvnw.cmd test
```

## Swagger

Swagger UI：

```text
http://localhost:8081/swagger-ui/marketDay/index.html
```

OpenAPI JSON：

```text
http://localhost:8081/v3/api-docs
```

詳細 Swagger / OpenAPI 文件請看：

```text
swagger.md
```

## 統一 API Response

所有 Controller 目前直接回傳 `ApiResponse<T>`，成功時 `data` 會放入對應的 Response DTO：

```json
{
  "statusCode": 200,
  "message": "Organizer account retrieved successfully",
  "messageDetails": null,
  "data": {}
}
```

錯誤訊息會透過 `ApiResponse.fail(...)` 統一轉成中文，讓前端可以直接用中文判斷錯誤。

詳細格式請看：

```text
api-response.md
```

## JWT 與登入狀態

- 登入成功後會回傳 JWT。
- JWT expiration 會與 DB `users.expired_time` 綁定。
- 同帳號重新登入後，舊 token 會因 `expired_time` 不一致而失效。
- `JwtAuthenticationFilter.protectedApis` 是需要 JWT 驗證 API 的清單來源。
- `isCurrentLoginSession` 只檢查目前 token 是否仍是 DB 中有效登入 session，不再額外檢查 `status = 'ACTIVE'`。

目前需要 JWT 驗證的 API：

```text
POST /api/auth/logout
GET  /api/auth/me
POST /api/users/me
POST /api/account/deactivate
GET  /api/vendor/account
GET  /api/vendor/stall-map/{applicationNo}
POST /api/stalls/select
GET  /api/organizer/account
GET  /api/organizer/applications/search
GET  /api/organizer/applications/{id}
```

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
| POST | `/api/stalls/select` | `StallSelectionRequest` | 是 | 依 `applicationNo` 與 `stallNo` 選位；後端自行查出活動並檢查攤主身分。 |
| GET | `/api/events/{eventId}/stallsStatus` | - | 否 | 查詢活動攤位狀態。 |
| GET | `/api/vendor/account` | - | 是 | 取得目前登入攤主資料。 |
| GET | `/api/vendor/stall-map/{applicationNo}` | - | 是 | 查詢待選位或已成功選位申請單的攤位圖資料；已選位時回傳 `selectedStall`。 |

### 主辦方 API

| Method | API | Request | JWT | 說明 |
| --- | --- | --- | --- | --- |
| GET | `/api/organizer/account` | Authorization header | 是 | 取得目前登入主辦方資料。 |
| GET | `/api/organizer/applications/search` | Authorization header | 是 | 查詢目前主辦方 published 活動的全部申請資料，依申請時間倒序。 |
| GET | `/api/organizer/applications/{id}` | Authorization header | 是 | 查詢主辦方申請明細。 |

## 文件維護規則

- README 的更新紀錄請放在「更新日誌」底下，並以日期分區。
- 新增或調整 API 時，同步更新 `README.md`、`api-response.md`、`swagger.md`。
- 新增需要 JWT 驗證的 API 時，同步更新 `JwtAuthenticationFilter.protectedApis`。
- 新增 request body 時，請建立或更新 `dto/request`。
- 新增 response data 時，請建立或更新 `dto/response`。
- 錯誤訊息請透過 `ApiResponse.fail(...)` 回傳，讓前端收到中文錯誤訊息。

## Google 登入 / 註冊輔助測試頁面

```text
http://localhost:8081/test.html
http://localhost:8081/test_2.html
```
