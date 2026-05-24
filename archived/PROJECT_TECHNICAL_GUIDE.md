# PROJECT_TECHNICAL_GUIDE.md

本文件定義本專案的框架、套件、開發限制與實作邊界。

本文件是給 Codex / Copilot / 開發者使用的技術限制文件。  
產品與玩法細節請參考 `SPEC.md` 或主規格文件。

---

# 1. 專案目標

建立一個 Host-driven Multiplayer Action Framework 的 MVP。

第一個遊戲模式：

> AI 混入聊天室 / 精簡版 AI 狼人殺

但架構應保留未來擴充能力：

- 完整狼人殺
- 象棋
- 誰是臥底
- 阿瓦隆
- 其他多人互動遊戲

---

# 2. 第一階段技術選型

## Backend

使用：

- Java 17
- Spring Boot
- Spring Web
- Spring WebSocket / STOMP
- Spring Scheduling
- In-memory store

不使用：

- DB
- Redis
- Kafka
- Spring Security
- JPA / Hibernate
- OAuth / JWT
- Docker / Kubernetes

---

## Frontend

使用：

- React
- TypeScript
- STOMP WebSocket client
- Vite 可接受

不要求：

- 複雜 UI framework
- 狀態管理框架
- SSR
- Next.js
- i18n
- 手機版最佳化

MVP 前端可以簡單，但必須能清楚驗證流程。

---

# 3. Repository 建議結構

若採用前後端同 repo，可使用：

```text
project-root/
  ├─ backend/
  ├─ frontend/
  ├─ docs/
  ├─ SPEC.md
  ├─ AGENTS.md
  ├─ PROJECT_TECHNICAL_GUIDE.md
  └─ codex-YYYYMMDD-000N.md
```

若一開始只做 backend，也可以先保留：

```text
project-root/
  ├─ src/
  ├─ docs/
  ├─ SPEC.md
  ├─ AGENTS.md
  └─ PROJECT_TECHNICAL_GUIDE.md
```

---

# 4. Backend package 建議

```text
com.example.hostgame
  ├─ api
  ├─ agent
  ├─ domain
  ├─ dto
  ├─ flow
  ├─ service
  ├─ store
  ├─ websocket
  └─ scheduler
```

說明：

- `domain`：GameRoom、Player、ChatMessage、Stage、Action 等核心模型
- `dto`：傳給前端的 view model，不可直接暴露 domain internal state
- `api`：REST API
- `websocket`：WebSocket/STOMP event 接收與廣播
- `service`：ChatService、RoomService、PlayerActionService
- `flow`：HostService、GameFlowEngine、Stage transition
- `store`：InMemoryGameStore
- `scheduler`：檢查 stage timeout
- `agent`：AgentController interface 與 Mock / Noop 實作

---

# 5. 核心模組責任

## LobbyService

負責：

- 建立房間
- 查詢等待中房間
- 加入房間前檢查
- 房間列表 DTO

不負責：

- 遊戲規則
- 聊天
- 投票
- stage transition

---

## RoomService

負責：

- 房間 lifecycle
- 玩家加入
- 查詢房間公開狀態
- 開始遊戲入口

不直接處理：

- player action validation
- chat message validation
- vote resolution

---

## HostService / GameFlowEngine

負責：

- stage transition
- 主持人公告
- stage timeout
- completion rule
- system action
- availableActions 計算觸發

Host 不是 Player。

---

## PlayerActionService

所有玩家操作的統一入口。

負責：

- 接收 PlayerAction
- 驗證 player 是否可執行該 action
- 呼叫對應 service
- 記錄 action
- 觸發 completionRule 檢查

Human Player 與 AI Player 都必須走這裡。

---

## ChatService

負責：

- SEND_MESSAGE
- cooldown
- content validation
- 建立 ChatMessage
- audience routing
- MESSAGE_CREATED event

不得處理：

- stage transition
- vote resolution
- AI 決策

---

## VoteService

負責：

- SUBMIT_VOTE
- vote validation
- vote record
- vote resolution
- elimination candidate

不負責：

- 主持人公告文字
- WebSocket 直接廣播

---

## AvailableActionService

負責：

- 根據 room.currentStage 與 player 狀態計算可用操作
- 建立 AvailableAction DTO
- future：依 role / faction / visibility rule 計算 action

---

## WebSocketBroadcaster

負責：

- 房間廣播
- 個人事件
- WebSocket event envelope

不負責：

- 規則判斷
- 狀態轉移
- action validation

---

## AgentController

負責：

- 接收 AI player 的 availableActions
- 根據可見上下文決定是否提交 action

不得：

- 直接改 GameRoom
- 直接寫入 messages
- 直接寫入 votes
- 直接改 player status

---

# 6. WebSocket 通道設計

## 房間公開事件

```text
/topic/rooms/{roomId}
```

用於：

