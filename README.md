# Market Day API

本資料夾是 Market Day 後端 API 專案，使用 Spring Boot 建立，包含本地/Google 註冊登入、JWT 驗證、使用者資料、攤位選位、主辦方資料與主辦方報名列表查詢。

## 更新日誌

### 2026-06-26

- 移除攤主選位地圖回傳中的底圖資訊，不再回傳 `mapImageUrl`。
- 移除專案對 `uploads` 靜態資源路徑的參考，包含 `app.upload-dir`、`app.upload-url-prefix` 與 `UploadResourceConfig`。
- 將 DB、Google OAuth、Mail 等本機私密設定改為透過環境變數提供。
- 改用 `run-local.cmd` 作為本機啟動入口，先設定環境變數，再啟動 Spring Boot。
- 將 `run-local.cmd` 加入 `.gitignore`，避免提交含私密資料的本機啟動腳本。

### 2026-06-25

- 移除文件中已不存在的 `MainScreenController`、`MainScreenService`、`MainScreenRepository` 與 `/api/main-screen` API。
- 新增主辦方 API 文件：`GET /api/organizer/account`、`GET /api/organizer/applications/search`。
- 同步主辦方報名列表回傳欄位與 `applicationStatus` 顯示狀態。
- 同步 DB 狀態設計：`is_cancelled`、`payment_status`、`deposit_status`、`refund_status`、`application_dates.apply_date`。
- 補充 `GET /api/organizer/applications/search` 目前由 Service 解析 Authorization，但尚未加入 `JwtAuthenticationFilter.protectedApis` 的注意事項。

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
├─ api-response.md
├─ swagger.md
├─ README.md
├─ src/main/java/com/example/demo
│  ├─ DemoApplication.java
│  ├─ Controller/
│  ├─ dto/
│  ├─ entity/
│  ├─ Filter/
│  ├─ Repository/
│  ├─ Service/
│  └─ swagger/
└─ src/main/resources/
   ├─ application.properties
   └─ static/
```

## 資料庫

請參考 `sql/README.md` 建立 SQL Server 資料庫後，再執行本 API 專案。

## 設定

本機啟動請透過 `run-local.cmd` 設定環境變數，不需要直接修改 `application.properties` 內的私密值。

`application.properties` 會讀取下列環境變數：

| 變數 | 用途 |
| --- | --- |
| `DB_USERNAME` | SQL Server 帳號 |
| `DB_PASSWORD` | SQL Server 密碼 |
| `GOOGLE_CLIENT_ID` | Google OAuth Client ID |
| `MAIL_USERNAME` | Gmail 寄信帳號 |
| `MAIL_PASSWORD` | Gmail App Password |

範例：

```cmd
set DB_USERNAME=sa
set DB_PASSWORD=你的SQLServer密碼
set GOOGLE_CLIENT_ID=你的GoogleClientId
set MAIL_USERNAME=你的Gmail帳號
set MAIL_PASSWORD=你的GmailAppPassword
```

`run-local.cmd` 已加入 `.gitignore`，可在本機填入自己的私密資料，不要提交。

## 執行

在 terminal 進入 `demo/` 後直接執行：

```cmd
run-local.cmd
```

API base URL：

```text
http://localhost:8081
```

編譯：

```powershell
.\mvnw.cmd clean compile
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

需要 JWT 的 API 可在 Swagger UI 右上角 `Authorize` 輸入：

```text
Bearer <JWT_TOKEN>
```

Swagger 撰寫規範請看：

```text
swagger.md
```

## 統一 API Response

所有 User/Stall/Organizer Controller 回傳會由 `GlobalResponseAdvice` 統一包裝成：

```json
{
  "statusCode": 200,
  "message": "Success",
  "messageDetails": "Executed API: GET /api/organizer/account",
  "data": {}
}
```

詳細格式請看：

```text
api-response.md
```

## JWT 與登入狀態

登入成功後：

```text
users.isLogin = 1
users.expired_time = now + 1 hour
```

`JwtAuthenticationFilter` 會檢查受保護 API 的 `Authorization` header，並透過 `UpdateActiveTimeService` 延長登入有效時間。

目前 filter 保護的 API：

```text
POST /api/auth/logout
GET  /api/auth/me
POST /api/users/me
POST /api/account/deactivate
GET  /api/vendor/account
GET  /api/organizer/account
```

注意：`GET /api/organizer/applications/search` 目前由 `OrganizerService` 解析 `Authorization`，但尚未列入 `JwtAuthenticationFilter.protectedApis`。

## API 清單

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
| GET | `/api/vendor/stall-map/{applicationNo}` | - | 否 | 取得攤主報名與選位資訊 |

### 主辦方 API

| Method | API | Request | JWT | 說明 |
| --- | --- | --- | --- | --- |
| GET | `/api/organizer/account` | Authorization header | 是 | 取得目前登入主辦方資料 |
| GET | `/api/organizer/applications/search` | Authorization header | 是 | 查詢目前主辦方 published 活動的所有報名資料 |

`/api/organizer/applications/search` 回傳每筆報名資料，包含活動名稱、活動時間、報名日期、品牌名稱、攤主姓名、品牌類型、報名時間與 `applicationStatus`。

## `applicationStatus`

`ApplicationStatusService` 依 DB 欄位推導前端顯示狀態：

```text
已取消
已退款
退款處理中
退款申請中
待審核
審核未通過
待付款
待選位
保證金已退還
報名完成
```

保證金狀態只有在已付款、已選位、活動已結束且 `deposit_status = RETURNED` 時，才會顯示 `保證金已退還`。

## Google 端登入 / 註冊輔助測試頁面

```text
http://localhost:8081/test.html
http://localhost:8081/test_2.html
```

這兩個頁面主要用來輔助測試 Google credential 取得、Google 註冊 / 登入，以及登入後 JWT 呼叫流程。其他 API 建議優先使用 Swagger UI 測試。
