import { Button, Space, Typography } from 'antd'
import type { AvatarOption } from '../api/roomApi'
import PlayerAvatarBadge from './PlayerAvatarBadge'

const { Text } = Typography

/**
 * Chọn nhân vật vui nhộn cho phòng đấu.
 * <p>
 * Có nút "Ngẫu nhiên" vì phần lớn người chơi không muốn cân nhắc — họ chỉ muốn vào chơi nhanh.
 */
export default function AvatarPicker({
  avatars,
  value,
  onChange,
  label = 'Chọn nhân vật',
}: {
  avatars: AvatarOption[]
  value: string | undefined
  onChange: (code: string) => void
  label?: string
}) {
  const pickRandom = () => {
    if (avatars.length === 0) return
    onChange(avatars[Math.floor(Math.random() * avatars.length)].code)
  }

  return (
    <div className="flex flex-col gap-2">
      <Space size={8}>
        <Text className="text-ink-soft text-xs font-bold">{label}</Text>
        <Button size="small" onClick={pickRandom}>
          Ngẫu nhiên
        </Button>
      </Space>

      <div className="flex flex-wrap gap-2">
        {avatars.map((avatar) => (
          <button
            key={avatar.code}
            type="button"
            title={avatar.code}
            onClick={() => onChange(avatar.code)}
            className="cursor-pointer border-0 bg-transparent p-0"
          >
            <PlayerAvatarBadge
              emoji={avatar.emoji}
              color={avatar.color}
              ring={value === avatar.code}
            />
          </button>
        ))}
      </div>
    </div>
  )
}
