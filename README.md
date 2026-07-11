# Market Day SQL

本資料夾存放小集日 Market Day 專案的 SQL Server 資料庫結構、ERD 圖與測試資料。

## 更新日誌

> 更新日誌請依日期與 branch 分區：日期使用 `###`，branch 使用 `####`，越近的更新放越上面，避免不同分支的更動混在同一段。

### 2026-07-11

#### simon branch

- 新增 `test10.sql`，提供公開品牌 API 測試資料：50 筆品牌、每品牌 10 筆商品、10 筆測試活動，每個品牌皆參與所有測試活動，活動時間包含已結束、進行中與未開始。
- 新增 `test11.sql`，提供主辦方 profile API 測試資料：建立 1 筆主辦方帳號、共用聯絡/地址資料與主辦方公司/服務時間資料；測試帳號為 `organizer1@example.test`，密碼為 `a12345678`。
- `market_events` 移除 `traffic_info`，交通資訊改由 `event_traffic_infos` 保存。
- 新增 `event_traffic_infos`，以 `event_id` 關聯活動，並用 `traffic_title`、`traffic_details` 保存多筆交通方式與詳細資訊。
- `event_applications` 移除 `review_note`，報名退件原因改由 `application_review_notes` 保存。
- 新增 `application_review_notes`，以 `application_id` 關聯報名，並用 `review_note`、`review_note_detail` 保存一組退件原因與詳細說明。
- `vendor_profiles` 新增 `brand_summary` 保存品牌簡述，原 `brand_description` 語意調整為品牌介紹。
- `vendor_profiles` 新增 `brand_name` 保存品牌名稱；品牌名稱不再使用 `user_profiles.name`。
- `vendor_profiles` 移除 `brand_type`，品牌類型統一由 `category_id` 關聯 `categories` 取得。
- `vendor_profiles` 移除 `product_summary`，品牌特色商品改由 `vendor_products` 保存詳細資料。
- `vendor_products` 新增 `is_featured`，用來標記特色商品；顯示排序由後端查詢邏輯決定。
- `organizer_profiles` 新增 `organizer_name` 保存主辦方名稱；主辦方名稱不再使用 `user_profiles.name`。
- `user_profiles` 移除 `name`，只保留共用聯絡人、電話、Email、縣市、地區與地址等資料。
- `user_profiles.address` 只保存詳細地址原文，不負責組合縣市與地區；後端回傳主辦資料時也直接回傳此欄位。
- 後端同步注意：
  - 主辦方報名審核退件 API 不再將退件原因包成 JSON 寫入 `event_applications.review_note`。
  - `GET /api/organizer/applications/{id}` 需從 `application_review_notes` 取最新一筆退件原因並回傳 `reviewNote`、`reviewNoteDetail`。
  - 目前公開活動詳情 API `GET /api/markets/{id}` 仍有舊的 `trafficInfo` 回傳欄位，後續需改查 `event_traffic_infos`。

### 2026-07-08

#### simon branch

- `market_events` 新增 `create_at`，用來記錄活動建立時間，預設值為 `SYSDATETIME()`。
- `event_equipments` 新增 `unit`，用來記錄設備租借單位，例如 `張`、`頂`、`組`、`條`；`EQUIPMENT` 類型必填，`POWER` 類型需為 `NULL`。
- `event_equipments` 新增 `per_stall_rental_limit`，用來限制單一攤位可租借的數量上限。
- `event_equipments` 新增 `rental_status`，用來控制設備是否開放租借，允許值為 `ACTIVE`、`UNACTIVE`，預設為 `ACTIVE`。
- `equipment_rentals` 新增 `unit`，保存租借當下的設備單位快照，避免活動設備日後調整單位時影響歷史報名紀錄。
- 設備租借數量語意調整：`quantity` 表示租借幾個 `unit`，`pricing_unit` 與 `rental_units` 仍負責時間計費，例如 `2 組 x 2 天`。

### 2026-07-07

#### simon branch

