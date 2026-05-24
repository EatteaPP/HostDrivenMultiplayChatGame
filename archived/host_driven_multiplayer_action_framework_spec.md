# Host-driven Multiplayer Action Framework 規劃書

> 目的：  
> 本文件彙整目前討論出的所有想法與架構判斷，作為交給 Codex 進行整體規劃與分階段開發的基礎規格。
>
> 第一階段目標不是做完整產品，也不是一次完成狼人殺，而是建立一個可運作、可擴充、可驗證的多人房間制互動遊戲 MVP。  
> 初期可以先做「AI 混入聊天室／精簡版狼人殺」，但架構要保留未來擴充成狼人殺、象棋或其他回合制互動遊戲的彈性。

---

# 1. 專案定位

本專案的核心不是單純聊天室，也不是一開始就做完整狼人殺。

目前收斂出的核心定位是：

> Host-driven Multiplayer Action Framework  
> 房間制、主持人驅動、後端驗證、前端依授權 action 顯示操作 UI 的多人互動遊戲框架

第一個 MVP 遊戲可以是：

> AI 混入聊天室 / 精簡版 AI 狼人殺

但底層架構應可支援：

- 精簡版狼人殺
- 完整狼人殺
- 誰是臥底
- 阿瓦隆
- 秘密希特勒
- 象棋
- 其他回合制、階段制、資訊不對稱互動遊戲

---

# 2. 目前核心想法整理

## 2.1 使用者端流程

使用者打開站台後，預設進入遊戲大廳。

遊戲大廳功能：

1. 顯示目前等待中的房間清單
2. 房間以 card view 顯示
3. 使用者可建立新房間
4. 使用者可加入等待中的房間
5. 是否提供註冊功能可暫緩決定，MVP 可不做登入

房間建立規則：

1. 每次建立房間產生一個 UUID
2. 房間加入主頁大廳列表
3. 房間初期狀態為 `WAITING`
4. 每個房間自動配置一個系統主持人 / 房間管理員

---

## 2.2 房間主持人 / 房間管理員

使用者所稱的「房間管理員」，本質上是：

> 系統自動控制流程的主持人

主持人不是玩家，不應由人操作。

主持人的責任：

1. 控制遊戲流程
2. 管理目前 stage / phase
3. 根據狀態開放玩家可執行的 action
4. 推送當前狀態給所有 client
5. 發送主持人公告到聊天室
6. 依照玩家動作或時間倒數推進到下一階段
7. 執行系統行為，例如投票結算、淘汰、死亡、勝負判定
8. 在特定條件達成時結束遊戲

主持人不應該交給人操作，原因：

- 真人可能忘記推進
- 真人可能誤判規則
- 真人可能惡意操作
- 真人操作會造成標準不一致
- 系統自動主持比較穩定且可重現

---

# 3. 核心架構原則

## 3.1 後端是唯一規則來源

瀏覽器端只負責：

- 顯示畫面
- 顯示可用操作
- 顯示聊天訊息
- 顯示玩家狀態
- 顯示倒數
- 把使用者 action 送到後端

瀏覽器端不負責：

- 判斷遊戲規則
- 判斷 action 是否合法
- 判斷玩家是否能發言
- 判斷玩家是否能投票
- 判斷遊戲是否結束
- 判斷訊息可見範圍
- 直接改變其他玩家狀態

所有操作都必須送到後端，由後端驗證並套用結果。

---

## 3.2 前端只根據 availableActions 顯示操作

玩家目前可以做什麼，不由前端自行推導。

正確模式：

```text
後端根據 currentStage、player 狀態、role、faction、alive 等資訊
計算出每位玩家的 availableActions
前端收到後，只負責渲染對應 UI
```

例如：

```json
{
  "type": "AVAILABLE_ACTIONS_UPDATED",
  "payload": {
    "playerId": "player-001",
    "stage": "VOTING",
    "availableActions": [
      {
        "actionType": "SUBMIT_VOTE",
        "targets": [
          { "playerId": "p-002", "playerNo": 2 },
          { "playerId": "p-003", "playerNo": 3 }
        ]
      }
    ]
  }
}
```

前端看到 `SUBMIT_VOTE` 才顯示投票 UI。

錯誤模式：

```text
前端看到 currentStage = VOTING
自行判斷可以投票
```

---

## 3.3 聊天訊息與控制事件分離

主持人進入新階段時，應同時發出兩種不同用途的事件。

