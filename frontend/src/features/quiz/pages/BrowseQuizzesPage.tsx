import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Pagination, Select, Skeleton, Space } from 'antd'
import EmptyState from '@/shared/components/EmptyState'
import PageHeader from '@/shared/components/PageHeader'
import type { Difficulty } from '../api/quizApi'
import { DIFFICULTY_OPTIONS } from '../constants'
import { useCategories, useQuizList } from '../hooks/useQuizQueries'
import QuizCard from '../components/QuizCard'

const PAGE_SIZE = 12

/**
 * Trang "Khám phá quiz" — bộ mặt **học viên**: lưới card, chip danh mục
 * (docs/ui-design-system.md §1). Từ khoá lấy từ ô tìm kiếm trên header qua `?q=`.
 */
export default function BrowseQuizzesPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const keyword = searchParams.get('q') ?? undefined

  const [page, setPage] = useState(0)
  const [categoryId, setCategoryId] = useState<string | undefined>()
  const [difficulty, setDifficulty] = useState<Difficulty | undefined>()

  const { data: categories } = useCategories()
  const { data, isPending } = useQuizList({
    page,
    size: PAGE_SIZE,
    q: keyword,
    categoryId,
    difficulty,
  })

  const chipClass = (active: boolean) =>
    `rounded border px-3 py-1.5 text-sm font-bold whitespace-nowrap ${
      active
        ? 'border-ink bg-ink text-white'
        : 'border-line bg-white text-ink hover:border-ink'
    }`

  return (
    <Space direction="vertical" size="large" className="w-full">
      <PageHeader
        title="Khám phá quiz"
        description={
          keyword
            ? `Kết quả cho “${keyword}” — ${data?.totalElements ?? 0} quiz`
            : 'Các quiz đã được xuất bản công khai'
        }
      />

      {/* Chip danh mục kiểu thanh chủ đề của Udemy */}
      <div className="flex gap-2 overflow-x-auto pb-1">
        <button
          type="button"
          className={chipClass(categoryId === undefined)}
          onClick={() => {
            setCategoryId(undefined)
            setPage(0)
          }}
        >
          Tất cả
        </button>
        {(categories ?? []).map((category) => (
          <button
            key={category.id}
            type="button"
            className={chipClass(categoryId === category.id)}
            onClick={() => {
              setCategoryId(category.id)
              setPage(0)
            }}
          >
            {category.name}
          </button>
        ))}
      </div>

      <div className="flex items-center gap-3">
        <Select
          allowClear
          placeholder="Độ khó"
          style={{ width: 160 }}
          value={difficulty}
          onChange={(value) => {
            setDifficulty(value)
            setPage(0)
          }}
          options={DIFFICULTY_OPTIONS}
        />
        <span className="text-ink-soft text-xs">{data?.totalElements ?? 0} quiz</span>
      </div>

      {isPending ? (
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {Array.from({ length: 4 }).map((_, index) => (
            <div key={index} className="browse-card p-3">
              <Skeleton active paragraph={{ rows: 3 }} />
            </div>
          ))}
        </div>
      ) : (data?.content.length ?? 0) === 0 ? (
        <div className="browse-card">
          <EmptyState
            title="Chưa có quiz nào phù hợp"
            hint={
              keyword || categoryId || difficulty
                ? 'Thử bỏ bớt bộ lọc hoặc đổi từ khoá tìm kiếm.'
                : 'Chưa có quiz công khai nào trong hệ thống.'
            }
          />
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
            {data?.content.map((quiz) => (
              <QuizCard
                key={quiz.id}
                quiz={quiz}
                onClick={() => navigate(`/quizzes/${quiz.id}`)}
              />
            ))}
          </div>

          <div className="flex justify-center pt-2">
            <Pagination
              current={(data?.page ?? 0) + 1}
              pageSize={data?.size ?? PAGE_SIZE}
              total={data?.totalElements ?? 0}
              showSizeChanger={false}
              onChange={(nextPage) => setPage(nextPage - 1)}
            />
          </div>
        </>
      )}
    </Space>
  )
}
