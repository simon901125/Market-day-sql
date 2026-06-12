# Market Day SQL

本資料夾存放小集日 Market Day 專案的 SQL Server 資料庫結構、ERD 圖與測試資料。

## 主要檔案

### `MarketDayDB.sql`

`MarketDayDB.sql` 是主要資料庫建置腳本，用來建立 `MarketDayDB` 所需的資料表、主鍵、外鍵、索引、預設值與欄位限制。

目前包含的核心資料表例如：

- `users`：使用者帳號資料，包含 LOCAL / GOOGLE 登入來源、角色、狀態等欄位
- `categories`：活動與攤位共用分類，不再用 `type` 區分分類用途
- `vendors`：攤位資料，包含負責人姓名、縣市、區與詳細地址
- `vendor_images`：攤位圖片，圖片類型包含 `AVATAR`、`COVER`、`GALLERY`
- `vendor_products`：攤位商品
- `market_events`：市集活動，包含發布狀態與活動審核狀態
- `event_sessions`：活動場次
- `event_images`：活動圖片
- `event_stall_zones`：活動攤位分區
- `event_stalls`：活動攤位，可記錄攤位尺寸、編號與選位狀態
- `event_applications`：攤主活動報名，包含選擇攤位、保證金與報名審核備註
- `application_sessions`：報名場次
- `payments`：付款紀錄
- `refunds`：退款紀錄，關聯報名與原付款紀錄
- `notifications`：通知
- `request_logs`：API request 紀錄

建置新資料庫時，建議先執行此檔案。

### `MarketDayDB_ERD.png`

`MarketDayDB_ERD.png` 是 `MarketDayDB.sql` 對應的 ERD 圖，用來快速查看資料表之間的關聯。

建議在閱讀或修改 SQL schema 前先查看 ERD，確認：

- 各資料表的主要責任
- foreign key 關聯方向
- 使用者、攤位、活動、攤位分區、活動攤位、報名、付款、退款之間的流程關係
- 修改資料表時可能影響到的相依表

## 輔助檔案

### `test.sql`

`test.sql` 放置測試資料，目前主要用於建立基本 `users` 測試帳號。

建議在執行完 `MarketDayDB.sql` 建立資料表後，再執行 `test.sql`。

### `categories.sql`

`categories.sql` 放置前台分類篩選會使用的共用分類資料，包含：

```text
餐飲美食、文創手作、親子家庭、寵物生活、植物選物、服飾配件、玩具選物
```

建議在執行完 `MarketDayDB.sql` 建立資料表後，再執行 `categories.sql`。

### `dropDB.sql`

`dropDB.sql` 用於刪除或重建資料庫前的清理作業。

執行前請確認目前資料庫中的資料可以被刪除，避免誤刪開發或測試資料。

## 建議執行順序

首次建立資料庫：

```text
1. MarketDayDB.sql
2. categories.sql
3. test.sql
```

需要重新建立資料庫：

```text
1. dropDB.sql
2. MarketDayDB.sql
3. categories.sql
4. test.sql
```

## 與後端程式的關聯

Spring Boot 專案目前透過 `application.properties` 連線到：

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=MarketDayDB;encrypt=true;trustServerCertificate=true
```

因此執行後端 API 前，需先確認：

- SQL Server 已啟動
- `MarketDayDB` 已建立
- `MarketDayDB.sql` 已成功執行
- `users` 等必要資料表存在
- 若要測試登入流程，已透過 API 註冊帳號，或執行 `test.sql` 建立測試帳號

## 修改 schema 的注意事項

修改 `MarketDayDB.sql` 時，請同步檢查：

- `MarketDayDB_ERD.png` 是否需要更新
- 後端 Java class 是否需要新增或調整欄位
- API request / response 是否需要更新
- Swagger schema 是否需要同步更新
- `test.sql` 是否需要補測試資料

近期 schema 已補上活動審核、攤位選位、保證金與退款流程；若後端開始實作這些功能，需同步檢查相關 Entity / DTO / API 文件是否已包含 `review_status`、`review_note`、`selected_stall_id`、`deposit_amount`、`deposit_status`、`event_stalls` 與 `refunds`。

若變更 `users` 欄位，請特別檢查：

- `users.java`
- `UserController.java`
- `AuthService.java`
- `JwtService.java`
- `demo/src/main/java/com/example/demo/swagger/*Request.java`
