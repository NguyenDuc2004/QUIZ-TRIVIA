/**
 * Chứng minh vai trò của Redis Pub/Sub — số liệu cho báo cáo mục 3.5.
 *
 * VẤN ĐỀ CẦN CHỨNG MINH: phiếu yêu cầu "so sánh có/không Redis Pub/Sub". Nhưng kiến trúc hiện tại
 * cho MỌI sự kiện đi qua Redis, kể cả tới người chơi trên chính instance vừa phát — nên không có
 * chế độ "tắt Redis" để bật/tắt mà so. Bỏ Redis đi thì tính năng phòng đấu nhiều instance không
 * còn tồn tại, chứ không phải chạy chậm hơn.
 *
 * CÁCH CHỨNG MINH THAY THẾ: chạy HAI instance backend (8080 và 8081) dùng chung Redis, chia người
 * chơi ra hai bên, rồi để host trên instance A bắt đầu ván.
 *
 *   - Hai tiến trình JVM này KHÔNG có kênh liên lạc nào khác. Broker của Spring nằm trong bộ nhớ
 *     từng instance; chúng chỉ dùng chung PostgreSQL (không phải kênh nhắn tin) và Redis.
 *   - Nếu người chơi bên B nhận được câu hỏi do A phát, thì Redis Pub/Sub là con đường DUY NHẤT
 *     có thể. Không cần tắt nó đi mới chứng minh được — đó là suy luận loại trừ.
 *   - Bỏ Redis ra thì bên B nhận 0 sự kiện. Đó chính là "không có Redis", và kết quả không phải là
 *     chậm hơn mà là **hỏng hẳn**.
 *
 * Đồng thời đo luôn: sự kiện đi vòng qua Redis sang instance khác đắt hơn bao nhiêu so với ở lại
 * ngay trên instance phát.
 *
 * Chạy (cần backend thứ hai ở cổng 8081):
 *   node loadtest_two_instances.mjs 40
 */
import { Client } from '@stomp/stompjs'

const A = { api: 'http://localhost:8080/api/v1', ws: 'ws://localhost:8080/ws/websocket', ten: 'A (8080)' }
const B = { api: 'http://localhost:8081/api/v1', ws: 'ws://localhost:8081/ws/websocket', ten: 'B (8081)' }

const PLAYERS = Number(process.argv[2] ?? 40)
const SECONDS_PER_QUESTION = 60
const QUESTION_COUNT = 3

async function call(node, method, path, body, token) {
  const res = await fetch(node.api + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const text = await res.text()
  let json = null
  try { json = text ? JSON.parse(text) : null } catch { /* không phải JSON */ }
  return [res.status, json]
}

const uniq = () => Math.random().toString(36).slice(2, 8)

function percentile(values, p) {
  if (values.length === 0) return null
  const sorted = [...values].sort((a, b) => a - b)
  return sorted[Math.max(0, Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1))]
}

function connectPlayer(node, roomCode, guestKey, onQuestion) {
  return new Promise((resolve, reject) => {
    const client = new Client({
      brokerURL: node.ws,
      connectHeaders: { 'X-Guest-Key': guestKey },
      reconnectDelay: 0,
      debug: () => {},
      onConnect: () => {
        client.subscribe(`/topic/room/${roomCode}`, (m) => {
          const event = JSON.parse(m.body)
          if (event.type === 'QUESTION') onQuestion(event.data, Date.now())
        })
        resolve({ close: () => client.deactivate() })
      },
      onStompError: (f) => reject(new Error('STOMP: ' + f.headers.message)),
      onWebSocketError: () => reject(new Error(`Không nối được tới ${node.ten}`)),
    })
    client.activate()
    setTimeout(() => reject(new Error(`Hết giờ nối tới ${node.ten}`)), 30000)
  })
}

// ---------------------------------------------------------------- kiểm tra instance thứ hai

const health = await fetch('http://localhost:8081/actuator/health').catch(() => null)
if (!health || !health.ok) {
  console.log('Chưa thấy backend thứ hai ở cổng 8081. Khởi động:')
  console.log('  ./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081')
  process.exit(1)
}

// ---------------------------------------------------------------- chuẩn bị phòng trên instance A

const [, reg] = await call(A, 'POST', '/auth/register', {
  email: `hai-instance-${uniq()}@example.com`, password: 'MatKhau@123',
  displayName: 'Host hai instance', role: 'CREATOR',
})
const host = reg.accessToken

const [, quiz] = await call(A, 'POST', '/quizzes',
  { title: `Quiz hai instance ${uniq()}`, visibility: 'PUBLIC' }, host)
const ids = []
for (let i = 0; i < QUESTION_COUNT; i++) {
  const [, q] = await call(A, 'POST', '/questions', {
    type: 'SINGLE_CHOICE', content: `Câu ${i + 1} (${uniq()})`,
    difficulty: 'EASY', points: 1, topic: 'Đo tải',
    options: [{ content: 'Đúng', correct: true }, { content: 'Sai', correct: false }],
  }, host)
  ids.push(q.id)
}
await call(A, 'PUT', `/quizzes/${quiz.id}/questions`, { questionIds: ids }, host)