### 聊天訊息事件

給人看的。

例如：

```json
{
  "type": "MESSAGE_CREATED",
  "payload": {
    "messageType": "HOST",
    "speakerName": "主持人",
    "content": "投票階段開始，請在 60 秒內投票。未完成視同棄權，所有人完成投票後自動進入下一階段。",
    "createdAt": "2026-05-24T20:00:00+08:00"
  }
}
```

用途：

- 顯示在聊天室
- 人類玩家看懂狀態
- AI Agent 也可把它當成公開上下文

### 遊戲控制事件

給前端 UI 或 Agent 控制用。

例如：

```json
{
  "type": "STAGE_CHANGED",
  "payload": {
    "stage": "VOTING",
    "stageEndsAt": "2026-05-24T20:01:00+08:00"
  }
}
```

用途：

- 改變 UI 狀態
- 顯示投票選單
- 開始倒數
- 停用或啟用輸入框
- 通知 AI Agent 目前可用 action

前端不得靠解析主持人文字來控制 UI。

---

## 3.4 所有 action 都走同一個後端入口

不論是人類玩家或 AI 玩家，最終都應提交相同格式的 `PlayerAction`。

人類玩家：

```text
Browser UI -> WebSocket/API -> Backend
```

AI 玩家：

```text
AgentController -> Internal Action Gateway -> Backend
```

最後都走：

```text
PlayerActionService.submitAction(roomId, playerId, action)
```

後端統一檢查：

- 房間是否存在
- 玩家是否存在
- 玩家是否屬於該房間
- 玩家是否存活
- 目前 stage 是否允許該 action
- 玩家是否在 allowed actors 內
- action payload 是否合法
- 目標是否合法
- 冷卻時間是否已過
- 是否已完成本階段必要 action

AI 不得直接修改：

- GameRoom
- ChatMessage
- Vote
- Player state
- Stage state
- 任一遊戲狀態

---

# 4. Stage / Phase 模型

## 4.1 基本概念

遊戲流程由主持人依照 stage 推進。

每個 stage 定義：

- stageCode
- stageName
- durationSeconds
- onEnterHostMessages
- allowedActions
- allowedSpeakers
- visibility rules
- completionRule
- onCompleteSystemAction
- nextStage

MVP 可以先寫死流程，不需要做成通用 DSL。

未來可逐步抽象成 `GameFlowConfig`。

---

## 4.2 MVP 精簡版 AI 混入聊天室流程

第一版流程可先固定為：

```text
WAITING
  ↓ startGame
DISCUSSION
  ↓ time up
VOTING
  ↓ all voted or timeout
ELIMINATION
  ↓ resolve vote
DISCUSSION
  ↓ loop until alivePlayers <= 3
ENDED
```

### WAITING

- 玩家可加入房間
- 可開始遊戲
- 不可聊天或投票
- 不觸發 AI

### DISCUSSION

- 存活玩家可發言
- 每位玩家發言有冷卻時間
- 不可投票
- 時間到進入 VOTING

### VOTING

- 存活玩家可投票
- 可設定是否允許聊天
- 每位玩家只能投一次
- 所有人完成投票或時間到，進入 ELIMINATION

### ELIMINATION

- 系統統計票數
- 淘汰得票最高玩家
- 平票規則可先隨機淘汰
- 主持人公告淘汰結果
- 檢查是否結束遊戲
- 若未結束，回到 DISCUSSION

### ENDED

- 停止所有玩家 action
- 公布身份
- 公布結果

---

## 4.3 Stage completionRule

每個 stage 可用不同條件結束。

### TIMEOUT_ONLY

純時間倒數。

適合：

- 討論階段
- 思考階段

例：

```json
{
  "type": "TIMEOUT_ONLY",
  "durationSeconds": 300
}
```

---

### ALL_ACTIONS_COMPLETED_OR_TIMEOUT

所有必要玩家完成 action，或時間到。

適合：

- 投票階段
- 多人確認階段

例：

```json
{
  "type": "ALL_ACTIONS_COMPLETED_OR_TIMEOUT",
  "requiredAction": "SUBMIT_VOTE",
  "durationSeconds": 60
}
```

規則：

- 所有必要玩家完成指定 action -> 立即推進
- 時間到 -> 未完成者視同棄權，推進下一階段

---

### ROLE_ACTION_COMPLETED_OR_TIMEOUT

指定角色完成 action，或時間到。

適合狼人殺未來擴充：

