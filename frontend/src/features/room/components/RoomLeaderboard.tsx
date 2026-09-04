import { Typography } from 'antd'
import type { RoomPlayer } from '../api/roomApi'

const { Text } = Typography

/** Bảng xếp hạng trực tiếp trong phòng (FR-23). */
export default function RoomLeaderboard({
  players,
  currentPlayerId,
  title = 'Bảng xếp hạng',
}: {
  players: RoomPlayer[]
  currentPlayerId?: string
  title?: string
}) {
  return (
    <div className="border border-line bg-surface">
      <div className="border-b border-line px-4 py-3">
        <Text className="font-bold!">{title}</Text>
      </div>

      {players.length === 0 ? (
        <div className="px-4 py-6 text-center">
          <Text className="text-ink-soft text-xs">Chưa có ai trong phòng</Text>
        </div>
      ) : (
        <ul className="m-0 list-none p-0">
          {players.map((player) => {
            const isMe = player.playerId === currentPlayerId
            return (
              <li
                key={player.playerId}
                className={`flex items-center gap-3 border-b border-line px-4 py-2 last:border-b-0 ${
                  isMe ? 'bg-surface-subtle' : ''
                }`}
              >
                <span className="w-6 text-center text-xs font-bold text-ink-soft">
                  {player.rank}
                </span>
                <span className="flex-1 truncate text-sm font-bold">
                  {player.displayName}
                  {isMe && <span className="ml-1 text-ink-soft font-normal">(bạn)</span>}
                </span>
                <span className="text-sm font-bold tabular-nums">{player.score}</span>
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}
