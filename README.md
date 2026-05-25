# HostDrivenMultiplayChatGame

這個專案是用來練習 **agentic coding** 的小型實驗專案，目標是把一個 Host-driven multiplayer action framework 的 MVP 做得可跑、可驗證、可 review。

## 專案目標

- 以 **Java 17 + Spring Boot** 建立後端服務
- 以 **React + TypeScript** 建立前端介面
- 讓 AI 與 Human Player 都經過同一套 `PlayerActionService`
- 練習把「規則放後端、UI 只讀取 availableActions」這類架構原則落實到專案中

## 目前內容

- 以「AI 混入聊天室 / 精簡版 AI 狼人殺」為第一個 demo 模式
- 使用 in-memory store，不引入資料庫、Redis、登入或完整狼人殺職業系統
- 重點放在：
  - Host-driven flow
  - WebSocket / STOMP
  - action 驗證
  - cooldown
  - stage transition
  - 額外可擴充的 architecture boundary

## 相關文件

- `DEMO_GUIDE.md`：本機 demo 的操作步驟
- `MVP_PHASE_PLAN.md`：專案的 MVP 規劃與開發順序
- `AGENTS.md`：本專案的 agent / coding workflow 規則
- `TEMP_SCOPE_NOTE.md`：暫時收斂說明（原始 AI 題目不變，MVP 過渡期可先用真人叛徒模式）

## 技術堆疊

- Backend：Java 17, Spring Boot, Spring Web, Spring WebSocket
- Frontend：React, TypeScript, Vite
- Store：In-memory

## 快速開始

### 一鍵啟動前後端（Windows PowerShell）

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-dev.ps1
```

會自動開兩個新視窗，標題分別是：
- `HostGame Backend (Spring Boot)`
- `HostGame Frontend (Vite)`

### 後端

```powershell
mvn spring-boot:run
```

預設後端會啟動在 `http://127.0.0.1:8080`。

### 前端

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

預設前端會啟動在 `http://127.0.0.1:5173`。

## 學習重點

這個專案的主要學習目標不是做出完整產品，而是練習以下幾件事：

- 用 agent 協助拆解需求、整理規格與產出程式
- 建立清楚的 domain / service / DTO 邊界
- 保持後端規則唯一來源，避免把遊戲規則寫在前端
- 透過 demo 與測試驗證流程是否符合設計

## 注意事項

- 前端只根據後端提供的 `availableActions` 決定可顯示的 UI
- 所有玩家操作都必須送到後端驗證
- 遊戲結束前，不應揭露玩家是否為 AI / Human
- 建立房間時可設定目標（`找出叛徒` / `找出AI`）、討論秒數、投票秒數、發言 CD、最大輪次
- 目前預設目標為 `找出叛徒`；只有目標為 `找出AI` 時才允許加入 AI 玩家

若你想繼續擴充這個專案，可以先從 `MVP_PHASE_PLAN.md` 裡的下一個 phase 開始。