const [, room] = await call(A, 'POST', '/rooms',
  { quizId: quiz.id, secondsPerQuestion: SECONDS_PER_QUESTION, allowGuests: true }, host)

console.log(`Phòng ${room.roomCode} tạo trên instance ${A.ten}`)
console.log(`Chia ${PLAYERS} người chơi: một nửa nối ${A.ten}, một nửa nối ${B.ten}\n`)

// ---------------------------------------------------------------- host nối instance A

const hostClient = await new Promise((resolve, reject) => {
  const client = new Client({
    brokerURL: A.ws,
    connectHeaders: { Authorization: `Bearer ${host}` },
    reconnectDelay: 0,
    debug: () => {},
    onConnect: () => resolve({
      send: (action) => client.publish({
        destination: `/app/room/${room.roomCode}/${action}`, body: '{}',
      }),
      close: () => client.deactivate(),
    }),
    onStompError: (f) => reject(new Error('STOMP host: ' + f.headers.message)),
  })
  client.activate()
  setTimeout(() => reject(new Error('Host không nối được')), 30000)
})

// ---------------------------------------------------------------- nối người chơi hai bên

const arrivals = { A: [], B: [] }
const players = []

for (let i = 0; i < PLAYERS; i++) {
  const node = i % 2 === 0 ? A : B
  const side = i % 2 === 0 ? 'A' : 'B'

  // Khách vào phòng qua CHÍNH instance mình sẽ nối WebSocket
  const [status, guest] = await call(node, 'POST', `/rooms/${room.roomCode}/join-as-guest`, {
    displayName: `Người chơi ${side}${i + 1}`, avatar: 'FOX',
  })
  if (status !== 200 && status !== 201) {
    console.log(`Khách thứ ${i + 1} không vào được qua ${node.ten}: ${status}`)
    process.exit(1)
  }

  players.push(await connectPlayer(node, room.roomCode, guest.guestKey, (data, at) => {
    arrivals[side].push({ index: data.index, at, serverSentAt: data.deadlineAtMillis - data.timeLimitSec * 1000 })
  }))
}

// ---------------------------------------------------------------- chạy ván

for (let q = 0; q < QUESTION_COUNT; q++) {
  hostClient.send(q === 0 ? 'start' : 'next')
  const deadline = Date.now() + 20000
  const expected = { A: Math.ceil(PLAYERS / 2), B: Math.floor(PLAYERS / 2) }
  while (Date.now() < deadline) {
    const gotA = arrivals.A.filter((x) => x.index === q).length
    const gotB = arrivals.B.filter((x) => x.index === q).length
    if (gotA >= expected.A && gotB >= expected.B) break
    await new Promise((r) => setTimeout(r, 20))
  }
  await new Promise((r) => setTimeout(r, 400))
}

players.forEach((p) => p.close())
hostClient.close()

// ---------------------------------------------------------------- kết quả

const summarise = (side) => {
  const values = arrivals[side].map((x) => x.at - x.serverSentAt)
  return {
    nhan: arrivals[side].length,
    p50: percentile(values, 50),
    p95: percentile(values, 95),
  }
}

const a = summarise('A')
const b = summarise('B')
const expectedEach = { A: Math.ceil(PLAYERS / 2) * QUESTION_COUNT, B: Math.floor(PLAYERS / 2) * QUESTION_COUNT }

console.log('=== Kết quả: sự kiện có vượt được sang instance khác không ===\n')
console.log('| Người chơi nối vào | Sự kiện mong đợi | Nhận được | P50 | P95 |')
console.log('|---|---:|---:|---:|---:|')
console.log(`| ${A.ten} — cùng instance với host | ${expectedEach.A} | ${a.nhan} | ${a.p50} ms | ${a.p95} ms |`)
console.log(`| ${B.ten} — instance KHÁC, chỉ nối qua Redis | ${expectedEach.B} | ${b.nhan} | ${b.p50} ms | ${b.p95} ms |`)

const crossWorks = b.nhan >= expectedEach.B
console.log('')
if (crossWorks) {
  console.log('KẾT LUẬN: người chơi trên instance B nhận đủ sự kiện do instance A phát.')
  console.log('Hai JVM này không có kênh liên lạc nào khác ngoài Redis (PostgreSQL không phải kênh')
  console.log('nhắn tin, broker của Spring nằm trong bộ nhớ từng instance) — nên Redis Pub/Sub là')
  console.log('con đường duy nhất có thể. Bỏ nó ra thì bên B nhận 0 sự kiện: không phải chậm hơn,')
  console.log('mà là phòng đấu nhiều instance không còn hoạt động.')
} else {
  console.log(`KẾT LUẬN: bên B chỉ nhận ${b.nhan}/${expectedEach.B} sự kiện — phát tán liên instance CÓ VẤN ĐỀ.`)
}