- 預言家查驗
- 女巫用藥
- 守衛守護
- 狼人擊殺

例：

```json
{
  "type": "ROLE_ACTION_COMPLETED_OR_TIMEOUT",
  "role": "SEER",
  "requiredAction": "INSPECT_PLAYER",
  "durationSeconds": 30
}
```

---

### IMMEDIATE

進入後立即執行系統行為。

適合：

- 投票結算
- 夜晚結算
- 淘汰結算
- 勝負判定

例：

```json
{
  "type": "IMMEDIATE",
  "onCompleteSystemAction": "RESOLVE_VOTE"
}
```

---

# 5. 房間與大廳設計

## 5.1 Lobby

大廳負責：

- 顯示等待中的房間
- 建立房間
- 加入房間
- 顯示房間人數
- 顯示房間狀態

MVP 可用 HTTP 查詢房間列表，不一定需要 Lobby WebSocket。

建議 API：

```http
GET /api/rooms?status=WAITING
POST /api/rooms
POST /api/rooms/{roomId}/join
```

---

## 5.2 GameRoom

房間負責：

- 玩家清單
- 目前 stage
- 聊天訊息
- 可用 action
- 投票資料
- 淘汰狀態
- AI 玩家
- 主持人公告
- 遊戲流程設定

建議資料模型：

```java
class GameRoom {
    UUID roomId;
    GameType gameType;
    RoomStatus roomStatus;
    GameStage currentStage;

    List<Player> players;
    List<ChatMessage> messages;
    List<PlayerActionRecord> actionRecords;

    Instant createdAt;
    Instant startedAt;
    Instant stageStartedAt;
    Instant stageEndsAt;
    Instant endedAt;

    int round;

    GameState gameState;
    GameFlowConfig flowConfig;
}
```

---

# 6. Player 模型

## 6.1 控制來源與遊戲角色應分離

玩家有兩種不同概念：

### 控制來源

```java
enum PlayerControllerType {
    HUMAN,
    AI
}
```

表示這個玩家由誰操作：

- HUMAN：瀏覽器使用者
- AI：後端 AgentController

### 遊戲角色

MVP 可先不做完整角色。

未來可加：

```java
enum GameRole {
    VILLAGER,
    WEREWOLF,
    SEER,
    WITCH,
    HUNTER,
    GUARD
}
```

玩家可以是：

- 真人控制的村民
- AI 控制的狼人
- 真人控制的預言家
- AI 控制的女巫

控制來源與遊戲角色不可混在同一個 enum 裡。

---

## 6.2 Player 基本模型

```java
class Player {
    String playerId;
    int playerNo;
    String color;

    PlayerControllerType controllerType;

    PlayerStatus status; // ALIVE / ELIMINATED / DEAD
    Instant joinedAt;
    Instant lastMessageAt;

    // future extension
    GameRole role;
    Faction faction;
}
```

MVP 前端遊戲中不可收到 `controllerType`，避免提前知道誰是 AI。

---

# 7. Host / RoomManager 設計

## 7.1 Host 不是 Player

主持人是系統角色，不是玩家。

不應該有：

- playerNo
- alive
- role
- faction
- 可投票性
- 勝負參與

Host 應該是：

```text
GameFlowEngine + HostService + RoomManager
```

---

## 7.2 Host 的責任

每次進入 stage 時，Host 必須：

1. 更新 `room.currentStage`
2. 設定 `stageStartedAt`
3. 設定 `stageEndsAt`
4. 產生 HOST ChatMessage
5. 廣播 `MESSAGE_CREATED`
6. 廣播 `STAGE_CHANGED`
7. 為每位玩家計算 `availableActions`
8. 對每位玩家發送 `AVAILABLE_ACTIONS_UPDATED`
9. 若該玩家是 AI，通知 `AgentController`
10. 設定 stage completion 檢查

玩家提交 action 後，Host / GameFlowEngine 必須：

1. 驗證 action
2. 記錄 action
3. 套用 action 結果
4. 檢查 completionRule 是否達成
5. 若達成，執行 systemAction
6. 推進到下一 stage

時間到時，GameScheduler 必須：

1. 掃描 active rooms
2. 檢查 `stageEndsAt`
3. 若逾時，觸發 Host 完成該 stage
4. 推進下一 stage

---

# 8. ChatMessage 與訊息同步

## 8.1 聊天訊息類型

```java
enum MessageType {
    PLAYER,
    HOST
}
```

PLAYER：

