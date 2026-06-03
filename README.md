# Market Day API

小集日 Market Day 後端 API 專案，使用 Spring Boot 建立使用者註冊、登入、JWT 驗證、Google 登入、目前登入者資訊與使用者資料更新等功能。

## 技術棧

- Java 21
- Spring Boot 3.5.14
- Spring Web
- Spring Data JDBC
- SQL Server
- JWT：`jjwt 0.12.6`
- Swagger / OpenAPI：`springdoc-openapi 2.8.17`
- Maven Wrapper

## Spring Boot 依賴與環境需求

執行本專案前需準備：

- JDK 21
- SQL Server
- Maven Wrapper 由專案提供，無需另外安裝 Maven
- Google OAuth Web Client ID（若要測試 Google 註冊 / 登入）

主要 dependencies：

| Dependency | 用途 |
| --- | --- |
| `spring-boot-starter-web` | 建立 REST API 與靜態測試頁 |
| `spring-boot-starter-data-jdbc` | 使用 `NamedParameterJdbcTemplate` 存取 SQL Server |
| `spring-boot-starter-validation` | 後續欄位驗證使用 |
| `mssql-jdbc` | SQL Server JDBC driver |
| `jjwt-api` / `jjwt-impl` / `jjwt-jackson` | JWT 產生、解析與驗證 |
| `springdoc-openapi-starter-webmvc-ui` | Swagger UI / OpenAPI 文件 |
| `lombok` | 預留簡化 Java class 使用，目前主要 class 仍以 getter/setter 撰寫 |
| `spring-boot-starter-test` | 測試用 |
| `spring-security-test` | 後續若導入 Spring Security 測試時使用 |

## 專案結構

```text
demo/
├─ pom.xml
├─ mvnw / mvnw.cmd
├─ src/main/java/com/example/demo
│  ├─ DemoApplication.java
│  ├─ UserController.java
│  ├─ AuthService.java
│  ├─ JwtService.java
│  ├─ GoogleTokenInfo.java
│  ├─ users.java
│  └─ swagger/
│     ├─ OpenApiConfig.java
│     ├─ LocalRegisterRequest.java
│     ├─ LocalLoginRequest.java
│     ├─ GoogleCredentialRequest.java
│     └─ UpdateUserProfileRequest.java
└─ src/main/resources
   ├─ application.properties
   └─ static/
      ├─ test.html
      └─ test_2.html
```

## 資料庫

本專案使用 SQL Server，主要資料庫腳本位於專案根目錄的 `sql/` 資料夾：

```text
sql/MarketDayDB.sql
sql/MarketDayDB_ERD.png
sql/test.sql
sql/dropDB.sql
```

建議首次建立資料庫時依序執行：

```text
1. sql/MarketDayDB.sql
2. sql/test.sql
```

更多資料庫說明請參考：

```text
../sql/README.md
```

## 環境設定

設定檔位置：

```text
src/main/resources/application.properties
```

目前主要設定：

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=MarketDayDB;encrypt=true;trustServerCertificate=true
spring.datasource.username=sa
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver

google.client-id=your_google_client_id.apps.googleusercontent.com

jwt.secret=your_long_jwt_secret
jwt.expiration-ms=86400000
```

注意：

- `spring.datasource.password` 請依本機 SQL Server 設定調整
- `google.client-id` 需替換為 Google Cloud Console 的 OAuth Web Client ID
- `jwt.secret` 正式環境不建議直接寫在 repo，應改用環境變數或部署平台 secret

## 啟動專案

在 `demo/` 目錄下執行：

```powershell
.\mvnw.cmd spring-boot:run
```

或先編譯：

```powershell
.\mvnw.cmd -q -DskipTests compile
```

預設啟動後 API 位於：

```text
http://localhost:8080
```

## Swagger

啟動後可開啟 Swagger UI：

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON：

```text
http://localhost:8080/v3/api-docs
```

需要 JWT 的 API 可在 Swagger UI 右上角 `Authorize` 輸入：

```text
Bearer <JWT_TOKEN>
```

Swagger 設計規範請參考專案根目錄：

```text
../swagger.md
```

## 目前 API

### Auth / User

| Method | API                           | 說明                                |
| ------ | ----------------------------- | ----------------------------------- |
| GET    | `/usersall`                 | 查詢所有 users                      |
| POST   | `/api/auth/register/local`  | 本地端註冊                          |
| POST   | `/api/auth/register/google` | Google 註冊                         |
| POST   | `/api/auth/login/local`     | 本地端登入，成功後回傳 JWT          |
| POST   | `/api/auth/login/google`    | Google 登入，成功後回傳 JWT         |
| POST   | `/api/auth/logout`          | 登出，需要 Bearer JWT               |
| GET    | `/api/auth/me`              | 取得目前登入者資訊，需要 Bearer JWT |
| POST   | `/api/users/me`             | 修改目前登入者資料，需要 Bearer JWT |

## 測試頁

目前 `static/` 中提供簡易測試頁：

```text
http://localhost:8080/test.html
http://localhost:8080/test_2.html
```

這兩個頁面主要是為了測試 Google API 與 JWT 頁面切換流程而建立。

除 Google credential 相關流程外，其餘 API 建議優先透過 Swagger UI 測試：

```text
http://localhost:8080/swagger-ui/index.html
```

`test.html`：

- 測試 Google 註冊 / 登入
- 取得 Google credential
- 測試登入成功後取得 JWT
- 登入成功後將 JWT 存入 `localStorage`

`test_2.html`：

- 模擬登入後頁面
- 使用 JWT 呼叫 `/api/auth/me`
- 顯示目前登入者資料
- 呼叫 `/api/auth/logout`

## JWT 流程

登入成功後，後端會回傳：

```json
{
  "message": "Login successful",
  "token": "JWT_TOKEN",
  "user": {
    "email": "local-test@example.test",
    "name": "Local Test User",
    "role": "VENDOR"
  }
}
```

前端呼叫需要登入的 API 時需帶：

```http
Authorization: Bearer JWT_TOKEN
```

## Google 登入注意事項

Google 登入需要前端先透過 Google Identity Services 取得 credential，再傳給後端：

```json
{
  "credential": "GOOGLE_ID_TOKEN"
}
```

Google Cloud Console 需設定 Authorized JavaScript origins，例如：

```text
http://localhost:8080
http://127.0.0.1:5500
http://localhost:5500
```

若使用 VS Code Live Server，前端來源可能是 `http://127.0.0.1:5500`，此時若呼叫 `http://localhost:8080` API，後端需要額外設定 CORS。建議開發測試時直接使用：

```text
http://localhost:8080/test.html
```

## 開發注意事項

- 目前密碼 hash 使用 `SHA-256 + Base64`，正式環境建議改成 BCrypt
- JWT 目前未實作 blacklist，登出主要由前端刪除 token
- Google token 驗證目前透過 Google `tokeninfo` endpoint，正式環境可改用 Google 官方 Java library
- API 欄位驗證與防注入機制仍需後續補強
