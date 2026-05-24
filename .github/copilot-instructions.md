# Copilot Repository Instructions

本文件給 GitHub Copilot / VS Code coding assistant 使用。

本專案主要目標是建立一個 Host-driven Multiplayer Action Framework 的 MVP。第一個遊戲模式是「AI 混入聊天室 / 精簡版 AI 狼人殺」，但架構要保留未來擴充為狼人殺、象棋或其他回合制互動遊戲的彈性。

---

# 1. 回答與文件風格

採用「新手，但不是笨蛋」風格。

意思是：

- 使用者可能是第一次接觸某個技術細節
- 但使用者有能力理解真正工程內容
- 不要幼稚化
- 不要過度省略背景
- 不要堆疊沒解釋的術語

說明類回覆建議順序：

1. 一句話結論
2. 用白話說明它是什麼
3. 說明關鍵機制或為什麼重要
4. 給實務例子、常見錯誤或影響
5. 有需要時給下一步建議

---

# 2. 寫程式時的基本要求

請遵守：

- 不要把遊戲規則放到前端
- 前端只根據後端提供的 availableActions 顯示 UI
- 後端是唯一規則來源
- 所有玩家 action 都必須送後端驗證
- Human Player 與 AI Player 都走同一套 PlayerActionService
- AI 不得直接修改 GameRoom、ChatMessage、Vote 或任何狀態
- HOST 是系統主持人，不是 Player
- 遊戲結束前不可把 AI / HUMAN 身份傳給前端
- 聊天訊息事件與 UI 控制事件必須分離
- MESSAGE_CREATED 只負責聊天室顯示
- STAGE_CHANGED / AVAILABLE_ACTIONS_UPDATED 負責 UI 控制

---

# 3. 技術限制

MVP 階段使用：

- Java 17
- Spring Boot
- Spring Web
- Spring WebSocket / STOMP
- React
- TypeScript
- In-memory store

MVP 階段不要主動加入：

- 資料庫
- Redis
- 登入
- Spring Security
- JPA
- Docker
- Kubernetes
- LLM API
- 完整狼人殺職業系統
- generic DSL

---

# 4. 文件與註解

- 專案文件使用繁體中文
- code comments 優先使用繁體中文
- 儲存為 UTF-8 without BOM
- 技術詞彙必要時可使用英文
- 寫文件時以可讀性優先

---

# 5. 除錯與架構說明

當說明 bug 或程式碼時，請說清楚：

- 目前程式碼在做什麼
- 為什麼會成功或失敗
- 實際影響是什麼
- 常見修法是什麼
- 隱含假設是什麼

當討論架構、後端、資料庫時，請明確區分：

- 業務語意
- 技術機制
- 驗證責任
- 一致性期待
- transaction / state boundary
- cache / ordering / failure handling

---

# 6. 不要過度實作

若使用者要求單一 phase，只完成該 phase。

不要順手加入：

- 其他 stage
- 其他遊戲模式
- 完整 AI
- 登入權限
- 持久化
- UI 美化大改
- 通用框架抽象

本專案第一階段目標是：

> 可跑、可驗證、可 review、不過度工程化。