- `users` 移除 `name`、`phone`，只保存登入識別、密碼、provider、狀態、自動登出與驗證時間等帳號欄位。
- `users.provider` 支援 `LOCAL`、`GOOGLE`、`BOTH`，並新增 `google_sub`；`GOOGLE/BOTH` 必須有 `google_sub`，`LOCAL` 不可有 `google_sub`。
- 新增 `UX_users_google_sub` filtered unique index，確保同一個 Google 帳號最多只能註冊或綁定一個平台帳號。
- `user_profiles.contact_name`、`contact_phone` 改為可空；註冊時只建立最小共用 profile，名稱改由 `vendor_profiles.brand_name` 或 `organizer_profiles.organizer_name` 保存。

- `payments.payment_no` 欄位註解改為「藍新的 MerchantOrderNo」，並保留唯一約束避免付款訂單號重複。
- `payments` 新增 `provider_response_code` 與 `provider_message`，用來記錄金流服務商回應代碼與訊息。
- `refunds.refund_no` 欄位註解改為「藍新退款交易序號」，並保留唯一約束避免退款交易序號重複。
- `refunds` 新增 `reason` 與 `failed_reason`，用來記錄退款原因與退款失敗原因。

### 2026-07-06

#### simon branch

- `event_equipments` 新增 `charge_type` 與 `item_type`，用來區分活動設備為免費/付費，以及一般設備/用電項目；允許值分別為 `FREE`、`PAID` 與 `EQUIPMENT`、`POWER`。

### 2026-07-04

#### simon branch

- `market_events` 狀態欄位改為單一 `workflow_status`，取代原本的 `review_status` 與 `publish_status`。
- `workflow_status` 允許值為 `DRAFT`、`PENDING_REVIEW`、`MAP_BUILDING`、`READY_TO_PUBLISH`、`PUBLISHED`、`FINAL_REVIEW`、`UNPUBLISH_REQUESTED`、`UNPUBLISHED`、`CANCELLED`。
- 新增 `brands_public_at`，品牌名單是否公開改由此時間欄位判斷，不再使用 `BRANDS_PUBLISHED` workflow 狀態。
- `PUBLISHED` 表示活動已發布並進入報名、審核、付款、選位等公開後流程；報名截止後的最終名單確認改由 `FINAL_REVIEW` 表示，品牌公開則以 `brands_public_at <= now` 判斷。
- 移除活動流程中的 `REVISION_REQUIRED`；需要補件時回到 `PENDING_REVIEW` 流程處理。

### 2026-07-02

#### simon branch

- 選位資料由 `event_applications.selected_stall_id` 改為 `application_dates.selected_stall_id`，支援同一筆申請單在不同報名日期選擇不同攤位。
- `application_dates.selected_stall_id` 新增對 `event_stalls.id` 的關聯，並以同日同攤位唯一限制避免同一天重複選位。
- `event_stalls.status` 保留為攤位本體狀態，只表示攤位是否可用；每日是否被選走改由 `application_dates.selected_stall_id` 搭配 `apply_date` 判斷。

### 2026-06-30

#### simon branch

- `event_equipments` 新增 `wattage_limit`，用來記錄電力設備的瓦數上限。
- 新增 `rental_appliances`，關聯 `equipment_rentals`，記錄 `equipment_rental_id`、`appliance_name`、`wattage`，用來保存租借電力時填寫的電器與瓦數。
- `MarketDayDB.sql` 新增 `event_equipments`，記錄活動可租借設備、租金、計費方式、詳細資訊與庫存數量。
- `MarketDayDB.sql` 新增 `equipment_rentals`，記錄報名租借設備，並保留租借當下的設備名稱、單價、計費方式、數量與小計。
- `MarketDayDB.sql` 新增 `status_logs`，透過 `request_log_id` 關聯 API 請求並記錄狀態來源與更新後狀態。

### 2026-06-29

#### simon branch

- 調整 `market_events` 活動時間欄位：
  - 移除 `start_date`、`end_date`、`start_time`、`end_time`。
  - 改為 `start_at`、`end_at`、`registration_start_at`、`registration_end_at` 四個 `DATETIME2(0)` 欄位。
  - 同步更新活動日期檢查、索引與欄位註解。
- 調整 `users.status`：
  - 允許值新增 `DISABLED`。
  - 停用帳號使用 `DISABLED`，`IS_DELETED` 保留給刪除或封存語意。
