import React from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { Client } from '@stomp/stompjs';
import './styles.css';

type GameType = 'AI_CHAT_WEREWOLF';
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

const api = {
  listWaitingRooms: () => request<RoomView[]>('/api/rooms?status=WAITING'),
  createRoom: () =>
    request<RoomView>('/api/rooms', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ gameType: 'AI_CHAT_WEREWOLF' }),
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
      const room = await api.createRoom();
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
                      {room.currentStage} | {room.players.length} players
                    </div>
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
                      {activeRoom.roomStatus} | {activeRoom.currentStage} | Round {activeRoom.round}
                    </p>
                  </div>
                  <div className="actions compact-actions">
                    {currentPlayer && <span className="player-badge">Player {currentPlayer.playerNo}</span>}
                    {activeRoom.currentStage === 'WAITING' && (
                      <>
                        <button type="button" onClick={addAiPlayer} disabled={loading}>
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
                        <span>Player {player.playerNo}</span>
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
                          <article className={`message ${message.messageType.toLowerCase()}`} key={message.messageId}>
                            <div className="message-speaker">
                              {message.messageType === 'HOST' ? 'Host' : `Player ${message.playerNo}`}
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
                        <button type="button" onClick={sendMessage} disabled={loading || !sendMessageAction || !messageText.trim()}>
                          Send
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
                            Vote Player {target.playerNo}
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
