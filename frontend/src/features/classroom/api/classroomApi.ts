import { apiClient } from '@/shared/api/client'

/** Vai trò của **tôi** trong một lớp. Máy chủ tính, frontend không tự so ownerId với id của mình. */
export type VaiTroLop = 'OWNER' | 'CO_TEACHER' | 'STUDENT'

export type MemberRole = 'STUDENT' | 'CO_TEACHER'

/** Trạng thái một bài tập với **chính người gọi**. Khớp `TrangThaiBaiTap` của backend. */
export type TrangThaiBaiTap = 'CHUA_LAM' | 'DANG_LAM' | 'DA_NOP' | 'NOP_TRE' | 'QUA_HAN'

export interface Classroom {
  id: string
  name: string
  description: string | null
  /** **null với học sinh** — mã lớp là thứ để mời người vào, chỉ giáo viên cầm. */
  classCode: string | null
  ownerName: string
  soThanhVien: number
  soBaiTap: number
  vaiTroCuaToi: VaiTroLop
  createdAt: string
}

export interface Member {
  userId: string
  displayName: string
  email: string
  role: MemberRole
  vaiTroNhan: string
  joinedAt: string
}

export interface Assignment {
  id: string
  classroomId: string
  tenLop: string
  title: string
  instruction: string | null
  quizId: string
  quizTitle: string
  soCau: number
  openAt: string | null
  dueAt: string | null
  createdAt: string
  /** Bốn trường dưới chỉ khác null ở màn của học sinh (`/me/assignments`). */
  trangThai: TrangThaiBaiTap | null
  trangThaiNhan: string | null
  attemptId: string | null
  diem: number | null
  diemToiDa: number | null
}

export interface AssignmentResultRow {
  userId: string
  tenHocSinh: string
  attemptId: string | null
  /** null khi chưa nộp — **khác 0 điểm**, đừng trộn hai thứ đó khi hiển thị. */
  diem: number | null
  diemToiDa: number | null
  nopLuc: string | null
  trangThai: TrangThaiBaiTap
  trangThaiNhan: string
}

export interface AssignmentResults {
  baiTap: Assignment
  soThanhVien: number
  soDaNop: number
  soNopTre: number
  /** Tính trên **bài đã nộp**; null khi chưa ai nộp. */
  diemTrungBinh: number | null
  danhSach: AssignmentResultRow[]
}

export const classroomApi = {
  cuaToi: () => apiClient.get<Classroom[]>('/classrooms').then((res) => res.data),

  chiTiet: (id: string) => apiClient.get<Classroom>(`/classrooms/${id}`).then((res) => res.data),

  tao: (body: { name: string; description?: string }) =>
    apiClient.post<Classroom>('/classrooms', body).then((res) => res.data),

  capNhat: (id: string, body: { name: string; description?: string }) =>
    apiClient.put<Classroom>(`/classrooms/${id}`, body).then((res) => res.data),

  xoa: (id: string) => apiClient.delete<void>(`/classrooms/${id}`).then(() => undefined),

  thamGia: (code: string) =>
    apiClient.post<Classroom>(`/classrooms/join/${code.trim().toUpperCase()}`).then((res) => res.data),

  thanhVien: (id: string) =>
    apiClient.get<Member[]>(`/classrooms/${id}/members`).then((res) => res.data),

  doiVaiTro: (id: string, userId: string, role: MemberRole) =>
    apiClient
      .put<Member>(`/classrooms/${id}/members/${userId}/role`, { role })
      .then((res) => res.data),

  xoaThanhVien: (id: string, userId: string) =>
    apiClient.delete<void>(`/classrooms/${id}/members/${userId}`).then(() => undefined),

  baiTapCuaLop: (id: string) =>
    apiClient.get<Assignment[]>(`/classrooms/${id}/assignments`).then((res) => res.data),

  giaoBai: (
    id: string,
    body: { quizId: string; title: string; instruction?: string; openAt?: string; dueAt?: string },
  ) => apiClient.post<Assignment>(`/classrooms/${id}/assignments`, body).then((res) => res.data),

  xoaBaiTap: (assignmentId: string) =>
    apiClient.delete<void>(`/assignments/${assignmentId}`).then(() => undefined),

  baiTapCuaToi: () => apiClient.get<Assignment[]>('/me/assignments').then((res) => res.data),

  /** Bắt đầu hoặc làm tiếp. Trả về id lượt làm bài để điều hướng sang màn làm bài đã có. */
  batDauBaiTap: (assignmentId: string) =>
    apiClient
      .post<{ attemptId: string }>(`/assignments/${assignmentId}/attempts`)
      .then((res) => res.data.attemptId),

  ketQua: (assignmentId: string) =>
    apiClient.get<AssignmentResults>(`/assignments/${assignmentId}/results`).then((res) => res.data),

  /**
   * Tải bảng điểm CSV (FR-58).
   *
   * Trả `Blob` chứ không phải JSON, và **để bên gọi tự tạo link tải**: gọi `window.open` ở đây thì request
   * không mang header `Authorization`, server trả 401 và người dùng nhận một tab trắng không rõ vì sao.
   */
  taiBangDiemCsv: (assignmentId: string) =>
    apiClient
      .get<Blob>(`/assignments/${assignmentId}/results.csv`, { responseType: 'blob' })
      .then((res) => res.data),

}