- 調整活動下架流程：
  - `market_events` 活動流程狀態支援 `UNPUBLISH_REQUESTED`。
  - 新增 `event_unpublish_requests` 記錄主辦方下架申請、下架原因、管理員審核結果與審核備註。
- 調整通知中心資料：
  - `notifications` 新增 `is_read`、`read_at`。
  - 新增 `(user_id, is_read)` 索引，支援未讀通知查詢。
- 新增管理員操作紀錄：
  - 新增 `admin_operation_logs`，支援管理員後台操作紀錄列表。
  - 欄位包含操作人、操作類型、操作對象、操作內容與操作時間。
- 刪除原本用來補狀態的 alter SQL 對齊檔，後續以 `MarketDayDB.sql` 為主建表來源。
- 後端同步注意：
  - Repository 查詢活動時間時改用 `start_at`、`end_at`。
  - 對外 API response 改用 `eventStartAt/eventEndAt` 或 `startAt/endAt`。
  - 帳號停用 API 寫入 `users.status = DISABLED`。

### 2026-06-25

#### simon branch

- 調整 `event_applications` 狀態欄位設計：
  - `review_status` 保留報名審核流程，允許值為 `PENDING`、`APPROVED`、`REJECTED`。
  - `payment_status` 改為付款流程狀態，允許值為 `PENDING`、`PAID`、`FAILED`、`EXPIRED`。
  - `deposit_status` 改為保證金退還狀態，允許值為 `NOT_RETURNED`、`RETURNED`。
  - 新增 `is_cancelled` 統一記錄報名是否取消，移除以 `review_status` 或 `payment_status` 表示取消的做法。
  - 新增 `created_at` 記錄報名建立時間。
- 調整退款狀態：
  - `refunds.refund_status` 改為 `REFUND_REQUESTED`、`REFUNDING`、`REFUND_FAILED`、`REFUNDED`。
  - 退款流程由 `refunds` 表管理，不再放在 `event_applications.review_status`。
- 調整報名日期設計：
  - 移除以 `event_sessions`、`application_sessions` 表示報名場次的設計。
  - 改由 `application_dates.apply_date` 記錄攤主實際報名參加日期。
- 後端同步注意：
  - `ApplicationStatusService` 會依取消、退款、審核、付款、選位、活動結束與保證金狀態推導前端顯示的 `applicationStatus`。
  - `保證金已退還` 需同時符合已付款、已選位、活動已結束且 `deposit_status = RETURNED`。
  - Repository 若查詢 `event_applications`，取消狀態應以 `is_cancelled` 判斷，不應再檢查 `review_status/payment_status = CANCELLED`。

## 主要檔案

### `MarketDayDB.sql`

`MarketDayDB.sql` 是主要資料庫建置腳本，用來建立 `MarketDayDB` 所需的資料表、主鍵、外鍵、索引、預設值與欄位限制。

目前包含的核心資料表例如：

- `users`：使用者登入帳號資料，包含 email、password_hash、provider（LOCAL/GOOGLE/BOTH）、google_sub、status、isLogin、expired_time、email_verified_at、created_at、updated_at；不保存 name/phone。
- `categories`：活動與品牌共用分類，不再用 `type` 區分分類用途
- `user_profiles`：攤主與主辦方共用聯絡資料，包含 contact_name、contact_phone、contact_email、地址等資料。
- `vendor_profiles`：攤主品牌專屬資料，包含品牌名稱、分類、社群連結、品牌簡述與品牌介紹
- `organizer_profiles`：主辦方專屬資料，包含主辦方名稱、公司名稱、統一編號與服務時間
- `vendor_images`：品牌圖片，關聯 `vendor_profiles`，圖片類型包含 `AVATAR`、`COVER`、`GALLERY`
- `vendor_products`：品牌商品，關聯 `vendor_profiles`，可用 `is_featured` 標記特色商品
- `market_events`：市集活動，包含活動時間、報名時間、活動流程狀態與活動建立時間
- `event_traffic_infos`：活動交通資訊，關聯 `market_events`，一個活動可保存多筆交通方式與詳細資訊
- `event_unpublish_requests`：活動下架申請，記錄主辦方申請原因與管理員審核結果
- `event_images`：活動圖片
- `event_stall_zones`：活動攤位分區
- `event_stalls`：活動攤位，可記錄攤位尺寸、編號與攤位本體狀態
- `event_equipments`：活動可租借設備與用電項目，包含租金、計費方式、租借單位、庫存、單攤租借上限、租借狀態與瓦數上限
- `event_applications`：攤主活動報名，包含保證金、報名審核狀態與報名建立時間
- `application_review_notes`：報名審核退件原因，關聯 `event_applications`，保存退件原因與詳細說明
- `equipment_rentals`：報名租借設備，保存租借當下的設備名稱、單價、計費方式、單位、數量、計費單位數與小計
- `application_dates`：報名參加日期，一筆報名可選擇活動中的多個日期，並在每個日期記錄選擇的攤位
- `payments`：付款紀錄
- `refunds`：退款紀錄，關聯報名與原付款紀錄
- `notifications`：通知，包含未讀/已讀狀態
- `admin_operation_logs`：管理員後台操作紀錄
- `request_logs`：API request 紀錄

