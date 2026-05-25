import React from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { Client } from '@stomp/stompjs';
import './styles.css';

type GameType = 'AI_CHAT_WEREWOLF';
type RoomObjective = 'FIND_TRAITOR' | 'FIND_AI';
type RoomStatus = 'WAITING' | 'IN_PROGRESS' | 'ENDED';
type GameStage = 'WAITING' | 'DISCUSSION' | 'VOTING' | 'ELIMINATION' | 'ENDED';
type PlayerStatus = 'ALIVE' | 'ELIMINATED' | 'DEAD';
type ActionType = 'SEND_MESSAGE' | 'SUBMIT_VOTE';
type MessageType = 'PLAYER' | 'HOST';

type PlayerPublicView = {
  playerId: string;
  playerNo: number;
  color: string;
  status: PlayerStatus;
};

type ChatMessageView = {
  messageId: string;
  roomId: string;
  messageType: MessageType;
  playerId: string | null;
  playerNo: number | null;
  speakerName: string | null;
  content: string;
  createdAt: string;
};

type ActionTarget = {
  playerId: string;
  playerNo: number;
};

type AvailableAction = {
  actionType: ActionType;
  targets: ActionTarget[];
  metadata: Record<string, unknown>;
};

type AvailableActionsUpdatedView = {
  playerId: string;
  stage: GameStage;
  availableActions: AvailableAction[];
};

type RoomView = {
  roomId: string;
  gameType: GameType;
  objective: RoomObjective;
  objectiveHint: string | null;
  roomStatus: RoomStatus;
  currentStage: GameStage;
  players: PlayerPublicView[];
  messages: ChatMessageView[];
  createdAt: string;
  startedAt: string | null;
  stageStartedAt: string | null;
  stageEndsAt: string | null;
  endedAt: string | null;
  round: number;
  discussionSeconds: number;
  votingSeconds: number;
  messageCooldownSeconds: number;
  maxRounds: number;
};

type JoinRoomResponse = {
  room: RoomView;
  player: PlayerPublicView;
};

type WsEvent<T> = {
  type: string;
  payload: T;
  createdAt: string;
};

type ApiError = {
  title?: string;
  detail?: string;
};

type ActionResult = {
  accepted: boolean;
  actionType: ActionType;
  result: unknown;
};

type CreateRoomPayload = {
  gameType: GameType;
  objective: RoomObjective;
  objectiveHint: string;
  discussionSeconds: number;
  votingSeconds: number;
  messageCooldownSeconds: number;
  maxRounds: number;
};

