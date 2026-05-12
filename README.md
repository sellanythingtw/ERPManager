# InventoryLocalApp v0.1.0

本專案為「本地端進銷存系統」第一版程式骨架。

## 技術方向

- Java 21
- Spring Boot 3.3.x
- Thymeleaf + Bootstrap/自訂 CSS
- SQLite 本地資料庫
- OpenPDF 產生 PDF
- ZXing 產生 Code128 條碼
- Apache POI 預留 Excel 匯入/匯出
- Google Drive API 預留 PDF / 庫存表上傳

## 參考既有專案後採用的風格

- package 使用 `com.gigastone.*`
- 保留 `MsgUtils`、`FilesUtils` 類型的靜態工具類
- Controller 可同時提供本地網頁與 `/api/*` JSON 呼叫
- Service 負責主要交易邏輯
- Google Drive、Excel、PDF 等外部功能集中在 Service / Utils 中

## 目前已完成的骨架

- 產品 / 客戶 / 供應商 Entity + Repository + Controller
- 進貨單 Entity、進貨明細、進貨批次 `purchase_lots`
- 進貨確認時建立批次、產生條碼號碼、建立庫存異動
- 銷貨單 Entity、銷貨明細
- 銷貨刷條碼查詢進貨批次
- 銷貨數量檢查與批次扣庫
- 沖帳資料表與基礎收款流程
- PDF 產生服務初版
- 標籤 PDF 產生服務初版
- 即時庫存查詢

## 啟動方式

```bash
mvn spring-boot:run
```

啟動後開啟：

```text
http://127.0.0.1:8080
```

## 注意

此版是開發骨架，不是完整可交付版本。下一階段建議先補：

1. 進貨單明細輸入頁
2. 商品 Excel 匯入
3. 進貨確認後貼紙批次列印頁
4. 銷貨單完整頁面流程
5. 欠款篩選、列印、匯出
6. Google Drive OAuth 與實際上傳