- 玩家發出的訊息
- 受 stage、alive、cooldown、audience 規則控制

HOST：

- 系統主持人公告
- 不受冷卻限制
- 由系統產生

---

## 8.2 ChatMessage 模型

```java
class ChatMessage {
    String messageId;
    String roomId;

    MessageType messageType;
    MessageAudience audience;

    String playerId;       // only for PLAYER
    Integer playerNo;      // only for PLAYER
    String speakerName;    // for HOST, e.g. "主持人"

    String content;
    Instant createdAt;
}
```

---

## 8.3 訊息同步方式

聊天訊息不是前端互相傳。

正確流程：

```text
1. 玩家在瀏覽器輸入訊息
2. 前端透過 WebSocket/API 將 SEND_MESSAGE action 送到後端
3. 後端驗證：
   - room 存在
   - player 屬於 room
   - player alive
   - currentStage 允許 SEND_MESSAGE
   - cooldown 已過
   - content 非空且長度合法
   - audience 合法
4. 後端建立 ChatMessage
5. 存入 room.messages
6. 後端根據 audience 廣播給可見玩家
7. 收到事件的瀏覽器顯示訊息
```

---

## 8.4 Spring Boot 建議同步方式

建議使用 Spring WebSocket + STOMP topic。

每個房間一個 topic：

```text
/topic/rooms/{roomId}
```

前端進入房間後訂閱：

```text
/topic/rooms/{roomId}
```

後端房間廣播：

```java
messagingTemplate.convertAndSend(
    "/topic/rooms/" + roomId,
    event
);
```

適合全房間公開事件：

- 公開聊天室訊息
- 公開主持人公告
- stage 改變
- 玩家加入
- 玩家淘汰
- 遊戲結束

個人事件可用：

```text
/user/queue/events
```

適合：

- availableActions
- message rejected
- cooldown updated
- vote accepted / rejected
- private role info
- private inspection result

---

# 9. Game Events 設計

## 9.1 建議事件類型

MVP 至少需要：

```text
MESSAGE_CREATED
ROOM_STATE_UPDATED
STAGE_CHANGED
AVAILABLE_ACTIONS_UPDATED
ACTION_ACCEPTED
ACTION_REJECTED
MESSAGE_REJECTED
PLAYER_ELIMINATED
GAME_ENDED
```

可再細分：

```text
VOTING_STARTED
VOTE_ACCEPTED
VOTE_REJECTED
COOLDOWN_UPDATED
BOARD_UPDATED
TURN_CHANGED
MOVE_APPLIED
```

但 MVP 可先統一用較少事件。

---

## 9.2 WebSocket Event Envelope

```java
class WsEvent<T> {
    String type;
    T payload;
    Instant createdAt;
}
```

範例：

```json
{
  "type": "MESSAGE_CREATED",
  "payload": {
    "messageType": "HOST",
    "speakerName": "主持人",
    "content": "遊戲開始。8 位玩家中有 2 位 AI 混入。",
    "createdAt": "2026-05-24T20:00:00+08:00"
  }
}
```

---

# 10. AvailableActions 設計

## 10.1 AvailableAction 模型

```java
class AvailableAction {
    ActionType actionType;
    List<ActionTarget> targets;
    Map<String, Object> metadata;
}
```

```java
enum ActionType {
    SEND_MESSAGE,
    SUBMIT_VOTE,

    // future
    SELECT_KILL_TARGET,
    INSPECT_PLAYER,
    USE_POTION,
    MOVE_PIECE
}
```

---

## 10.2 MVP Action

MVP AI 混入聊天室只需要：

```text
SEND_MESSAGE
SUBMIT_VOTE
```

### SEND_MESSAGE

條件：

- 玩家 alive
- stage = DISCUSSION 或 VOTING
- 未超過冷卻時間
- content 合法

### SUBMIT_VOTE

條件：

- 玩家 alive
- stage = VOTING
- 本輪尚未投票
- target alive
- target != self

---

# 11. AI Player 預留設計

## 11.1 AI 本質

AI 對系統來說不是特殊規則。

AI 只是：

> 非瀏覽器操作入口的 Player controller

人類玩家：

```text
Host 發送 availableActions -> Browser -> 使用者選擇 action -> 後端
```

AI 玩家：

```text
Host 發送 availableActions -> AgentController -> Agent 決策 action -> 後端
```

兩者最後都走同一個 `PlayerActionService`。

---

