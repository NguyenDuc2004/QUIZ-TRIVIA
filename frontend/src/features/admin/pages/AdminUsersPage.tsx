import { useState } from 'react'
import { Input, InputNumber, Popconfirm, Select, Space, Switch, Table, Tag, Tooltip, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import EmptyState from '@/shared/components/EmptyState'
import PageHeader from '@/shared/components/PageHeader'
import { useAuthStore } from '@/features/auth/store/authStore'
import type { Role } from '@/features/auth/api/authApi'
import type { AdminUser } from '../api/adminApi'
import { useAdminUsers, useChangeRole, useSetAiQuota, useSetLocked } from '../hooks/useAdmin'

const { Text } = Typography

const ROLE_LABEL: Record<Role, string> = {
  LEARNER: 'Người học',
  CREATOR: 'Người tạo nội dung',
  ADMIN: 'Quản trị viên',
}

/**
 * Quản lý người dùng — bộ mặt **bảng điều khiển**: bảng dày thông tin (docs/ui-design-system.md §1).
 *
 * Hai chỗ giao diện cố tình khoá lại, khớp với chốt chặn ở backend:
 * - Không có nút xoá người dùng. Khoá là chặn đường vào, giữ nguyên dữ liệu của họ.
 * - Không cho tự khoá hoặc tự hạ vai trò chính mình — backend cũng chặn, nhưng để nút bấm được rồi
 *   báo lỗi là bắt người dùng học bằng cách thất bại.
 */
export default function AdminUsersPage() {
  const [page, setPage] = useState(0)
  const [keyword, setKeyword] = useState('')
  const [role, setRole] = useState<Role | undefined>()
  const setAiQuota = useSetAiQuota()
  const [locked, setLocked] = useState<boolean | undefined>()

  const currentUserId = useAuthStore((state) => state.user?.id)
  const { data, isFetching } = useAdminUsers({ keyword: keyword || undefined, role, locked, page, size: 10 })
  const changeRole = useChangeRole()
  const setUserLocked = useSetLocked()

  const columns: ColumnsType<AdminUser> = [
    {
      title: 'Người dùng',
      key: 'user',
      render: (_, row) => (
        <div>
          <Text className="block text-sm font-bold">{row.displayName}</Text>
          <Text className="text-ink-soft text-xs">{row.email}</Text>
          {row.locked && (
            <Tag color="red" className="mt-1">
              Đã khoá
            </Tag>
          )}
        </div>
      ),
    },
    {
      title: 'Vai trò',
      dataIndex: 'role',
      width: 210,
      render: (value: Role, row) => {
        const isSelf = row.id === currentUserId
        return (
          <Space direction="vertical" size={2}>
            <Select<Role>
              size="small"
              value={value}
              className="w-full"
              // Tự hạ vai trò của mình thì không còn ai mở lại được quyền quản trị
              disabled={isSelf || changeRole.isPending}
              onChange={(newRole) => changeRole.mutate({ id: row.id, role: newRole })}
              options={(Object.keys(ROLE_LABEL) as Role[]).map((r) => ({
                value: r,
                label: ROLE_LABEL[r],
              }))}
            />
            {isSelf && <Text className="text-ink-soft text-[10px]">Không thể tự đổi vai trò</Text>}
          </Space>
        )
      },
    },
    {
      title: 'Cách đăng nhập',
      dataIndex: 'loginMethod',
      width: 150,
      render: (value: string) => <Text className="text-xs">{value}</Text>,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'locked',
      width: 170,
      render: (value: boolean, row) => {
        const isSelf = row.id === currentUserId
        if (isSelf) {
          return <Tag color="green">Đang hoạt động</Tag>
        }
        return (
          <Popconfirm
            title={value ? 'Mở khoá tài khoản này?' : 'Khoá tài khoản này?'}
            description={
              value
                ? 'Người dùng sẽ đăng nhập lại được ngay.'
                : 'Mọi phiên đăng nhập của họ bị thu hồi ngay. Dữ liệu vẫn giữ nguyên.'
            }
            okText={value ? 'Mở khoá' : 'Khoá'}
            cancelText="Hủy"
            okButtonProps={{ danger: !value }}
            onConfirm={() => setUserLocked.mutate({ id: row.id, locked: !value })}
          >
            <Switch
              size="small"
              checked={!value}
              loading={setUserLocked.isPending}
              checkedChildren="Hoạt động"
              unCheckedChildren="Đã khoá"
            />
          </Popconfirm>
        )
      },
    },
    {
      // FR-84. Cột này hiện CẢ hạn mức lẫn số đã dùng hôm nay: một ô nhập không kèm mức tiêu thụ thật
      // thì quản trị viên chỉ đoán, và con số họ đặt sẽ hoặc quá chặt hoặc vô nghĩa.
      title: (
        <Tooltip title="Số lượt gọi AI tối đa mỗi ngày. Để trống = dùng mặc định hệ thống; đặt 0 = cấm người này gọi AI.">
          <span>Hạn mức AI</span>
        </Tooltip>
      ),
      dataIndex: 'aiDailyQuota',
      width: 190,
      render: (quota: number | null, row) => (
        <div className="flex flex-col gap-1">
          <InputNumber
            size="small"
            min={0}
            max={10000}
            value={quota}
            placeholder="Mặc định"
            className="w-full"
            disabled={setAiQuota.isPending}
            // Ô trống gửi null = xoá hạn mức riêng. KHÔNG quy về 0: 0 nghĩa là cấm, một quyết định khác hẳn.
            onChange={(value) => setAiQuota.mutate({ id: row.id, quota: value ?? null })}
          />
          <Text className="text-ink-soft text-xs">
            {quota === 0
              ? 'Đang cấm gọi AI'
              : `Hôm nay đã dùng ${row.aiUsedToday}${quota === null ? '' : `/${quota}`}`}
          </Text>
        </div>
      ),
    },
    {
      title: 'Ngày tạo',
      dataIndex: 'createdAt',
      width: 130,
      render: (value: string) => (
        <Text className="text-xs">{new Date(value).toLocaleDateString('vi-VN')}</Text>
      ),
    },
  ]

  return (
    <Space direction="vertical" size="large" className="w-full">
      <PageHeader
        title="Quản lý người dùng"
        description="Đổi vai trò và khoá tài khoản. Khoá là chặn đường vào — dữ liệu của người dùng vẫn giữ nguyên."
      />

      <Space wrap>
        <Input.Search
          allowClear
          placeholder="Tìm theo email hoặc tên"
          className="w-72"
          onSearch={(value) => {
            setKeyword(value)
            setPage(0)
          }}
        />
        <Select<Role>
          allowClear
          placeholder="Mọi vai trò"
          className="w-48"
          value={role}
          onChange={(value) => {
            setRole(value)
            setPage(0)
          }}
          options={(Object.keys(ROLE_LABEL) as Role[]).map((r) => ({
            value: r,
            label: ROLE_LABEL[r],
          }))}
        />
        <Select<string>
          allowClear
          placeholder="Mọi trạng thái"
          className="w-44"
          value={locked === undefined ? undefined : String(locked)}
          onChange={(value) => {
            setLocked(value === undefined ? undefined : value === 'true')
            setPage(0)
          }}
          options={[
            { value: 'false', label: 'Đang hoạt động' },
            { value: 'true', label: 'Đã khoá' },
          ]}
        />
      </Space>

      {!isFetching && data && data.content.length === 0 ? (
        <EmptyState
          title="Không có người dùng nào khớp bộ lọc"
          hint="Thử bỏ bộ lọc hoặc đổi từ khoá tìm kiếm"
        />
      ) : (
        <Table<AdminUser>
          scroll={{ x: 'max-content' }}
          rowKey="id"
          columns={columns}
          dataSource={data?.content ?? []}
          loading={isFetching}
          rowClassName={(row) => (row.locked ? 'opacity-60' : '')}
          pagination={{
            current: page + 1,
            pageSize: 10,
            total: data?.totalElements ?? 0,
            onChange: (p) => setPage(p - 1),
            showSizeChanger: false,
          }}
        />
      )}
    </Space>
  )
}
