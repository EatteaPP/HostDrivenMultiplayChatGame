# Demo Guide

## What This Is

這份文件說明如何在本機 demo Host-driven Multiplayer Action Framework MVP。

目前 demo 重點是 Host-driven 主流程：建立房間、加入玩家、開始遊戲、發言、進入投票、淘汰玩家、遊戲結束。
若房間目標設為 `找出AI`，可再加入 Mock AI 進行 AI 模式驗證。

## Prerequisites

請先確認本機已具備：

- Java 17
- Maven
- nvm-windows
- Node.js 22
- npm

本專案目前在 Windows PowerShell 下建議使用 `npm.cmd`，因為 PowerShell 可能會阻擋 `npm.ps1`。

## Start Backend

在專案根目錄執行：

```powershell
$env:JAVA_HOME='C:\Tools\JAVA\java-17'
$env:MAVEN_HOME='C:\Users\EatteaPP\tools\apache-maven-3.9.14'
$env:Path='C:\Tools\JAVA\java-17\bin;C:\Users\EatteaPP\tools\apache-maven-3.9.14\bin;C:\Program Files\Git\cmd;' + $env:Path
mvn spring-boot:run
```

成功後應看到後端啟動在：

```text
http://127.0.0.1:8080
```

## Start Frontend

另開一個 PowerShell，在 `frontend` 目錄執行：

```powershell
$env:NVM_HOME='C:\Users\EatteaPP\AppData\Local\nvm'
$env:NVM_SYMLINK='C:\nvm4w\nodejs'
$env:Path="$env:NVM_HOME;$env:NVM_SYMLINK;$env:Path"
& 'C:\nvm4w\nodejs\npm.cmd' install
& 'C:\nvm4w\nodejs\npm.cmd' run dev -- --host 127.0.0.1 --port 5173
```

成功後開啟：

```text
http://127.0.0.1:5173
```

## Demo Flow

### 1. Create Room

1. 開啟前端頁面。
2. 設定房間參數：
   - Objective（`找出叛徒` / `找出AI`）
   - 討論秒數
   - 投票秒數
   - 發言 CD 秒數
   - 最大輪次（預設 10）
2. 點擊 `Create Room`。
3. 頁面右側會出現目前房間資訊。

預期結果：

- Room status 是 `WAITING`
- Stage 是 `WAITING`
- 玩家列表目前可能是空的
- 房間資訊會顯示目標、秒數設定與輪次上限

### 2. Join As Human Player

1. 在 Lobby 區塊點擊該房間的 `Join`。

預期結果：

- 頁面顯示 `Joined as Player 1`
- 玩家列表出現 `Player 1`
- 前端不會顯示 AI / HUMAN 身份

### 3. (Optional) Add Mock AI

1. 將 Objective 設為 `找出AI` 建立房間。
2. 在房間右上角點擊 `Add AI`。

預期結果：

- 玩家列表新增 `Player 2`
- 前端仍不會顯示 `Player 2` 是 AI

### 4. Start Game

1. 點擊 `Start`。

預期結果：

- Room status 變成 `IN_PROGRESS`
- Stage 變成 `DISCUSSION`
- 聊天室出現 Host message：

```text
Game started. Discussion phase begins.
```

- 若有加入 Mock AI，AI 會自動發言：

```text
I am thinking this through.
```

這裡代表 AI 不是直接修改聊天室，而是透過 `PlayerActionService` 送出 `SEND_MESSAGE`。

### 5. Send Human Message

1. 在輸入框輸入訊息。
2. 點擊 `Send`。

預期結果：

- 聊天室顯示 `Player 1` 的訊息
- 如果太快連續送訊息，後端 cooldown 會拒絕

### 6. Wait For Voting

目前 discussion 預設時間較長。若要快速測試投票，可以暫時在程式中調整 `GameFlowConfig.discussionSeconds`，或透過測試案例驗證 voting flow。

進入投票階段後，預期結果：

- Stage 變成 `VOTING`
- 前端會根據後端 `availableActions` 顯示投票按鈕
- 玩家不能投自己
- 被淘汰玩家不能投票
- 不能投已淘汰玩家

### 7. Game End

當存活玩家數量小於或等於 3 時，後端會進入 `ENDED`。
若輪次達到房間設定的最大輪次（例如 10）仍未結束，也會強制進入 `ENDED`。

預期結果：

- Stage 變成 `ENDED`
- 前端不再取得可用 action
- `GAME_ENDED` event 才會揭露身份資訊（含陣營）
- 勝負判定（目前 MVP 版）：只要任一叛徒存活，叛徒方勝；否則平民方勝

## Useful API Checks

### List Waiting Rooms

```powershell
curl.exe http://127.0.0.1:8080/api/rooms?status=WAITING
```

### Create Room

```powershell
curl.exe -X POST http://127.0.0.1:8080/api/rooms `
  -H "Content-Type: application/json" `
  -d "{\"gameType\":\"AI_CHAT_WEREWOLF\",\"objective\":\"FIND_TRAITOR\",\"discussionSeconds\":180,\"votingSeconds\":60,\"messageCooldownSeconds\":15,\"maxRounds\":10}"
```

### Join Room

```powershell
curl.exe -X POST http://127.0.0.1:8080/api/rooms/{roomId}/join `
  -H "Content-Type: application/json" `
  -d "{}"
```

### Add AI Player

```powershell
curl.exe -X POST http://127.0.0.1:8080/api/rooms/{roomId}/ai-players
```

注意：只有房間目標為 `FIND_AI` 時，後端才允許加入 AI player。

### Start Game

```powershell
curl.exe -X POST http://127.0.0.1:8080/api/rooms/{roomId}/start
```

## Important Review Points

- Host 不在 players list。
- 前端只顯示 public player data。
- 遊戲結束前不揭露 AI / HUMAN。
- Human 與 AI action 都走 `PlayerActionService`。
- AI 不直接修改 `GameRoom`、`Vote`、`ChatMessage`。
- cooldown、投票、淘汰都由後端驗證。
- Scheduler 只負責檢查 timeout，實際轉場由 `GameFlowEngine` 處理。

## Stop Servers

在後端與前端的 PowerShell 視窗按：

```text
Ctrl + C
```

如果 port 被占用，可以查詢：

```powershell
netstat -ano | findstr ":8080"
netstat -ano | findstr ":5173"
```

再依 PID 停止行程：

```powershell
Stop-Process -Id <PID> -Force
```
