import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { getApiErrorMessage } from '@/shared/api/client'
import {
  categoryApi,
  questionApi,
  quizApi,
  type QuestionBody,
  type QuestionListParams,
  type QuizBody,
  type QuizListParams,
} from '../api/quizApi'

const QUIZ_KEY = 'quizzes'
const QUESTION_KEY = 'questions'

export function useCategories() {
  return useQuery({
    queryKey: ['categories'],
    queryFn: categoryApi.list,
    staleTime: 10 * 60 * 1000, // danh mục ít đổi
  })
}

export function useQuizList(params: QuizListParams) {
  return useQuery({
    queryKey: [QUIZ_KEY, params],
    queryFn: () => quizApi.list(params),
  })
}

/** Thông tin giới thiệu quiz, không kèm câu hỏi — dùng ở trang trước khi vào làm bài. */
export function useQuizSummary(quizId: string | undefined) {
  return useQuery({
    queryKey: [QUIZ_KEY, quizId, 'summary'],
    queryFn: () => quizApi.get(quizId!),
    enabled: Boolean(quizId),
  })
}

export function useQuizDetail(quizId: string | undefined) {
  return useQuery({
    queryKey: [QUIZ_KEY, quizId, 'detail'],
    queryFn: () => quizApi.getDetail(quizId!),
    enabled: Boolean(quizId),
  })
}

export function useCreateQuiz() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: QuizBody) => quizApi.create(body),
    onSuccess: () => {
      message.success('Đã tạo quiz')
      queryClient.invalidateQueries({ queryKey: [QUIZ_KEY] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useUpdateQuiz() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: QuizBody }) => quizApi.update(id, body),
    onSuccess: () => {
      message.success('Đã lưu quiz')
      queryClient.invalidateQueries({ queryKey: [QUIZ_KEY] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useDeleteQuiz() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => quizApi.remove(id),
    onSuccess: () => {
      message.success('Đã xóa quiz')
      queryClient.invalidateQueries({ queryKey: [QUIZ_KEY] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useSetQuizQuestions() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, questionIds }: { id: string; questionIds: string[] }) =>
      quizApi.setQuestions(id, questionIds),
    onSuccess: (_data, variables) => {
      message.success('Đã cập nhật danh sách câu hỏi')
      queryClient.invalidateQueries({ queryKey: [QUIZ_KEY] })
      queryClient.invalidateQueries({ queryKey: [QUIZ_KEY, variables.id, 'detail'] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useQuestionBank(params: QuestionListParams) {
  return useQuery({
    queryKey: [QUESTION_KEY, params],
    queryFn: () => questionApi.list(params),
  })
}

export function useCreateQuestion() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: QuestionBody) => questionApi.create(body),
    onSuccess: () => {
      message.success('Đã thêm câu hỏi vào ngân hàng')
      queryClient.invalidateQueries({ queryKey: [QUESTION_KEY] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useUpdateQuestion() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: QuestionBody }) => questionApi.update(id, body),
    onSuccess: () => {
      message.success('Đã lưu câu hỏi')
      queryClient.invalidateQueries({ queryKey: [QUESTION_KEY] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useDeleteQuestion() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => questionApi.remove(id),
    onSuccess: () => {
      message.success('Đã xóa câu hỏi')
      queryClient.invalidateQueries({ queryKey: [QUESTION_KEY] })
    },
    // Backend trả 409 kèm số quiz đang dùng câu hỏi — hiển thị nguyên văn cho người dùng
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}
