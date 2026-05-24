# MVP Phase Plan

## 文件目的

這份文件把 `host_driven_multiplayer_action_framework_spec.md` 的初步構想整理成可逐輪實作、可驗收、可 code review 的 MVP 階段規劃。

本專案第一階段目標是先完成一個可跑、可驗證、邊界清楚的 Host-driven Multiplayer Action Framework。第一個遊戲模式是「AI 混入聊天室 / 精簡版 AI 狼人殺」，但架構要保留未來擴充成狼人殺、棋類或其他多人互動遊戲的空間。

## 核心原則

- Host / GameFlowEngine 控制 stage。
- 後端是唯一規則來源。
- 前端只根據 `availableActions` 顯示 UI。
- 所有玩家操作都送後端驗證。
- Human Player 與 AI Player 都走同一套 `PlayerActionService`。
- AI 不得直接修改 `GameRoom`、`Vote`、`ChatMessage` 或玩家狀態。
- 遊戲結束前，前端不可知道誰是 AI / HUMAN。
- MVP 使用 in-memory store，不主動導入 DB、Redis、登入、權限、LLM 或完整狼人殺職業。

## 建議與疑問

### 建議

1. 先把 `GameRoom`、`Player`、`ChatMessage`、`PlayerAction`、`AvailableAction` 的 domain model 做穩，再做 WebSocket 與前端。
2. `availableActions` 應從 Phase 6 開始成為前端顯示操作的唯一依據，避免前端自行判斷 stage 規則。
3. `MESSAGE_CREATED` 與 `STAGE_CHANGED` 要分開事件，不要讓前端 parse Host 訊息來推斷 stage。
4. DTO 從一開始就分成 public view 與 reveal view，避免之後才補救 AI / HUMAN 身份外洩。
5. Scheduler 只負責偵測 timeout，實際 stage transition 應集中交給 `GameFlowEngine`，降低重複轉場風險。
6. Mock AI 先做 deterministic 行為，例如固定延遲後發言或投票，方便測試，不要在 MVP 早期接 LLM。

### 目前疑問

1. 既有規範 `.md` 在目前終端輸出呈現亂碼，建議後續另外做一輪文件 encoding 檢查與修復。這不阻擋本輪規劃，但會影響日後維護與 review。
2. MVP 結束條件目前採用 `alivePlayers <= 3`，但勝負文案仍需要最後確認，例如 AI 數量為 0、1、2 時要如何描述。
3. MVP 是否允許房主手動調整討論與投票秒數？若沒有明確需求，建議先寫死在後端 config。

## Phase 0: 規格整理與工作邊界

### 目標

建立專案可執行的階段規劃，讓後續每輪 Codex 任務都能只做單一 phase。

### 任務

- 整理 MVP 範圍。
- 整理禁止事項。
- 整理 phase 順序。
- 建立本文件。

### 文件產出

- `MVP_PHASE_PLAN.md`
- 本輪 `round-summary/codex-YYYYMMDD-000N.md` round summary

### 驗收方式

- 檔案存在於 workspace root。
- 文件包含階段、任務、驗收方式與 open items。

## Phase 1: Spring Boot 專案骨架與 Domain Model

### 目標

建立後端基本骨架與核心 domain model，不實作遊戲流程。

### 任務

- 建立 Java 17 + Spring Boot 專案。
- 建立 package 結構：
  - `api`
  - `agent`
  - `domain`
  - `dto`
  - `flow`
  - `scheduler`
  - `service`
  - `store`
  - `websocket`
- 建立 domain model：
  - `GameRoom`
  - `Player`
  - `ChatMessage`
  - `PlayerAction`
  - `AvailableAction`
  - stage / status / action enum
- 建立 `GameStore` interface。
- 建立 `InMemoryGameStore`。

### 文件產出

- 更新 README 或 docs 入口，說明後端啟動方式。
- Round summary。

### 驗收方式

- 專案可以 compile。
- domain model 不包含 DB / JPA annotation。
- Host 不在 players list。
- Player public DTO 不包含 `controllerType`、role、faction。

## Phase 2: Lobby / Room API

### 目標

完成建立房間、查詢房間、加入房間與取得房間狀態的 HTTP API。

### 任務

- `POST /api/rooms`
- `GET /api/rooms?status=WAITING`
- `POST /api/rooms/{roomId}/join`
- `GET /api/rooms/{roomId}`
- 建立 `RoomService` 或 `LobbyService`。
- 建立 request / response DTO。

### 文件產出

- API 使用範例文件或 README API 區塊。
- Round summary。

### 驗收方式

- 可用 curl 建立房間。
- 可加入房間並取得 `playerId`、`playerNo`、`color`。
- 回傳 DTO 不洩漏 AI / HUMAN 身份。
- 不導入登入、DB、Redis。

## Phase 3: WebSocket / STOMP 基礎事件

### 目標

