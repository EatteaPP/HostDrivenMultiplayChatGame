# CODEX_WORKFLOW.md

本文件說明如何使用 Codex app / VS Code Codex plugin 開發本專案。

---

# 1. 建議工具分工

## Codex app

作為主要施工工具。

適合：

- 完成指定 phase
- 建立檔案
- 修改多個檔案
- 執行測試
- 產生 round summary

## VS Code + Codex plugin

作為局部輔助工具。

適合：

- 解釋單一檔案
- 小範圍修正
- 檢查 method
- 補測試
- code review

## 開發者本人

負責：

- 看 git diff
- 決定架構是否合理
- 決定是否接受 agent 改動
- 控制任務範圍
- 避免 AI 過度工程化

---

# 2. 每輪建議流程

1. 開新 branch 或確認目前 branch
2. 明確指定 phase
3. 讓 Codex 只做該 phase
4. Codex 執行可行的 validation
5. Codex 建立 round summary
6. 開發者 review diff
7. 修正問題
8. commit
9. 進下一 phase

---

# 3. 建議 Prompt 範例

```text
請閱讀 SPEC.md、AGENTS.md、PROJECT_TECHNICAL_GUIDE.md。

請只實作 Phase 2：Lobby 與 Room API。

需求：
- 建立 POST /api/rooms
- 建立 GET /api/rooms?status=WAITING
- 建立 POST /api/rooms/{roomId}/join
- 建立 GET /api/rooms/{roomId}
- 使用 InMemoryGameStore
- 不使用 DB
- 不使用 Redis
- 不實作 WebSocket
- 不實作 AI
- 不實作投票

完成後：
- 執行可行的 compile/test
- 建立 codex-YYYYMMDD-000N.md round summary
- 列出改動檔案與驗證結果
```

---

# 4. 禁止 Prompt

避免：

```text
幫我做完整 AI 狼人殺網站
```

避免：

```text
幫我把整個系統一次做完
```

避免：

```text
你覺得還缺什麼都幫我補
```

這些 prompt 會導致 agent 過度發揮，產生難以 review 的 code。

---

# 5. Review Checklist

每次 review 問自己：

- 這次改動是否超出 phase？
- 是否引入未要求的套件？
- 前端是否偷做規則？
- 後端是否有統一驗證？
- DTO 是否洩漏 internal state？
- AI 是否仍只是 action source？
- HOST 是否仍不是 Player？
- round summary 是否存在？
- validation 是否真的跑過？