## 11.2 初期可以沒有 AI

MVP 可以先不接真 AI。

需要保留：

```java
enum PlayerControllerType {
    HUMAN,
    AI
}
```

與：

```java
interface AgentController {
    void onAvailableActionsUpdated(
        GameRoomView roomView,
        PlayerPrivateView playerView,
        List<AvailableAction> availableActions
    );
}
```

初期可實作：

- NoopAgentController：不做事
- MockAgentController：隨機發言、隨機投票
- 未來 LlmAgentController：呼叫 LLM

---

## 11.3 AI 不得直接修改狀態

AI 只能提交 action。

正確：

```text
AgentController -> PlayerActionService.submitAction()
```

錯誤：

```text
AgentController 直接 room.messages.add()
AgentController 直接 voteMap.put()
AgentController 直接 player.alive = false
```

---

## 11.4 AI 上下文可後續討論

初期只需保留彈性。

未來 AI context 可以包含：

- 目前 stage
- availableActions
- 最近公開訊息
- 主持人公告
- 玩家可見的 private info
- 自己 playerNo
- 自己 role / faction
- 自己過往 action
- 可見的歷史紀錄

但不可包含：

- 未公開身份資訊
- 其他 AI 是誰
- 不該看見的 faction chat
- 完整 GameRoom internal state

---

# 12. 資訊不對稱與 Visibility Rules

## 12.1 核心觀念

在資訊不對稱遊戲中，Host 不只控制：

- stage
- availableActions

還要控制：

- 誰可以看到什麼
- 誰可以對誰說話
- 訊息要送到哪個 audience
- 哪些資訊是 public
- 哪些資訊是 private
- 哪些資訊只限同陣營

---

## 12.2 MessageAudience

```java
enum AudienceType {
    PUBLIC,
    PRIVATE,
    FACTION,
    ROLE,
    DEAD
}
```

或：

```java
class MessageAudience {
    AudienceType type;
    Set<String> targetPlayerIds;
    Faction faction;
    GameRole role;
}
```

MVP 可先只支援：

```text
PUBLIC
```

但資料模型可以預留 audience 欄位。

---

## 12.3 不可把所有資訊丟給前端

錯誤做法：

```text
後端把所有訊息都送給所有 client
前端自行判斷要不要顯示
```

這會導致 DevTools 看穿隱藏資訊。

正確做法：

```text
後端根據 audience 決定事件要送給誰
不該看到的人根本不收到該事件
```

---

## 12.4 狼人夜晚階段範例

```text
stage = WEREWOLF_ACTION

PUBLIC HOST message:
主持人：現在是晚上，所有玩家請等待。

FACTION HOST message:
主持人：狼人請選擇今晚擊殺目標。

狼人 availableActions:
- SEND_FACTION_MESSAGE
- SELECT_KILL_TARGET

村民 availableActions:
- none
```

狼人聊天只送給狼人，不送給村民。

---

# 13. 以象棋驗證架構通用性

本架構不只適用狼人殺。

象棋也可套用同樣模型：

```text
WAITING
  ↓
GAME_STARTED
  ↓
PLAYER_TURN(red)
  ↓
PLAYER_TURN(black)
  ↓
loop
  ↓
ENDED
```

主持人責任：

- 宣布遊戲開始
- 宣布輪到紅方
- 宣布輪到黑方
- 提醒思考時間過長
- 宣布超時
- 宣布勝負

玩家 action：

```text
MOVE_PIECE
```

後端驗證：

- 是否輪到該玩家
- 棋子是否屬於該玩家
- 移動是否符合棋子規則
- 是否造成非法局面
- 是否將軍、將死
- 是否超時
- 是否勝負已分

同步事件：

- BOARD_UPDATED
- TURN_CHANGED
- MOVE_APPLIED
- GAME_ENDED
- HOST_MESSAGE

此例證明核心架構不是狼人殺專屬，而是：

> Host-driven multiplayer action framework

---

# 14. 技術選型建議

## 14.1 Backend

建議：

- Java 17
- Spring Boot
- Spring Web
- Spring WebSocket / STOMP
- In-memory store
- Scheduled task

初期不使用：

- Redis
- DB
- 登入
- 多伺服器
- 產品化權限系統

---

## 14.2 Frontend

建議：

- React
- TypeScript
- WebSocket/STOMP client

前端頁面：

1. Lobby Page
2. Room Waiting Page
3. Game Room Page
4. Game Result View

---

