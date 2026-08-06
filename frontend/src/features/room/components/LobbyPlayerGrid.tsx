import { Tag, Typography } from 'antd'
import type { RoomPlayer } from '../api/roomApi'
import PlayerAvatarBadge from './PlayerAvatarBadge'

const { Text } = Typography

/**
 * Phòng chờ: mỗi người chơi là một thẻ có avatar, tên và trạng thái sẵn sàng (FR-21).
 * Danh sách cập nhật theo thời gian thực qua WebSocket, không cần tải lại trang.
 */
export default function LobbyPlayerGrid({
  players,
  hostId,
  currentPlayerId,
}: {
  players: RoomPlayer[]
  hostId: string
  currentPlayerId?: string
}) {
  if (players.length === 0) {
    return (
      <div className="border border-dashed border-line p-8 text-center">
        <Text className="text-ink-soft">Chưa có ai trong phòng</Text>
      </div>
    )
  }

  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
      {players.map((player) => {
        const isMe = player.playerId === currentPlayerId
        return (
          <div
            key={player.playerId}
            className={`flex flex-col items-center gap-2 border p-4 ${
              player.ready ? 'border-green-500 bg-green-50' : 'border-line bg-white'
            }`}
          >
            <PlayerAvatarBadge emoji={player.avatarEmoji} color={player.avatarColor} size="lg" />

            <Text className="max-w-full truncate text-center font-bold!">
              {player.displayName}
              {isMe && <span className="text-ink-soft font-normal"> (bạn)</span>}
            </Text>

            <div className="flex flex-wrap justify-center gap-1">
              {player.playerId === hostId && <Tag className="mr-0!">Chủ phòng</Tag>}
              {player.guest && <Tag className="mr-0!">Khách</Tag>}
            </div>

            <Tag color={player.ready ? 'green' : undefined} className="mr-0!">
              {player.ready ? 'Đã sẵn sàng' : 'Đang chờ'}
            </Tag>
          </div>
        )
      })}
    </div>
  )
}