建立房間事件廣播通道，讓同一房間的 client 可以收到後端事件。

### 任務

- 建立 Spring WebSocket / STOMP 設定。
- 建立 public topic：`/topic/rooms/{roomId}`。
- 建立 private queue：`/user/queue/events`。
- 建立 `WsEvent<T>` envelope。
- 建立 `WebSocketBroadcaster`。

### 文件產出

- WebSocket topic 文件。
- Round summary。

### 驗收方式

- 兩個 client 訂閱同一房間 topic 時，都能收到後端廣播事件。
- private event 不廣播到全房間。
- `MESSAGE_CREATED`、`STAGE_CHANGED`、`AVAILABLE_ACTIONS_UPDATED` 保持事件語意分離。

## Phase 4: PlayerActionService 與 SEND_MESSAGE

### 目標

建立所有玩家操作的共同入口，並實作玩家發言。

### 任務

- 建立 `PlayerActionService.submitAction(roomId, playerId, action)`。
- 建立 `ActionType.SEND_MESSAGE`。
- 實作 `ChatService`。
- 驗證 room、player、alive status、stage、content。
- 建立 `ChatMessage`。
- 廣播 `MESSAGE_CREATED`。

### 文件產出

- Action flow 文件。
- Round summary。

### 驗收方式

- Human player 發言必須經過 `PlayerActionService`。
- 被淘汰玩家不可發言。
- 非允許 stage 不可發言。
- 前端不可直接決定發言是否合法。

## Phase 5: HOST Message

### 目標

讓 Host 可以透過正式事件產生主持人訊息，但 Host 仍不是 Player。

### 任務

- 建立 `HostService`。
- 建立 `MessageType.HOST`。
- Host message 使用 `ChatMessage` 結構。
- 廣播 Host 的 `MESSAGE_CREATED`。

### 文件產出

- Host message 行為說明。
- Round summary。

### 驗收方式

- Host message 出現在聊天室。
- Host 不在 players list。
- Host 不具備 player action。

## Phase 6: Stage 與 Start Game

### 目標

建立最小 GameFlowEngine，支援 `WAITING -> DISCUSSION`。

### 任務

- 建立 `GameFlowEngine`。
- 建立 MVP stage enum：
  - `WAITING`
  - `DISCUSSION`
  - `VOTING`
  - `ELIMINATION`
  - `ENDED`
- `POST /api/rooms/{roomId}/start`
- 設定 `stageStartedAt`、`stageEndsAt`。
- 廣播 `STAGE_CHANGED`。
- 計算並發送 `AVAILABLE_ACTIONS_UPDATED`。

### 文件產出

- Stage transition 文件。
- Round summary。

### 驗收方式

- 房間從 `WAITING` 進入 `DISCUSSION`。
- 活著的玩家取得 `SEND_MESSAGE` action。
- availableActions 由後端計算。
- 前端不靠 hard-code stage 顯示主要操作。

## Phase 7: Cooldown

### 目標

限制玩家發言頻率，且 cooldown 以後端驗證為準。

### 任務

- 在 `Player` 或 action record 記錄 `lastMessageAt`。
- 設定 MVP 發言 cooldown，例如 15 秒。
- 違規時回傳 `ACTION_REJECTED` 或 `MESSAGE_REJECTED`。
- 可選擇發送 cooldown metadata 給前端。

### 文件產出

- Cooldown 規則文件。
- Round summary。

### 驗收方式

- 使用 DevTools 或直接打 API / WebSocket 也無法繞過 cooldown。
- 前端 disable 只是提示，不是規則來源。

## Phase 8: Voting

### 目標

完成討論階段結束後進入投票階段，並允許玩家投票。

### 任務

- Scheduler 偵測 `DISCUSSION` timeout。
- `GameFlowEngine` 轉入 `VOTING`。
- Host 發送投票說明。
- 發送 `STAGE_CHANGED`。
- 計算 `SUBMIT_VOTE` availableAction。
- 實作投票送出與驗證。

### 文件產出

- Voting action 文件。
- Round summary。

### 驗收方式

- 每位活著玩家只能投一次。
- 不可投自己。
- 不可投已淘汰玩家。
- 被淘汰玩家不可投票。
- 投票必須經過 `PlayerActionService`。

## Phase 9: Elimination

### 目標

結算投票並淘汰玩家。

### 任務

- 當所有活著玩家投票完成或 timeout，進入 `ELIMINATION`。
- 統計票數。
- 處理平票 MVP 規則。
- 更新被淘汰玩家狀態。
- 廣播 `PLAYER_ELIMINATED`。
- 更新 availableActions。
- 決定回到 `DISCUSSION` 或進入 `ENDED`。

### 文件產出

- Elimination 規則文件。
- Round summary。

### 驗收方式

- 被淘汰玩家不能再發言或投票。
- 淘汰狀態由後端控制。
- AI / HUMAN 身份仍不提前洩漏。