- MESSAGE_CREATED public
- HOST public message
- STAGE_CHANGED public
- ROOM_STATE_UPDATED
- PLAYER_ELIMINATED
- GAME_ENDED

## 個人事件

```text
/user/queue/events
```

用於：

- AVAILABLE_ACTIONS_UPDATED
- ACTION_REJECTED
- MESSAGE_REJECTED
- COOLDOWN_UPDATED
- VOTE_ACCEPTED
- private role info
- future private inspection result

注意：

不應把 private event 廣播到 `/topic/rooms/{roomId}`。

---

# 7. DTO 安全規則

遊戲結束前，傳給前端的 Player DTO 不可包含：

- controllerType
- AI / HUMAN
- hidden role
- hidden faction
- private notes
- internal flags

遊戲中 PlayerPublicView 只允許：

```text
playerId
playerNo
color
status / alive
```

遊戲結束後才可使用 PlayerRevealView：

```text
playerNo
controllerType
role / faction if applicable
alive
```

---

# 8. Event 設計要求

事件必須使用 envelope：

```json
{
  "type": "EVENT_TYPE",
  "payload": {},
  "createdAt": "2026-05-24T20:00:00+08:00"
}
```

MVP 事件：

- MESSAGE_CREATED
- STAGE_CHANGED
- AVAILABLE_ACTIONS_UPDATED
- ACTION_REJECTED
- MESSAGE_REJECTED
- PLAYER_ELIMINATED
- GAME_ENDED
- ROOM_STATE_UPDATED

原則：

- `MESSAGE_CREATED` 給聊天室顯示
- `STAGE_CHANGED` 給 UI 控制
- `AVAILABLE_ACTIONS_UPDATED` 給 UI 顯示可操作功能
- 不要讓前端 parse HOST message 來判斷 UI

---

# 9. Stage MVP 規則

MVP 固定流程：

```text
WAITING
DISCUSSION
VOTING
ELIMINATION
ENDED
```

## DISCUSSION

- 存活玩家可 SEND_MESSAGE
- 發言冷卻 15 秒
- 時間到進 VOTING

## VOTING

- 存活玩家可 SUBMIT_VOTE
- 每人一票
- 不可投自己
- 只能投存活玩家
- 全員完成或時間到進 ELIMINATION

## ELIMINATION

- 系統統計票數
- 得票最高者淘汰
- 平票先隨機淘汰
- 若 alivePlayers <= 3，進 ENDED
- 否則回 DISCUSSION

## ENDED

- 停止所有 action
- 公布結果

---

# 10. Memory store 限制

MVP 使用 in-memory。

接受限制：

- 伺服器重啟房間消失
- 不保留歷史紀錄
- 不支援多台伺服器
- 不支援完整 reconnect

但程式應預留抽象：

```java
interface GameStore {
    GameRoom getRoom(String roomId);
    void saveRoom(GameRoom room);
    List<GameRoom> findRooms(RoomStatus status);
    List<GameRoom> getActiveRooms();
}
```

---

# 11. Scheduler 要求

GameScheduler 使用固定頻率掃描 active rooms。

要求：

- 不可重複觸發同一 stage
- transition method 必須檢查 currentStage
- timeout 時由 GameFlowEngine 處理，不在 scheduler 中直接改狀態
- stage transition 應集中在 GameFlowEngine

---

# 12. 測試要求

MVP 至少應逐步補：

- RoomService test
- PlayerActionService test
- ChatService cooldown test
- VoteService test
- GameFlowEngine transition test
- DTO leakage test
- eliminated player action rejection test

若暫時無測試，round summary 必須寫明。

---

# 13. 開發限制總表

禁止主動加入：

| 類型 | MVP 是否允許 |
|---|---|
| DB | 不允許 |
| Redis | 不允許 |
| 登入 | 不允許 |
| Spring Security | 不允許 |
| JPA | 不允許 |
| LLM API | 不允許，僅預留介面 |
| 完整狼人殺職業 | 不允許 |
| Docker | 不允許 |
| Kubernetes | 不允許 |
| 多伺服器 | 不允許 |
| reconnect 完整恢復 | 不允許 |
| generic DSL | 不允許 |

---

# 14. 可接受的 MVP 簡化

可接受：

- 無登入
- 房間只存在 memory
- 所有人都可以按開始遊戲
- 無房主
- 無歷史紀錄
- 無 reconnect
- 無手機版最佳化
- 無內容審核
- 平票隨機淘汰
- 大廳用 HTTP 輪詢
- AI 先不做或只做 Mock

---

# 15. 不可妥協的 MVP 原則

不可接受：

- 前端決定規則
- 前端提前知道 AI 身份
- HOST 是 Player
- AI 直接改狀態
- 被淘汰玩家仍可透過 API 發言
- cooldown 只靠前端
- private event 廣播給全房間
- scheduler 重複結算
- Codex 自行引入 DB / Redis / 登入
