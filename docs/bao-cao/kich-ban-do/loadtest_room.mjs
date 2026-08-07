/**
 * Đo tải phòng đấu real-time — số liệu cho báo cáo mục 3.5.
 *
 * ĐO CÁI GÌ: độ trễ từ lúc máy chủ phát một câu hỏi tới lúc từng người chơi nhận được nó, qua
 * đúng đường thật — STOMP over WebSocket, phát tán qua Redis Pub/Sub.
 *
 * ĐO BẰNG CÁCH NÀO: sự kiện QUESTION mang sẵn `deadlineAtMillis` — mốc hết giờ theo đồng hồ máy
 * chủ. Trừ đi thời lượng câu hỏi ra được mốc máy chủ phát đi. So với lúc client nhận là ra độ trễ.
 * Không phải sửa một dòng code nghiệp vụ nào để đo.
 *
 * VÌ SAO KHÔNG DÙNG k6/Gatling như kế hoạch ghi: cả hai không nói được STOMP over SockJS nếu không
 * viết thêm extension, mà thứ cần đo lại chính là đường đó. Client Node dùng đúng thư viện
 * `@stomp/stompjs` mà trình duyệt dùng, nên nó nói đúng giao thức thật thay vì giả lập.
 *
 * GIỚI HẠN CỦA SỐ LIỆU: máy chủ và toàn bộ client chạy trên CÙNG một máy, nên con số này KHÔNG bao
 * gồm độ trễ mạng thật. Nó đo chi phí xử lý của máy chủ và của tầng phát tán, không đo trải nghiệm
 * người dùng ở xa. Phải ghi rõ điều này khi đưa vào báo cáo.
 *
 * Chạy:
 *   node loadtest_room.mjs                 # thang mặc định 10, 30, 50, 100
 *   node loadtest_room.mjs 20 60           # tự chọn số người chơi
 */
import { Client } from '@stomp/stompjs'

const BASE = 'http://localhost:8080/api/v1'
const WS = 'ws://localhost:8080/ws/websocket'

/** Thời lượng mỗi câu — đủ dài để mọi người kịp trả lời trước khi host bấm câu tiếp. */
const SECONDS_PER_QUESTION = 60
const QUESTION_COUNT = 3

/**
 * `--khong-tra-loi`: người chơi chỉ NHẬN câu hỏi, không gửi đáp án.
 * <p>
 * Dùng để tách hai nguồn gây chậm. Nếu bỏ phần trả lời mà độ trễ phát câu hỏi tụt hẳn, thì thứ làm
 * chậm là **kênh xử lý đáp án gửi lên**, không phải việc phát tán xuống. Hai nguyên nhân này cần
 * hai cách chữa khác nhau, nên phải phân biệt trước khi kết luận.
 */
const NO_ANSWER = process.argv.includes('--khong-tra-loi')

const LADDER = process.argv.filter((a) => /^\d+$/.test(a)).map(Number)
const SCALE = LADDER.length > 0 ? LADDER : [10, 30, 50, 100]

// ---------------------------------------------------------------- tiện ích

async function call(method, path, body, token, extraHeaders = {}) {
  const res = await fetch(BASE + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...extraHeaders,
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const text = await res.text()
  let json = null
  try { json = text ? JSON.parse(text) : null } catch { /* không phải JSON */ }
  return [res.status, json]
}

const uniq = () => Math.random().toString(36).slice(2, 8)

/** Phân vị theo kiểu "gần nhất" — đủ chính xác cho cỡ mẫu vài trăm mẫu. */
function percentile(values, p) {
  if (values.length === 0) return null
  const sorted = [...values].sort((a, b) => a - b)
  const index = Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1)
  return sorted[Math.max(0, index)]
}

const summarise = (values) => ({
  n: values.length,
  p50: percentile(values, 50),
  p95: percentile(values, 95),
  max: values.length ? Math.max(...values) : null,
})

// ---------------------------------------------------------------- chuẩn bị

async function register(role) {
  const [, body] = await call('POST', '/auth/register', {
    email: `tai-${role.toLowerCase()}-${uniq()}@example.com`,
    password: 'MatKhau@123', displayName: 'Đo tải', role,
  })
  return body.accessToken
}