const api = {
  listWaitingRooms: () => request<RoomView[]>('/api/rooms?status=WAITING'),
  createRoom: (payload: CreateRoomPayload) =>
    request<RoomView>('/api/rooms', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    }),
  joinRoom: (roomId: string) =>
    request<JoinRoomResponse>(`/api/rooms/${roomId}/join`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({}),
    }),
  getRoom: (roomId: string) => request<RoomView>(`/api/rooms/${roomId}`),
  startRoom: (roomId: string) =>
    request<RoomView>(`/api/rooms/${roomId}/start`, {
      method: 'POST',
    }),
  addAiPlayer: (roomId: string) =>
    request<RoomView>(`/api/rooms/${roomId}/ai-players`, {
      method: 'POST',
    }),
  getActions: (roomId: string, playerId: string) =>
    request<AvailableActionsUpdatedView>(`/api/rooms/${roomId}/players/${playerId}/actions`),
  submitAction: (roomId: string, playerId: string, actionType: ActionType, payload: Record<string, unknown>) =>
    request<ActionResult>(`/api/rooms/${roomId}/players/${playerId}/actions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ actionType, payload }),
    }),
};

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, init);
  if (!response.ok) {
    let message = `Request failed with ${response.status}`;
    try {
      const body = (await response.json()) as ApiError;
      message = body.detail ?? body.title ?? message;
    } catch {
      // Keep the HTTP status message when the response is not JSON.
    }
    throw new Error(message);
  }
  return response.json() as Promise<T>;
}

function App() {
  const [rooms, setRooms] = React.useState<RoomView[]>([]);
  const [activeRoom, setActiveRoom] = React.useState<RoomView | null>(null);
  const [currentPlayer, setCurrentPlayer] = React.useState<PlayerPublicView | null>(null);
  const [availableActions, setAvailableActions] = React.useState<AvailableAction[]>([]);
  const [messageText, setMessageText] = React.useState('');
  const [loading, setLoading] = React.useState(false);
  const [status, setStatus] = React.useState('Ready');
  const [objective, setObjective] = React.useState<RoomObjective>('FIND_TRAITOR');
  const [discussionSeconds, setDiscussionSeconds] = React.useState(180);
  const [votingSeconds, setVotingSeconds] = React.useState(60);
  const [messageCooldownSeconds, setMessageCooldownSeconds] = React.useState(15);
  const [maxRounds, setMaxRounds] = React.useState(10);
  const [objectiveHint, setObjectiveHint] = React.useState('');
  const [nowMs, setNowMs] = React.useState(() => Date.now());

  const activeRoomId = activeRoom?.roomId ?? null;
  const currentPlayerId = currentPlayer?.playerId ?? null;

  const refreshRooms = React.useCallback(async () => {
    setLoading(true);
    try {
      setRooms(await api.listWaitingRooms());
      setStatus('Lobby refreshed');
    } catch (error) {
      setStatus(toMessage(error, 'Unable to refresh lobby'));
    } finally {
      setLoading(false);
    }
  }, []);

  const refreshActiveRoom = React.useCallback(async () => {
    if (!activeRoomId) {
      return;
    }
    try {
      const room = await api.getRoom(activeRoomId);
      setActiveRoom(room);
    } catch (error) {
      setStatus(toMessage(error, 'Unable to refresh room'));
    }
  }, [activeRoomId]);

  const refreshAvailableActions = React.useCallback(async () => {
    if (!activeRoomId || !currentPlayerId) {
      setAvailableActions([]);
      return;
    }
    try {
      const updated = await api.getActions(activeRoomId, currentPlayerId);
      setAvailableActions(updated.availableActions);
    } catch (error) {
      setAvailableActions([]);
      setStatus(toMessage(error, 'Unable to refresh actions'));
    }
  }, [activeRoomId, currentPlayerId]);

  React.useEffect(() => {
    void refreshRooms();
  }, [refreshRooms]);

  React.useEffect(() => {
    void refreshAvailableActions();
  }, [refreshAvailableActions, activeRoom?.currentStage, activeRoom?.round, currentPlayer?.status]);

  React.useEffect(() => {
    const timer = window.setInterval(() => {
      setNowMs(Date.now());
    }, 1000);
    return () => window.clearInterval(timer);
  }, []);

  React.useEffect(() => {
    if (!activeRoomId) {
      return undefined;
    }

    const client = new Client({
      brokerURL: `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/ws`,
      reconnectDelay: 1500,
      onConnect: () => {
        client.subscribe(`/topic/rooms/${activeRoomId}`, (frame) => {
          const event = JSON.parse(frame.body) as WsEvent<unknown>;
          applyRoomEvent(event, setActiveRoom, setStatus);
          void refreshAvailableActions();
        });
      },
      onStompError: () => {
        setStatus('WebSocket error');
      },
    });

    client.activate();
    return () => {
      void client.deactivate();
    };
  }, [activeRoomId, refreshAvailableActions]);

  async function createRoom() {
    setLoading(true);
    try {
      const room = await api.createRoom({
        gameType: 'AI_CHAT_WEREWOLF',
        objective,
        objectiveHint,
        discussionSeconds,
        votingSeconds,
        messageCooldownSeconds,
        maxRounds,
      });
      setActiveRoom(room);
      setCurrentPlayer(null);
      setAvailableActions([]);
      setRooms(await api.listWaitingRooms());
      setStatus('Room created');
    } catch (error) {
      setStatus(toMessage(error, 'Unable to create room'));
    } finally {
      setLoading(false);
    }
  }

  async function joinRoom(roomId: string) {
    setLoading(true);
    try {
      const response = await api.joinRoom(roomId);
      setActiveRoom(response.room);
      setCurrentPlayer(response.player);
      setRooms(await api.listWaitingRooms());
      setStatus(`Joined as Player ${response.player.playerNo}`);
    } catch (error) {
      setStatus(toMessage(error, 'Unable to join room'));
    } finally {
      setLoading(false);
    }
  }

  async function startGame() {
    if (!activeRoom) {
      return;
    }
    setLoading(true);
    try {
      setActiveRoom(await api.startRoom(activeRoom.roomId));
      setRooms(await api.listWaitingRooms());
      setStatus('Game started');
    } catch (error) {
      setStatus(toMessage(error, 'Unable to start game'));
    } finally {
      setLoading(false);
    }
  }

  async function addAiPlayer() {
    if (!activeRoom) {
      return;
    }
    setLoading(true);
    try {
      setActiveRoom(await api.addAiPlayer(activeRoom.roomId));
      setRooms(await api.listWaitingRooms());
      setStatus('AI player added');
    } catch (error) {
      setStatus(toMessage(error, 'Unable to add AI player'));
    } finally {
      setLoading(false);
    }
  }

  async function sendMessage() {
    if (!activeRoom || !currentPlayer || !messageText.trim()) {
      return;
    }
    setLoading(true);
    try {
      await api.submitAction(activeRoom.roomId, currentPlayer.playerId, 'SEND_MESSAGE', {
        content: messageText.trim(),
      });
      setMessageText('');
      await refreshActiveRoom();
      await refreshAvailableActions();
      setStatus('Message sent');
    } catch (error) {
      setStatus(toMessage(error, 'Unable to send message'));
    } finally {
      setLoading(false);
    }
  }

  async function submitVote(targetPlayerId: string) {
    if (!activeRoom || !currentPlayer) {
      return;
    }
    setLoading(true);
    try {
      await api.submitAction(activeRoom.roomId, currentPlayer.playerId, 'SUBMIT_VOTE', {
        targetPlayerId,
      });
      await refreshActiveRoom();
      await refreshAvailableActions();
      setStatus('Vote submitted');
    } catch (error) {
      setStatus(toMessage(error, 'Unable to vote'));
    } finally {
      setLoading(false);
    }
  }

  const sendMessageAction = availableActions.find((action) => action.actionType === 'SEND_MESSAGE');
  const voteAction = availableActions.find((action) => action.actionType === 'SUBMIT_VOTE');
  const stageRemainingSeconds = getRemainingSeconds(activeRoom?.stageEndsAt ?? null, nowMs);
  const sendCooldownRemainingSeconds = getSendCooldownRemainingSeconds(activeRoom, currentPlayer, nowMs);
  const sendDisabledByCooldown = sendCooldownRemainingSeconds > 0;
  const sendButtonLabel = sendDisabledByCooldown ? `Send (${sendCooldownRemainingSeconds})` : 'Send';

  return (
    <main className="app-shell">
      <section className="lobby-panel">
        <div className="toolbar">
          <div>
            <h1>AI Chat Room</h1>
            <p>Host-driven multiplayer action framework</p>
          </div>
          <div className="actions">
            <button type="button" onClick={refreshRooms} disabled={loading}>
              Refresh
            </button>
            <button type="button" onClick={createRoom} disabled={loading}>
              Create Room
            </button>
          </div>
        </div>
        <div className="room-config-grid">
          <label>
            Objective
            <select value={objective} onChange={(event) => setObjective(event.target.value as RoomObjective)} disabled={loading}>
              <option value="FIND_TRAITOR">找出叛徒</option>
              <option value="FIND_AI">找出 AI</option>
            </select>
          </label>
          <label>
            本輪辨識目標
            <input
              type="text"
              maxLength={120}
              value={objectiveHint}
              onChange={(event) => setObjectiveHint(event.target.value)}
              placeholder="例如：找出女性、找出 I 人"
              disabled={loading}
            />
          </label>
          <label>
            討論秒數
            <input
              type="number"
              min={10}
              value={discussionSeconds}
              onChange={(event) => setDiscussionSeconds(Number(event.target.value))}
              disabled={loading}
            />
          </label>
          <label>
            投票秒數
            <input
              type="number"
              min={10}
              value={votingSeconds}
              onChange={(event) => setVotingSeconds(Number(event.target.value))}
              disabled={loading}
            />
          </label>
          <label>
            發言 CD 秒數
            <input
              type="number"
              min={1}
              value={messageCooldownSeconds}
              onChange={(event) => setMessageCooldownSeconds(Number(event.target.value))}
              disabled={loading}
            />
          </label>
          <label>
            最大輪次
            <input
              type="number"
              min={1}
              value={maxRounds}
              onChange={(event) => setMaxRounds(Number(event.target.value))}
              disabled={loading}
            />
          </label>
        </div>

        <div className="status-line">{status}</div>

        <div className="content-grid game-grid">
          <section className="room-list" aria-label="Waiting rooms">
            <h2>Lobby</h2>
            {rooms.length === 0 ? (
              <div className="empty-state">No waiting rooms</div>
            ) : (
              rooms.map((room) => (
                <article className="room-card" key={room.roomId}>
                  <div>
                    <div className="room-title">Room {shortId(room.roomId)}</div>
                    <div className="room-meta">
                      {objectiveLabel(room.objective)} | {room.currentStage} | {room.players.length} players
                    </div>
                    {room.objectiveHint && <div className="room-meta">{room.objectiveHint}</div>}
                  </div>
                  <button type="button" onClick={() => joinRoom(room.roomId)} disabled={loading}>
                    Join
                  </button>
                </article>
              ))
            )}
          </section>

          <section className="game-room" aria-label="Game room">
            {activeRoom ? (
              <>
                <div className="detail-header">
                  <div>
                    <h2>Room {shortId(activeRoom.roomId)}</h2>
                    <p>
                      {objectiveLabel(activeRoom.objective)} | {activeRoom.roomStatus} | {activeRoom.currentStage} | Round {activeRoom.round}/{activeRoom.maxRounds}
                    </p>
                    {activeRoom.objectiveHint && <p>{activeRoom.objectiveHint}</p>}
                    <p>
                      目前階段：{stageLabel(activeRoom.currentStage)}
                      {stageRemainingSeconds !== null ? `（剩餘 ${stageRemainingSeconds} 秒）` : ''}
                    </p>
                    <p>
                      討論 {activeRoom.discussionSeconds}s / 投票 {activeRoom.votingSeconds}s / 發言 CD {activeRoom.messageCooldownSeconds}s
                    </p>
                  </div>
                  <div className="actions compact-actions">
                    {currentPlayer && (
                      <span
                        className="player-badge"
                        style={{
                          backgroundColor: colorValue(currentPlayer.color),
                          borderColor: colorValue(currentPlayer.color),
                          color: '#ffffff',
                        }}
                      >
                        {playerLabel(currentPlayer.playerNo, currentPlayer.color)}
                      </span>
                    )}
                    {activeRoom.currentStage === 'WAITING' && (
                      <>
                        <button type="button" onClick={addAiPlayer} disabled={loading || activeRoom.objective !== 'FIND_AI'}>
                          Add AI
                        </button>
                        <button type="button" onClick={startGame} disabled={loading || activeRoom.players.length === 0}>
                          Start
                        </button>
                      </>
                    )}
                  </div>
                </div>

                <div className="room-layout">
                  <aside className="players">
                    {activeRoom.players.map((player) => (
                      <div className="player-row" key={player.playerId}>
                        <span className="color-dot" style={{ backgroundColor: colorValue(player.color) }} />
                        <span>{playerLabel(player.playerNo, player.color)}</span>
                        <span>{player.status}</span>
                      </div>
                    ))}
                  </aside>

                  <section className="chat-panel" aria-label="Chat log">
                    <div className="messages">
                      {activeRoom.messages.length === 0 ? (
                        <div className="empty-state">No messages yet</div>
                      ) : (
                        activeRoom.messages.map((message) => (
                          <article
                            className={`message ${message.messageType.toLowerCase()}`}
                            key={message.messageId}
                            style={messageStyle(message, activeRoom)}
                          >
                            <div className="message-speaker">
                              {message.messageType === 'HOST'
                                ? 'Host'
                                : playerLabelById(activeRoom, message.playerId, message.playerNo)}
                            </div>
                            <p>{message.content}</p>
                          </article>
                        ))
                      )}
                    </div>

                    {currentPlayer ? (
                      <div className="composer">
                        <input
                          aria-label="Message"
                          value={messageText}
                          onChange={(event) => setMessageText(event.target.value)}
                          onKeyDown={(event) => {
                            if (event.key === 'Enter') {
                              void sendMessage();
                            }
                          }}
                          placeholder="Message"
                          disabled={loading || !sendMessageAction}
                        />
                        <button
                          type="button"
                          onClick={sendMessage}
                          disabled={loading || !sendMessageAction || !messageText.trim() || sendDisabledByCooldown}
                        >
                          {sendButtonLabel}
                        </button>
                      </div>
                    ) : (
                      <div className="empty-state compact-empty">Join the room to act</div>
                    )}

                    {voteAction && (
                      <div className="vote-panel">
                        {voteAction.targets.map((target) => (
                          <button
                            type="button"
                            key={target.playerId}
                            onClick={() => submitVote(target.playerId)}
                            disabled={loading}
                          >
                            Vote {playerLabelById(activeRoom, target.playerId, target.playerNo)}
                          </button>
                        ))}
                      </div>
                    )}
                  </section>
                </div>
              </>
            ) : (
              <div className="empty-state">Create or join a room</div>
            )}
          </section>
        </div>
      </section>
    </main>
  );
}

function applyRoomEvent(
  event: WsEvent<unknown>,
  setActiveRoom: React.Dispatch<React.SetStateAction<RoomView | null>>,
  setStatus: React.Dispatch<React.SetStateAction<string>>,
) {
  if (event.type === 'ROOM_STATE_UPDATED') {
    setActiveRoom(event.payload as RoomView);
    return;
  }
  if (event.type === 'MESSAGE_CREATED') {
    const message = event.payload as ChatMessageView;
    setActiveRoom((room) => {
      if (!room || room.roomId !== message.roomId || room.messages.some((item) => item.messageId === message.messageId)) {
        return room;
      }
      return { ...room, messages: [...room.messages, message] };
    });
    return;
  }
  if (event.type === 'STAGE_CHANGED') {
    void event;
    return;
  }
  if (event.type === 'GAME_ENDED') {
    setStatus('Game ended');
  }
}

function toMessage(error: unknown, fallback: string) {
  return error instanceof Error ? error.message : fallback;
}

function shortId(roomId: string) {
  return roomId.slice(0, 8);
}

function colorValue(color: string) {
  const values: Record<string, string> = {
    red: '#d94b4b',
    blue: '#3977d8',
    green: '#2f9d73',
    yellow: '#d5a520',
    purple: '#8b5dc8',
    cyan: '#1f9bad',
    pink: '#d45d91',
    gray: '#7a8494',
  };
  return values[color] ?? '#7a8494';
}

function objectiveLabel(objective: RoomObjective) {
  return objective === 'FIND_AI' ? '找出 AI' : '找出叛徒';
}

function playerLabel(playerNo: number, color: string) {
  return `Player ${playerNo} (${colorName(color)})`;
}

function playerLabelById(room: RoomView, playerId: string | null, fallbackPlayerNo: number | null) {
  if (!playerId) {
    return fallbackPlayerNo == null ? 'Player ?' : `Player ${fallbackPlayerNo}`;
  }
  const player = room.players.find((item) => item.playerId === playerId);
  if (!player) {
    return fallbackPlayerNo == null ? 'Player ?' : `Player ${fallbackPlayerNo}`;
  }
  return playerLabel(player.playerNo, player.color);
}

function messageStyle(message: ChatMessageView, room: RoomView): React.CSSProperties {
  if (message.messageType !== 'PLAYER' || !message.playerId) {
    return {};
  }
  const player = room.players.find((item) => item.playerId === message.playerId);
  if (!player) {
    return {};
  }
  return {
    borderLeft: `4px solid ${colorValue(player.color)}`,
  };
}

function colorName(color: string) {
  return color.toUpperCase();
}

function stageLabel(stage: GameStage) {
  const labels: Record<GameStage, string> = {
    WAITING: '等待開始',
    DISCUSSION: '討論階段',
    VOTING: '投票階段',
    ELIMINATION: '淘汰階段',
    ENDED: '結束階段',
  };
  return labels[stage];
}

function getRemainingSeconds(stageEndsAt: string | null, nowMs: number) {
  if (!stageEndsAt) {
    return null;
  }
  const endMs = new Date(stageEndsAt).getTime();
  if (Number.isNaN(endMs)) {
    return null;
  }
  return Math.max(0, Math.ceil((endMs - nowMs) / 1000));
}

function getSendCooldownRemainingSeconds(room: RoomView | null, player: PlayerPublicView | null, nowMs: number) {
  if (!room || !player) {
    return 0;
  }
  const lastMyMessage = [...room.messages]
    .reverse()
    .find((message) => message.playerId === player.playerId);
  if (!lastMyMessage) {
    return 0;
  }
  const lastMessageMs = new Date(lastMyMessage.createdAt).getTime();
  if (Number.isNaN(lastMessageMs)) {
    return 0;
  }
  const cooldownEndsAt = lastMessageMs + room.messageCooldownSeconds * 1000;
  return Math.max(0, Math.ceil((cooldownEndsAt - nowMs) / 1000));
}

declare global {
  interface Window {
    __HOST_GAME_ROOT__?: Root;
  }
}

const rootElement = document.getElementById('root');
if (!rootElement) {
  throw new Error('Root element not found.');
}

window.__HOST_GAME_ROOT__ ??= createRoot(rootElement);
window.__HOST_GAME_ROOT__.render(<App />);
