import { useState } from 'react'
import { Button, Form, Input, Modal, Popconfirm, Table, Tag, Typography } from 'antd'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons'
import PageHeader from '@/shared/components/PageHeader'
import { useAdminCategories, useDeleteCategory, useSaveCategory } from '../hooks/useAdmin'
import type { AdminCategory, CategoryBody } from '../api/adminApi'

const { Text } = Typography

/**
 * Quản lý danh mục quiz (FR-79).
 *
 * Xoá danh mục còn quiz đang dùng bị backend trả 409 kèm số lượng. Giao diện không tự chặn trước bằng
 * cách ẩn nút: số quiz hiện trong bảng có thể đã cũ, và nơi duy nhất biết chắc là cơ sở dữ liệu tại thời
 * điểm xoá.
 */
export default function AdminCategoriesPage() {
  const { data, isLoading } = useAdminCategories()
  const save = useSaveCategory()
  const remove = useDeleteCategory()

  const [dangSua, setDangSua] = useState<AdminCategory | null>(null)
  const [moForm, setMoForm] = useState(false)
  const [form] = Form.useForm<CategoryBody>()

  const moThem = () => {
    setDangSua(null)
    form.resetFields()
    setMoForm(true)
  }

  const moSua = (category: AdminCategory) => {
    setDangSua(category)
    form.setFieldsValue({
      name: category.name,
      slug: category.slug,
      description: category.description ?? undefined,
    })
    setMoForm(true)
  }

  const luu = async () => {
    const values = await form.validateFields()
    await save.mutateAsync({ id: dangSua?.id, body: values })
    setMoForm(false)
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Danh mục quiz"
        description="Danh mục dùng cho bộ lọc ở trang khám phá và cho biểu đồ phân bổ nội dung."
        actions={
          <Button type="primary" icon={<PlusOutlined />} onClick={moThem}>
            Thêm danh mục
          </Button>
        }
      />

      <Table<AdminCategory>
        rowKey="id"
        loading={isLoading}
        dataSource={data}
        pagination={false}
        columns={[
          {
            title: 'Tên danh mục',
            dataIndex: 'name',
            render: (name: string, row) => (
              <div className="min-w-0">
                <Text className="font-bold!">{name}</Text>
                {row.description && (
                  <div className="text-ink-soft text-xs">{row.description}</div>
                )}
              </div>
            ),
          },
          {
            title: 'Đường dẫn',
            dataIndex: 'slug',
            render: (slug: string) => <Text className="text-ink-soft text-xs">/{slug}</Text>,
          },
          {
            title: 'Số quiz',
            dataIndex: 'soQuiz',
            width: 110,
            render: (soQuiz: number) =>
              soQuiz > 0 ? <Tag>{soQuiz}</Tag> : <Text className="text-ink-soft text-xs">—</Text>,
          },
          {
            title: '',
            width: 150,
            render: (_, row) => (
              <div className="flex gap-1">
                <Button size="small" icon={<EditOutlined />} onClick={() => moSua(row)}>
                  Sửa
                </Button>
                <Popconfirm
                  title="Xoá danh mục này?"
                  description={
                    row.soQuiz > 0
                      ? `Còn ${row.soQuiz} quiz đang dùng — hãy chuyển chúng sang danh mục khác trước.`
                      : 'Danh mục chưa có quiz nào, xoá được ngay.'
                  }
                  okText="Xoá"
                  cancelText="Thôi"
                  okButtonProps={{ danger: true, loading: remove.isPending }}
                  onConfirm={() => remove.mutate(row.id)}
                >
                  <Button size="small" danger icon={<DeleteOutlined />} />
                </Popconfirm>
              </div>
            ),
          },
        ]}
      />

      <Modal
        open={moForm}
        title={dangSua ? `Sửa "${dangSua.name}"` : 'Thêm danh mục'}
        okText="Lưu"
        cancelText="Huỷ"
        confirmLoading={save.isPending}
        onOk={luu}
        onCancel={() => setMoForm(false)}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" requiredMark={false}>
          <Form.Item
            name="name"
            label="Tên danh mục"
            rules={[{ required: true, message: 'Nhập tên danh mục' }]}
          >
            <Input placeholder="Ví dụ: Cơ sở dữ liệu" />
          </Form.Item>
          <Form.Item
            name="slug"
            label="Đường dẫn"
            extra="Để trống thì hệ thống tự sinh từ tên, có bỏ dấu tiếng Việt."
          >
            <Input placeholder="co-so-du-lieu" />
          </Form.Item>
          <Form.Item name="description" label="Mô tả">
            <Input.TextArea rows={3} maxLength={500} showCount />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
