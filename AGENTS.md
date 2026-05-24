# AGENTS.md

本文件是本專案給 Codex / coding agent 使用的 repository-level 工作規則。

目標不是限制 agent 不能做事，而是讓每一輪修改都可追蹤、可驗證、可 code review。

---

# 1. 專案定位

本專案是一個 Host-driven Multiplayer Action Framework 的 MVP。

第一個遊戲模式是「AI 混入聊天室 / 精簡版 AI 狼人殺」，但架構要保留未來擴充為狼人殺、象棋或其他多人互動遊戲的彈性。

核心原則：

- Host / GameFlowEngine 控制 stage
- 後端是唯一規則來源
- 前端只根據 availableActions 顯示 UI
- 所有玩家操作都送後端驗證
- Human Player 與 AI Player 都走同一套 PlayerActionService
- AI 不得直接修改任何遊戲狀態
- 遊戲結束前，前端不可知道誰是 AI / HUMAN

---

# 2. Agent 溝通風格

當使用者要求解釋、除錯、架構說明、文件、規格或 reasoning 時，採用以下風格：

> 我是新手，但不是笨蛋。請把我當成對這個領域還不熟，但有能力理解真正工程細節的人。

回答要求：

- 先給一句結論
- 用清楚直白的語言說明
- 必要技術名詞可以保留，但第一次出現時要簡短解釋
- 不要過度壓縮，也不要變成長篇教科書
- 不要用幼稚或居高臨下的語氣
- 架構、後端、資料庫議題要分清楚「這是什麼」與「為什麼重要」
- 要提醒隱含前提，例如驗證責任、交易邊界、狀態一致性、快取、排序、失敗處理
- 寫文件時優先可讀性，使用清楚標題與適中段落

直接實作任務時：

- 保持輸出實用、精簡
- 不要主動加入長篇教學
- 不要做超出本輪任務範圍的功能

---

# 3. 每輪實作範圍

每次只實作使用者指定的 phase / task。

若任務說「只做 Phase 3」，不得順手實作 Phase 4 或 Phase 5。

除非使用者明確要求，否則不得主動加入：

- 登入
- 資料庫
- Redis
- 權限系統
- 排行榜
- 檢舉系統
- 完整狼人殺職業
- LLM AI
- 多伺服器部署
- reconnect 完整恢復
- 過度抽象的 DSL 或 plugin framework

本專案第一階段優先：

- 可跑
- 可驗證
- 可 review
- 架構邊界清楚
- 不過度工程化

---

# 4. 必要開發衛生規則

每一輪只要有改動以下任何內容，都必須建立 round summary：

- code
- config
- SQL
- docs
- 專案結構
- 測試設定
- package / dependency

round summary 放在 workspace root。

檔名格式：

```text
codex-YYYYMMDD-000N.md
```

規則：

- 使用本機日期作為 `YYYYMMDD`
- 同一天依序使用 4 位數流水號，例如 `0001`, `0002`
- 不得覆蓋既有 round summary，除非使用者明確要求修訂該檔案
- 如果無法取得 branch，寫 `unknown`

round summary 內容：

```markdown
# Round Summary

- Date:
- Branch:
- Goal:
- Key changes:
- Validation:
- Open items / next steps:
```

要求：

- 內容簡短
- 只寫實際有做的事
- Validation 只寫實際執行過的驗證
- 不要宣稱執行過未執行的測試

---

# 5. 編碼與文件語言

- 所有專案文件必須使用 UTF-8 without BOM
- 專案文件優先使用繁體中文
- code comments 優先使用繁體中文
- 技術名詞使用英文較清楚時，可保留英文
- 不使用簡體中文詞彙
- 台灣慣用語優先，例如：
  - 軟體
  - 伺服器
  - 防火牆
  - 資料庫
  - 雲端
  - 電子郵件
  - 人工智慧

---

# 6. Code Review 必查項目

每一輪完成後，agent 必須自我檢查並在 round summary 的 open items 中列出疑點。

尤其注意：

1. 是否把遊戲規則寫在前端
2. 前端是否在遊戲結束前拿到 AI / HUMAN 身份
3. HOST 是否被錯誤放進 players list
4. AI 是否直接修改 GameRoom / Vote / ChatMessage
5. 所有 action 是否都有走 PlayerActionService
6. 被淘汰玩家是否仍可能發言或投票
7. cooldown 是否只靠前端
8. Scheduler 是否可能重複觸發 stage transition
9. MESSAGE_CREATED 與 STAGE_CHANGED 是否混在一起
10. availableActions 是否由後端計算
11. private event 是否錯誤廣播到全房間
12. 是否引入未經要求的 DB / Redis / 登入 / 權限系統
13. DTO 是否洩漏 internal state
14. 測試是否真的執行過

---

# 7. Git 與變更行為

- 修改前先理解目前檔案
- 不要大範圍重排無關程式碼
- 不要同時做多個 phase
- 不要因小任務重構整個專案
- 若發現規格矛盾，先寫明假設再實作
- 不要刪除使用者已建立的重要文件
- 不要覆蓋既有 round summary

---

# 8. 測試與驗證

每輪應盡量執行與該輪相關的最低限度驗證。

例如：

- backend domain / service 改動：執行 unit test 或至少 compile
- WebSocket event 改動：執行相關測試或提供手動驗證步驟
- frontend 改動：執行 build / lint，或說明未執行原因
- 文件改動：確認 markdown 檔案已建立且 encoding 為 UTF-8 without BOM

若無法執行驗證，必須明確寫：

```text
Validation not run: reason
```

不得假裝已驗證。

---

# 9. 第一階段禁止事項

MVP 階段不得主動導入：

- Spring Security
- OAuth / JWT
- JPA / Hibernate
- PostgreSQL / MySQL
- Redis
- Kafka
- Docker Compose
- Kubernetes
- 多租戶架構
- 完整權限模型
- LLM API
- 完整狼人殺職業系統
- generic workflow DSL

除非使用者明確要求。

---

# 10. 第一階段建議任務順序

1. 建立 Spring Boot 專案骨架
2. 建立 domain model
3. 建立 InMemoryGameStore
4. 建立 Lobby / Room API
5. 建立 WebSocket / STOMP
6. 建立 ChatMessage 與 HOST message
7. 建立 Stage 與 startGame
8. 建立 availableActions
9. 建立 PlayerActionService
10. 實作 SEND_MESSAGE
11. 實作 cooldown
12. 實作 VOTING
13. 實作 ELIMINATION
14. 實作 GAME_ENDED
15. 建立 React 前端
16. 預留 AgentController
17. MockAgentController
18. 後續再考慮 LLM AI