## 14.3 儲存

MVP 使用 memory：

```java
ConcurrentHashMap<String, GameRoom> rooms;
```

但可抽象：

```java
interface GameStore {
    GameRoom getRoom(String roomId);
    void saveRoom(GameRoom room);
    List<GameRoom> getRooms();
    List<GameRoom> getActiveRooms();
}
```

初期：

```text
InMemoryGameStore
```

未來：

```text
RedisGameStore
DatabaseGameStore
```

---

# 15. MVP 功能範圍

## 15.1 MVP 要做

1. 遊戲大廳
2. 建立房間
3. 房間列表
4. 加入房間
5. 房間等待頁
6. 系統主持人
7. 固定 stage 流程
8. WebSocket 房間同步
9. 主持人公告
10. 玩家聊天
11. 發言冷卻
12. 投票階段
13. 投票結算
14. 淘汰玩家
15. 淘汰後不能發言
16. 剩餘 3 人結束
17. 遊戲結果公布
18. availableActions 控制 UI
19. PlayerControllerType 預留 AI
20. AgentController interface 預留

---

## 15.2 MVP 不做

1. 使用者註冊
2. 登入
3. 資料庫
4. Redis
5. 多伺服器部署
6. reconnect 完整恢復
7. 房主權限
8. 排行榜
9. 檢舉系統
10. 文字審核
11. 完整狼人殺職業
12. 夜晚階段
13. 真 LLM AI
14. 手機版細節優化
15. 長期遊戲紀錄

---

# 16. 逐步完成方案

## Phase 0：建立 SPEC.md 與任務邊界

目標：

- 先建立本文件
- 明確定義 MVP 範圍
- 明確定義不做什麼
- 避免 Codex 一次做太大

驗收：

- `SPEC.md` 存在
- Codex 後續所有實作需遵守 SPEC
- 每次只做一個 milestone

---

## Phase 1：後端專案骨架

目標：

- 建立 Spring Boot 專案
- Java 17
- 建立 package 結構
- 建立 domain model
- 建立 InMemoryGameStore

建議 package：

```text
com.example.game
  ├─ domain
  ├─ store
  ├─ service
  ├─ websocket
  ├─ api
  ├─ dto
  ├─ scheduler
  └─ agent
```

驗收：

- 專案可啟動
- 有 GameRoom、Player、ChatMessage、PlayerAction、AvailableAction 等 model
- 有 InMemoryGameStore

---

## Phase 2：Lobby 與 Room API

目標：

- 建立房間
- 查詢等待房間
- 加入房間
- 查詢房間狀態

API：

```http
POST /api/rooms
GET /api/rooms?status=WAITING
POST /api/rooms/{roomId}/join
GET /api/rooms/{roomId}
```

驗收：

- 使用 Postman / curl 可以建立房間
- 房間有 UUID
- 房間出現在等待列表
- 玩家可加入
- 玩家取得 playerId、playerNo、color

---

## Phase 3：WebSocket 房間事件同步

目標：

- 建立 STOMP WebSocket
- 每個房間有 topic
- 前端或測試 client 可訂閱房間事件

Topic：

```text
/topic/rooms/{roomId}
```

驗收：

- client A、B 訂閱同一 room topic
- 後端廣播 event 時，A、B 都收到

---

## Phase 4：聊天室訊息

目標：

- 玩家送出 SEND_MESSAGE
- 後端驗證
- 建立 ChatMessage
- 廣播 MESSAGE_CREATED

驗收：

- 多個瀏覽器加入同房間
- Player A 發言
- Player B 即時看到
- 自己也透過廣播看到訊息

---

## Phase 5：主持人 HOST 訊息

目標：

- HostService 可產生 HOST message
- HOST message 與 PLAYER message 共用 ChatMessage
- 可廣播到房間聊天室

驗收：

- 後端可發送：
  - 主持人：遊戲開始
  - 主持人：投票開始
- 所有房間玩家看到主持人訊息

---

## Phase 6：Stage 與 startGame

目標：

- 實作固定 stage 流程
- WAITING -> DISCUSSION
- startGame 時 Host 公告遊戲開始
- 發送 STAGE_CHANGED
- 發送 AVAILABLE_ACTIONS_UPDATED

API：

```http
POST /api/rooms/{roomId}/start
```

驗收：

- 房間從 WAITING 變 DISCUSSION
- Host 發送遊戲開始訊息
- 存活玩家收到 SEND_MESSAGE action
- 前端顯示輸入框

