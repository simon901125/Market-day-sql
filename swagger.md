# Swagger / OpenAPI 設計規範

本文件整理本專案 Swagger / OpenAPI 的使用方式，後續新增 API 時可直接依照此規範撰寫。

## 1. 使用套件

本專案使用 `springdoc-openapi`：

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.17</version>
</dependency>
```

啟動 Spring Boot 後可透過以下網址查看：

```text
http://localhost:8080/swagger-ui/index.html
http://localhost:8080/v3/api-docs
```

## 2. 檔案位置

Swagger 相關檔案集中放在：

```text
demo/src/main/java/com/example/demo/swagger
```

目前包含：

```text
OpenApiConfig.java
LocalRegisterRequest.java
LocalLoginRequest.java
GoogleCredentialRequest.java
```

規則：

- OpenAPI 全域設定放在 `OpenApiConfig.java`
- Request body schema DTO 放在 `swagger` package
- Controller 需要 request body 時，優先使用 DTO，不要直接用 `Map<String, String>`

## 3. OpenAPI 全域設定

`OpenApiConfig.java` 負責設定：

- API 文件標題
- API 文件描述
- JWT Bearer security scheme

目前使用的 security scheme 名稱：

```text
bearerAuth
```

需要 JWT 的 API 要加：

```java
@SecurityRequirement(name = "bearerAuth")
```

## 4. Controller 註解規範

Controller 類別使用：

```java
@Tag(name = "使用者與驗證 API", description = "使用者註冊、登入、登出與目前登入者資訊 API")
```

每個 API 使用：

```java
@Operation(summary = "本地端登入", description = "使用 email/password 登入，成功後回傳 JWT token。")
```

規則：

- `summary` 使用短句，描述 API 做什麼
- `description` 補充流程、驗證、回傳重點
- 顯示文字統一使用繁體中文

## 5. Request Body Schema DTO

需要 request body 的 API，建立明確 DTO，例如：

```java
@Schema(description = "本地端登入請求資料")
public class LocalLoginRequest {

    @Schema(description = "使用者 Email", example = "local-test@example.test")
    private String email;

    @Schema(description = "使用者密碼", example = "LocalTest123")
    private String password;
}
```

Controller 使用：

```java
@PostMapping("/api/auth/login/local")
public Map<String, Object> login(@RequestBody LocalLoginRequest body) {
    ...
}
```

這樣 Swagger 的 `Schema` 與 `Example Value` 會自動顯示欄位與範例。

## 6. 欄位註解規範

每個 request DTO 欄位都建議補：

```java
@Schema(description = "欄位說明", example = "範例值")
```

若欄位有固定值，補 `allowableValues`：

```java
@Schema(
    description = "使用者角色",
    example = "VENDOR",
    allowableValues = {"VENDOR", "ORGANIZER", "ADMIN"}
)
private String role;
```

## 7. JWT API 規範

需要登入後才能使用的 API：

```java
@Operation(summary = "取得目前登入者資訊", description = "需要在 Authorization header 帶入 Bearer JWT，回傳目前登入者資料。")
@SecurityRequirement(name = "bearerAuth")
@GetMapping("/api/auth/me")
```

Swagger UI 測試方式：

1. 呼叫登入 API 取得 JWT
2. 點 Swagger UI 右上角 `Authorize`
3. 輸入：

```text
Bearer <JWT_TOKEN>
```

4. 再呼叫需要登入的 API

## 8. 新增 API 時的建議流程

1. 先確認 API 是否需要 request body
2. 若需要 request body，新增 DTO 到 `com.example.demo.swagger`
3. 在 DTO 欄位補 `@Schema(description, example)`
4. Controller 方法使用 DTO 作為 `@RequestBody`
5. Controller 方法補 `@Operation`
6. 若 API 需要 JWT，補 `@SecurityRequirement(name = "bearerAuth")`
7. 執行編譯確認

```powershell
.\mvnw.cmd -q -DskipTests compile
```

## 9. 範本

Request DTO：

```java
package com.example.demo.swagger;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "範例請求資料")
public class ExampleRequest {

    @Schema(description = "欄位說明", example = "example-value")
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
```

Controller：

```java
@Operation(summary = "範例 API", description = "說明這支 API 的用途。")
@PostMapping("/api/example")
public Map<String, Object> example(@RequestBody ExampleRequest body) {
    return Map.of("message", "success");
}
```

需要 JWT 的 Controller：

```java
@Operation(summary = "會員資料", description = "需要在 Authorization header 帶入 Bearer JWT。")
@SecurityRequirement(name = "bearerAuth")
@GetMapping("/api/member/profile")
public Map<String, Object> profile(@RequestHeader("Authorization") String authorizationHeader) {
    return Map.of("message", "success");
}
```