建置新資料庫時，建議先執行此檔案。

### `MarketDayDB_ERD.png`

`MarketDayDB_ERD.png` 是 `MarketDayDB.sql` 對應的 ERD 圖，用來快速查看資料表之間的關聯。

建議在閱讀或修改 SQL schema 前先查看 ERD，確認：

- 各資料表的主要責任
- foreign key 關聯方向
- 使用者、品牌、活動、攤位分區、活動攤位、報名、付款、退款之間的流程關係
- 修改資料表時可能影響到的相依表

## 輔助檔案

### `test.sql`

`test.sql` 放置測試資料，目前主要用於建立基本 `users` 測試帳號。

建議在執行完 `MarketDayDB.sql` 建立資料表後，再執行 `test.sql`。

### `test10.sql`

`test10.sql` 放置公開品牌 API 測試資料，主要用於測試：

- `GET /api/brands/search`
- `GET /api/brands/{id}`
- `GET /api/brands/scroll-options`

資料內容包含 50 筆品牌、每品牌 10 筆商品、每品牌 2-3 筆特色商品、10 筆測試市集活動，以及每個品牌參與所有測試活動的報名資料。活動時間刻意分成已結束、進行中與未開始，方便驗證品牌詳情中的歷史參與市集只統計 `end_at < now` 的活動。

### `test11.sql`

`test11.sql` 放置主辦方 profile API 測試資料，主要用於測試：

- `GET /api/organizer/profile/load`
- `POST /api/organizer/profile/save`

測試帳號：

```json
{
  "email": "organizer1@example.test",
  "password": "a12345678"
}
```

資料內容包含一筆 `users` 主辦方帳號、一筆 `user_profiles` 聯絡與地址資料，以及一筆 `organizer_profiles` 主辦方名稱、公司/團體名稱、統一編號、服務日期與服務時間資料。

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
4. 視測試需求執行 test10.sql 或 test11.sql
```

需要重新建立資料庫：

```text
1. dropDB.sql
2. MarketDayDB.sql
3. categories.sql
4. test.sql
5. 視測試需求執行 test10.sql 或 test11.sql
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
- `test.sql`、`test3.sql`、`test10.sql`、`test11.sql` 是否需要補測試資料或調整欄位名稱

近期 schema 已調整 `users` 與帳號資料邊界：`users` 不再保存 `name`、`phone`，Google 身分使用 `google_sub` 與 `provider = GOOGLE/BOTH` 表示，使用者顯示名稱改由 `vendor_profiles.brand_name` 或 `organizer_profiles.organizer_name` 保存，聯絡資料改由 `user_profiles` 保存。若後端開始實作這些功能，需同步檢查相關 Entity / DTO / API 文件是否已包含 `expired_time`、`google_sub`、`user_profiles`、`vendor_profiles`、`organizer_profiles`、`vendor_profile_id`、`review_status`、`application_review_notes`、`application_dates.selected_stall_id`、`deposit_amount`、`deposit_status`、`event_stalls`、`event_traffic_infos`、`refunds`、`event_unpublish_requests` 與 `admin_operation_logs`。

若變更 `users` 欄位，請特別檢查：

- `users.java`
- `UserController.java`
- `AuthService.java`
- `JwtService.java`
- `demo/src/main/java/com/example/demo/swagger/*Request.java`
