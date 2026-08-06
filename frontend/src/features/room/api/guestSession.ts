/**
 * Phiên khách vãng lai trong một phòng.
 *
 * Dùng `sessionStorage` chứ không phải `localStorage`: phiên chỉ có nghĩa trong đúng một ván đấu,
 * đóng tab là xong. Để trong localStorage thì lần sau mở máy vẫn còn một khoá chết nằm đó.
 */
const KEY_PREFIX = 'roomGuest:'

export interface StoredGuestSession {
  guestKey: string
  playerId: string
}

export const guestSession = {
  get(roomCode: string): StoredGuestSession | null {
    const raw = sessionStorage.getItem(KEY_PREFIX + roomCode)
    return raw ? (JSON.parse(raw) as StoredGuestSession) : null
  },

  save(roomCode: string, session: StoredGuestSession) {
    sessionStorage.setItem(KEY_PREFIX + roomCode, JSON.stringify(session))
  },

  clear(roomCode: string) {
    sessionStorage.removeItem(KEY_PREFIX + roomCode)
  },
}