async function buildRoom(hostToken) {
  const [, quiz] = await call('POST', '/quizzes',
    { title: `Quiz đo tải ${uniq()}`, visibility: 'PUBLIC' }, hostToken)

  const ids = []
  for (let i = 0; i < QUESTION_COUNT; i++) {
    const [, q] = await call('POST', '/questions', {
      type: 'SINGLE_CHOICE',
      content: `Câu đo tải số ${i + 1} (${uniq()})`,
      difficulty: 'EASY', points: 1, topic: 'Đo tải',
      options: [{ content: 'Đáp án đúng', correct: true }, { content: 'Đáp án sai', correct: false }],
    }, hostToken)
    ids.push(q.id)
  }
  await call('PUT', `/quizzes/${quiz.id}/questions`, { questionIds: ids }, hostToken)

  const [, room] = await call('POST', '/rooms', {
    quizId: quiz.id, secondsPerQuestion: SECONDS_PER_QUESTION, allowGuests: true,
  }, hostToken)
  return room
}

/**
 * Một người chơi: khách vãng lai nối qua STOMP.
 * <p>
 * Dùng khách thay vì tài khoản thật vì đó đúng là kịch bản đông người nhất trong thực tế (quét QR
 * vào phòng), và tránh phải tạo hàng trăm tài khoản chỉ để đo.
 */
function connectPlayer(roomCode, guestKey, onQuestion) {
  return new Promise((resolve, reject) => {
    const client = new Client({
      brokerURL: WS,
      connectHeaders: { 'X-Guest-Key': guestKey },
      reconnectDelay: 0,
      debug: () => {},
      onConnect: () => {
        client.subscribe(`/topic/room/${roomCode}`, (message) => {
          const event = JSON.parse(message.body)
          if (event.type === 'QUESTION') {
            onQuestion(event.data, Date.now())
          }
        })
        resolve({
          answer: (questionId, optionId) => client.publish({
            destination: `/app/room/${roomCode}/answer`,
            body: JSON.stringify({ questionId, optionIds: [optionId] }),
          }),
          close: () => client.deactivate(),
        })
      },
      onStompError: (f) => reject(new Error('STOMP: ' + f.headers.message)),
      onWebSocketError: () => reject(new Error('Lỗi WebSocket')),
    })
    client.activate()
    setTimeout(() => reject(new Error('Không nối được WebSocket sau 30 giây')), 30000)
  })
}

// ---------------------------------------------------------------- một vòng đo

