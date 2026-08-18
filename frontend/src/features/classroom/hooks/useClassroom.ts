import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { getApiErrorMessage } from '@/shared/api/client'
import { classroomApi, type MemberRole } from '../api/classroomApi'

const KEY = 'classrooms'

export function useMyClassrooms() {
  return useQuery({ queryKey: [KEY, 'list'], queryFn: () => classroomApi.cuaToi() })
}

export function useClassroom(id: string | undefined) {
  return useQuery({
    queryKey: [KEY, id],
    queryFn: () => classroomApi.chiTiet(id!),
    enabled: Boolean(id),
    // Lớp của người khác trả 404 — đó là kết quả bình thường, không phải sự cố cần thử lại
    retry: false,
  })
}

export function useMembers(id: string | undefined, enabled = true) {
  return useQuery({
    queryKey: [KEY, id, 'members'],
    queryFn: () => classroomApi.thanhVien(id!),
    // Học sinh gọi sẽ nhận 404. `enabled` để không gọi ngay từ đầu, nhưng vẫn tắt retry phòng khi bên gọi
    // truyền sai — ba lần thử lại cho một 404 cố ý là ba request vô ích
    enabled: Boolean(id) && enabled,
    retry: false,
  })
}

export function useClassAssignments(id: string | undefined) {
  return useQuery({
    queryKey: [KEY, id, 'assignments'],
    queryFn: () => classroomApi.baiTapCuaLop(id!),
    enabled: Boolean(id),
    retry: false,
  })
}

export function useMyAssignments() {
  return useQuery({ queryKey: ['assignments', 'me'], queryFn: () => classroomApi.baiTapCuaToi() })
}

export function useAssignmentResults(assignmentId: string | undefined) {
  return useQuery({
    queryKey: ['assignments', assignmentId, 'results'],
    queryFn: () => classroomApi.ketQua(assignmentId!),
    enabled: Boolean(assignmentId),
    retry: false,
  })
}

export function useCreateClassroom() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: { name: string; description?: string }) => classroomApi.tao(body),
    onSuccess: (lop) => {
      message.success(`Đã tạo lớp. Mã lớp: ${lop.classCode}`)
      queryClient.invalidateQueries({ queryKey: [KEY] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useJoinClassroom() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (code: string) => classroomApi.thamGia(code),
    onSuccess: (lop) => {
      message.success(`Đã vào lớp ${lop.name}`)
      queryClient.invalidateQueries({ queryKey: [KEY] })
      // Vào lớp mới thì có thể có bài tập chờ sẵn — làm mất hiệu lực cả danh sách bài tập của tôi
      queryClient.invalidateQueries({ queryKey: ['assignments'] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useDeleteClassroom() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => classroomApi.xoa(id),
    onSuccess: () => {
      message.success('Đã xoá lớp')
      queryClient.invalidateQueries({ queryKey: [KEY] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useAssignQuiz(classroomId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: {
      quizId: string
      title: string
      instruction?: string
      openAt?: string
      dueAt?: string
    }) => classroomApi.giaoBai(classroomId, body),
    onSuccess: () => {
      message.success('Đã giao bài cho lớp')
      queryClient.invalidateQueries({ queryKey: [KEY] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useDeleteAssignment(classroomId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (assignmentId: string) => classroomApi.xoaBaiTap(assignmentId),
    onSuccess: () => {
      message.success('Đã gỡ bài tập khỏi lớp')
      queryClient.invalidateQueries({ queryKey: [KEY, classroomId] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useChangeMemberRole(classroomId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ userId, role }: { userId: string; role: MemberRole }) =>
      classroomApi.doiVaiTro(classroomId, userId, role),
    onSuccess: (m) => {
      message.success(`${m.displayName} nay là ${m.vaiTroNhan.toLowerCase()}`)
      queryClient.invalidateQueries({ queryKey: [KEY, classroomId] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useRemoveMember(classroomId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (userId: string) => classroomApi.xoaThanhVien(classroomId, userId),
    onSuccess: () => {
      message.success('Đã xoá khỏi lớp')
      queryClient.invalidateQueries({ queryKey: [KEY, classroomId] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

/**
 * Bắt đầu làm bài tập.
 *
 * Không hiện thông báo thành công: người dùng vừa bấm "Làm bài" và ngay sau đó màn hình chuyển sang bài
 * làm — một hộp "đã bắt đầu" bật lên giữa lúc chuyển trang chỉ che mất đề.
 */
export function useStartAssignment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (assignmentId: string) => classroomApi.batDauBaiTap(assignmentId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['assignments'] }),
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}