---

## Phase 7：發言冷卻

目標：

- 玩家每次成功發言後更新 lastMessageAt
- 15 秒內再次發言被拒絕
- 後端回傳 MESSAGE_REJECTED 或 ACTION_REJECTED
- 前端同步冷卻時間

驗收：

- 前端按鈕可 disable
- 直接用 DevTools/WebSocket 重送訊息仍被後端擋
- 後端回傳剩餘秒數

---

## Phase 8：投票階段

目標：

- DISCUSSION 時間到進入 VOTING
- Host 公告投票開始
- 發送 STAGE_CHANGED
- 對玩家發送 SUBMIT_VOTE availableAction
- 玩家提交投票

驗收：

- 投票開始時前端出現投票選單
- 只能投 alive player
- 不能投自己
- 每輪只能投一次
- 投票成功有回應

---

## Phase 9：投票完成條件與淘汰

目標：

- 所有玩家投票完成，或時間到
- 進入 ELIMINATION
- 統計票數
- 淘汰得票最高者
- 平票先隨機淘汰
- Host 公告淘汰結果
- 廣播 PLAYER_ELIMINATED
- 更新 availableActions

驗收：

- 被淘汰玩家 UI 變暗
- 被淘汰玩家不能再發言
- 後端拒絕被淘汰玩家 SEND_MESSAGE
- 若未結束，回到 DISCUSSION

---

## Phase 10：遊戲結束

目標：

- alivePlayers <= 3 時進入 ENDED
- Host 公告遊戲結束
- 公布身份與結果

MVP 結果規則：

- 最後 3 人中 0 位 AI：真人完全識破
- 最後 3 人中 1 位 AI：AI 混入成功
- 最後 3 人中 2 位 AI：AI 完全混入成功

若 MVP 尚未實作 AI，可先用 placeholder identity 或只公布玩家狀態。

驗收：

- 剩 3 人時自動結束
- 所有 action 停止
- 前端顯示結果頁

---

## Phase 11：前端 Lobby

目標：

- 建立 React + TypeScript 前端
- 首頁顯示大廳
- 房間 card view
- 建立房間
- 加入房間

驗收：

- 使用者可從 UI 建立房間
- 房間出現在大廳
- 使用者可加入房間

---

## Phase 12：前端 Game Room

目標：

- 房間等待頁
- 玩家列表
- 聊天室
- 主持人訊息
- 可用 actions UI
- 投票 UI
- 玩家淘汰狀態
- 遊戲結果頁

驗收：

- 至少兩個瀏覽器可以完整玩一場 MVP

---

## Phase 13：AgentController 預留

目標：

- 建立 AgentController interface
- 建立 NoopAgentController
- 建立 MockAgentController
- AI player 可被加入房間
- Mock AI 可根據 availableActions 隨機發言或投票

驗收：

- AI 不直接修改狀態
- AI action 走 PlayerActionService
- AI 受冷卻、alive、stage 限制
- AI 被淘汰後不能行動

---

## Phase 14：LLM AI 後續擴充

目標：

- 建立 LlmAgentController
- 根據可見訊息、可用 action、私有狀態產生決策
- 不洩漏不可見資訊

驗收：

- AI 可根據聊天室內容發言
- AI 可根據投票階段投票
- AI 不知道其他 AI 身份，除非遊戲規則允許
- LLM 失敗時遊戲不中斷

---

## Phase 15：未來泛用化

當 MVP 穩定後，再考慮抽象：

- GameRuleEngine
- StageHandler
- VisibilityRule
- Role system
- Faction system
- RedisGameStore
- Database persistence
- reconnect
- multi-room scaling
- complete Werewolf rules
- Chinese chess GameRuleEngine

---

# 17. Codex 開發注意事項

## 17.1 不要一次做完整系統

每次只要求 Codex 完成一個 Phase。

錯誤 prompt：

```text
幫我做一個 AI 狼人殺網站
```

正確 prompt：

```text
請只實作 Phase 3：WebSocket 房間事件同步。
不要實作投票。
不要實作 AI。
不要新增資料庫。
請遵守 SPEC.md 中的事件格式。
```

---

## 17.2 Code Review 重點

每次 review 要看：