async function runRound(playerCount) {
  const hostToken = await register('CREATOR')
  const room = await buildRoom(hostToken)

  // Mốc thời gian nhận câu hỏi, gom theo chỉ số câu
  const arrivals = new Map()   // index -> [{ at, serverSentAt }]
  const record = (data, at) => {
    const serverSentAt = data.deadlineAtMillis - data.timeLimitSec * 1000
    if (!arrivals.has(data.index)) arrivals.set(data.index, [])
    arrivals.get(data.index).push({ at, serverSentAt, questionId: data.questionId,
      optionId: data.options[0].id })
  }

  // Nối host TRƯỚC người chơi.
  //
  // Lần đo đầu nối host sau cùng và vòng 100 người báo "host không nối được". Nhưng đó là hệ quả
  // của thứ tự, không phải giới hạn máy chủ: 100 client trong CÙNG một tiến trình Node làm vòng lặp
  // sự kiện bận rộn, nên kết nối cuối cùng chờ quá hạn. Nối host trước thì loại được nhầm lẫn đó,
  // và nếu vẫn hỏng thì mới là máy chủ có vấn đề thật.
  const hostClient = await new Promise((resolve, reject) => {
    const client = new Client({
      brokerURL: WS,
      connectHeaders: { Authorization: `Bearer ${hostToken}` },
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

  // Nối từng người một: nối ồ ạt hàng trăm WebSocket cùng lúc thì đo cả sự vụng về của
  // chính công cụ đo, không phải của máy chủ.
  const players = []
  const perPlayerConnectMs = []
  const connectStart = Date.now()
  for (let i = 0; i < playerCount; i++) {
    const [status, guest] = await call('POST', `/rooms/${room.roomCode}/join-as-guest`, {
      displayName: `Người chơi ${i + 1}`, avatar: 'FOX',
    })
    if (status !== 200 && status !== 201) {
      throw new Error(`Khách thứ ${i + 1} không vào được phòng: ${status}`)
    }
    const one = Date.now()
    players.push(await connectPlayer(room.roomCode, guest.guestKey, record))
    perPlayerConnectMs.push(Date.now() - one)
  }
  const connectMs = Date.now() - connectStart

  const latencies = []
  const answerLatencies = []
  let missing = 0

  for (let q = 0; q < QUESTION_COUNT; q++) {
    arrivals.set(q, [])
    hostClient.send(q === 0 ? 'start' : 'next')

    // Chờ đủ người nhận, tối đa 20 giây
    const deadline = Date.now() + 20000
    while (arrivals.get(q).length < playerCount && Date.now() < deadline) {
      await new Promise((r) => setTimeout(r, 20))
    }

    const received = arrivals.get(q)
    missing += playerCount - received.length
    for (const item of received) {
      latencies.push(item.at - item.serverSentAt)
    }

    // Mọi người trả lời cùng lúc — đây là lúc máy chủ chịu tải nặng nhất
    if (received.length > 0 && !NO_ANSWER) {
      const answerStart = Date.now()
      players.forEach((player, i) => {
        const item = received[i] ?? received[0]
        player.answer(item.questionId, item.optionId)
      })
      // Không có sự kiện xác nhận công khai cho từng câu trả lời, nên đo thời gian
      // đẩy hết lệnh đi — phần này chỉ nói về phía client, ghi lại cho đủ
      answerLatencies.push(Date.now() - answerStart)
      await new Promise((r) => setTimeout(r, 600))
    } else if (NO_ANSWER) {
      await new Promise((r) => setTimeout(r, 600))
    }
  }

  players.forEach((p) => p.close())
  hostClient.close()

  return {
    playerCount,
    connectMs,
    // Thời gian nối người CUỐI so với người ĐẦU: cho thấy chi phí tăng theo số kết nối,
    // và phần lớn phần tăng đó là của công cụ đo chứ không phải máy chủ.
    firstConnectMs: perPlayerConnectMs[0],
    lastConnectMs: perPlayerConnectMs[perPlayerConnectMs.length - 1],
    missing,
    broadcast: summarise(latencies),
    answerPushMs: summarise(answerLatencies),
  }
}

// ---------------------------------------------------------------- chạy thang

console.log('Đo tải phòng đấu real-time — mục 3.5')
console.log(`Mỗi vòng: ${QUESTION_COUNT} câu, ${SECONDS_PER_QUESTION}s/câu`)
console.log('LƯU Ý: máy chủ và client chạy cùng một máy — số liệu KHÔNG gồm độ trễ mạng thật.\n')

const results = []
for (const count of SCALE) {
  process.stdout.write(`  ${String(count).padStart(4)} người chơi … `)
  try {
    const result = await runRound(count)
    results.push(result)
    console.log(
      `nối ${result.connectMs}ms (đầu ${result.firstConnectMs}ms → cuối ${result.lastConnectMs}ms) · ` +
      `phát câu hỏi P50 ${result.broadcast.p50}ms ` +
      `P95 ${result.broadcast.p95}ms max ${result.broadcast.max}ms · ` +
      `mất ${result.missing} sự kiện`)
  } catch (e) {
    console.log(`HỎNG: ${e.message}`)
    results.push({ playerCount: count, error: e.message })
    break
  }
}

console.log('\n=== Bảng đưa vào báo cáo (mục 3.5) ===\n')
console.log('| Người chơi | Nối phòng | Phát câu hỏi P50 | P95 | Max | Sự kiện mất |')
console.log('|---:|---:|---:|---:|---:|---:|')
for (const r of results) {
  if (r.error) {
    console.log(`| ${r.playerCount} | — | — | — | — | HỎNG: ${r.error} |`)
    continue
  }
  console.log(`| ${r.playerCount} | ${r.connectMs} ms | ${r.broadcast.p50} ms | ` +
    `${r.broadcast.p95} ms | ${r.broadcast.max} ms | ${r.missing} |`)
}
console.log('\nĐiều kiện đo: một máy đơn, máy chủ và client cùng máy, không qua mạng.')