## Phase 10: Game End

### 目標

完成 MVP 結束條件與結果揭露。

### 任務

- 當 `alivePlayers <= 3` 時進入 `ENDED`。
- 停止所有玩家 action。
- 廣播 `GAME_ENDED`。
- 遊戲結束後才回傳 reveal view。
- 顯示玩家最終身份與結果。

### 文件產出

- Game end / reveal 文件。
- Round summary。

### 驗收方式

- 結束前 DTO 不洩漏 AI / HUMAN。
- 結束後才揭露 `controllerType`。
- `ENDED` 後不接受 `SEND_MESSAGE` 或 `SUBMIT_VOTE`。

## Phase 11: React Lobby

### 目標

建立前端 lobby，可以建立、瀏覽與加入房間。

### 任務

- 建立 React + TypeScript + Vite 前端。
- 建立 Lobby page。
- 建立 room card。
- 建立 create room / join room 流程。
- 串接 Phase 2 HTTP API。

### 文件產出

- 前端啟動文件。
- Round summary。

### 驗收方式

- 使用 UI 可建立房間。
- 使用 UI 可加入房間。
- 不在前端硬寫遊戲規則。

## Phase 12: React Game Room

### 目標

完成聊天室、玩家列表、Host 訊息、stage 顯示與 action UI。

### 任務

- 建立 Game Room page。
- 串接 WebSocket。
- 顯示 public room events。
- 顯示 player list。
- 顯示 chat messages。
- 根據 `availableActions` 顯示發言與投票 UI。
- 顯示淘汰與結束結果。

### 文件產出

- 前端事件處理文件。
- Round summary。

### 驗收方式

- 兩個瀏覽器可進同一房間並同步訊息。
- 操作 UI 由 `availableActions` 決定。
- 前端不在結束前顯示 AI / HUMAN。

## Phase 13: AgentController 與 Mock AI

### 目標

預留 AI Player 控制器，並建立不依賴 LLM 的 Mock AI。

### 任務

- 建立 `AgentController` interface。
- 建立 `NoopAgentController`。
- 建立 `MockAgentController`。
- 支援 AI player 加入房間。
- Mock AI 收到 availableActions 後透過 `PlayerActionService` 發言或投票。

### 文件產出

- AgentController 設計文件。
- Round summary。

### 驗收方式

- AI action 一律經過 `PlayerActionService`。
- AI 不直接修改 domain state。
- AI 受到 stage、alive、cooldown、vote validation 限制。
- 前端結束前仍不知道誰是 AI。

## Phase 14: MVP Review 與測試補強

### 目標

在引入更複雜功能前，補齊 MVP 的回歸測試與架構檢查。

### 任務

- 補 `RoomService` 測試。
- 補 `PlayerActionService` 測試。
- 補 cooldown 測試。
- 補 vote / elimination 測試。
- 補 DTO leakage 測試。
- 檢查 WebSocket event 分流。
- 檢查 Scheduler 重複轉場風險。

### 文件產出

- MVP review note。
- Round summary。

### 驗收方式

- 測試可執行。
- 已知風險列在文件與 round summary。
- 未引入 MVP 禁止項目。

## Phase 15: MVP 之後的擴充方向

### 目標

列出可以延後做的方向，不在 MVP 內實作。

### 可能方向

- LLM AgentController。
- 完整狼人殺角色。
- faction / private message。
- reconnect。
- RedisGameStore。
- Database persistence。
- 中國象棋或其他遊戲模式。
- 更完整的 GameRuleEngine。

### 驗收方式

- 本 phase 僅規劃，不直接實作。
- 每個擴充方向都要先獨立寫規格，再進入實作。

## 每輪 Code Review 必查

- 是否把遊戲規則寫在前端。
- 前端是否在遊戲結束前拿到 AI / HUMAN 身份。
- HOST 是否被錯誤放進 players list。
- AI 是否直接修改 `GameRoom`、`Vote`、`ChatMessage`。
- 所有 action 是否都有走 `PlayerActionService`。
- 被淘汰玩家是否仍可能發言或投票。
- cooldown 是否只靠前端。
- Scheduler 是否可能重複觸發 stage transition。
- `MESSAGE_CREATED` 與 `STAGE_CHANGED` 是否混在一起。
- `availableActions` 是否由後端計算。
- private event 是否錯誤廣播到全房間。
- 是否引入未經要求的 DB / Redis / 登入 / 權限系統。
- DTO 是否洩漏 internal state。
- 測試是否真的執行過。

## 文件目錄規則

- `round-summary/`：放置每輪 `codex-YYYYMMDD-000N.md` round summary。
- `archived/`：放置已被新規劃取代、或不希望後續 AI 每輪重新參考的舊文件。
- 專案根目錄保留目前仍會被執行或 review 直接使用的入口文件，例如 `AGENTS.md`、`MVP_PHASE_PLAN.md`。