1. 是否把規則放到前端
2. 是否讓前端取得不該知道的資訊
3. 是否把 HOST 當成 Player
4. AI 是否直接修改狀態
5. Scheduler 是否可能重複觸發
6. Stage 是否有檢查 currentStage
7. Action 是否統一走 PlayerActionService
8. WebSocket event 是否混亂
9. MESSAGE_CREATED 與 STAGE_CHANGED 是否分離
10. availableActions 是否由後端計算
11. private event 是否錯誤廣播到全房間
12. 玩家被淘汰後後端是否仍會拒絕 action
13. cooldown 是否只靠前端
14. identity 是否在遊戲結束前洩漏

---

# 18. 給 Codex 的總任務說明

以下可作為 Codex 初始總 prompt：

```text
我要開發一個 Host-driven Multiplayer Action Framework 的 MVP。

第一個遊戲模式是「AI 混入聊天室 / 精簡版 AI 狼人殺」，但目前先不需要真正接 LLM AI，只要保留 AI Player 的控制來源彈性。

技術：
- Backend: Java 17 + Spring Boot
- WebSocket: Spring WebSocket + STOMP
- Frontend: React + TypeScript
- Storage: 初期使用 InMemoryGameStore，不使用 DB、不使用 Redis
- 不做登入、不做產品化

核心架構：
- 系統有 Lobby 與 GameRoom
- Lobby 顯示等待中的房間，支援建立與加入房間
- 每個 GameRoom 有一個系統主持人 Host
- Host 不是 Player，不可被投票，不參與勝負
- Host 根據固定 stage 流程推進遊戲
- 每次進入 stage，Host 需要：
  1. 更新 room.currentStage
  2. 發送 HOST ChatMessage 到聊天室
  3. 發送 STAGE_CHANGED event
  4. 為每位玩家計算 availableActions
  5. 發送 AVAILABLE_ACTIONS_UPDATED
- 前端只能依照 availableActions 顯示可操作 UI
- 前端不能自行判斷規則
- 所有玩家操作都必須送後端，由 PlayerActionService 驗證
- 人類玩家與 AI 玩家都走同一套 PlayerActionService
- AI 不得直接修改任何 GameRoom 狀態

MVP stage：
WAITING -> DISCUSSION -> VOTING -> ELIMINATION -> DISCUSSION loop -> ENDED

MVP actions：
- SEND_MESSAGE
- SUBMIT_VOTE

MVP 規則：
- DISCUSSION 階段，存活玩家可發言
- 發言後有 15 秒 cooldown
- VOTING 階段，存活玩家可投票
- 每位玩家每輪只能投一次
- 不能投自己
- 只能投存活玩家
- 所有人投票完成或時間到後結算
- 得票最高者淘汰
- 平票先隨機淘汰
- 淘汰者不能再發言或投票
- 剩餘玩家 <= 3 時遊戲結束

事件設計：
- MESSAGE_CREATED：聊天室顯示用，可為 PLAYER 或 HOST message
- STAGE_CHANGED：前端控制 stage 用
- AVAILABLE_ACTIONS_UPDATED：前端顯示可操作功能用
- ACTION_REJECTED / MESSAGE_REJECTED：操作被拒絕
- PLAYER_ELIMINATED：玩家淘汰
- GAME_ENDED：遊戲結束

請務必遵守：
- 遊戲結束前，傳給前端的 player DTO 不可包含 controllerType 或 AI/HUMAN 身份
- HOST 不可放入 players list
- ChatMessage 要預留 audience 欄位，MVP 可先固定 PUBLIC
- 不要使用 DB 或 Redis
- 不要做登入
- 不要一次實作所有功能，請依照 SPEC.md 的 Phase 逐步開發
```

---

# 19. 最終結論

目前收斂出的設計是可行的。

第一版不應做成完整狼人殺，也不應直接做泛用遊戲 DSL。

合理策略是：

```text
先做 AI 混入聊天室 MVP
但用 Host-driven stage + availableActions + backend validation 的正確骨架
```

這樣可以同時達到：

1. 有可玩的成果
2. 可練習 WebSocket
3. 可練習狀態機
4. 可練習後端規則驗證
5. 可練習前後端同步
6. 可練習 AI-assisted coding
7. 可保留未來接 AI Agent 的彈性
8. 可保留未來擴充狼人殺、象棋或其他互動遊戲的可能性

最重要的架構原則：

> Browser 和 AI 都只是 PlayerAction 的來源  
> Host / GameFlowEngine 決定何時開放哪些 action  
> 後端是唯一規則來源  
> 前端只顯示後端授權的操作  
> 所有狀態改變由後端同步給參與者
