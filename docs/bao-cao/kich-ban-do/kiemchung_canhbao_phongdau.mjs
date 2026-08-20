/**
 * Chay that canh bao live trong phong dau tren server dang chay.
 *
 * Dung @stomp/stompjs + SockJS giong y trinh duyet — khong gia lap giao thuc.
 */
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const BASE = 'http://localhost:8080/api/v1'
const WS = 'http://localhost:8080/ws'
const ok = []

const rand = (n = 5) => Math.random().toString(36).slice(2, 2 + n)

async function call(method, path, body, token, expect) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = 'Bearer ' + token
  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const text = await res.text()
  if (expect !== undefined && res.status !== expect) {
    console.error(`FAIL ${method} ${path} -> ${res.status} (mong ${expect})\n${text.slice(0, 400)}`)
    process.exit(1)
  }
  let json = null
  try { json = text ? JSON.parse(text) : null } catch { json = text }
  return { status: res.status, body: json }
}

async function dangKy(role, ten) {
  const email = `proctor-${role.toLowerCase()}-${rand()}@example.com`
  const r = await call('POST', '/auth/register',
    { email, password: 'Matkhau@123', displayName: ten, role }, null, 201)
  return { token: r.body.accessToken, id: r.body.user.id, email }
}

/** Noi STOMP, tra ve { send, doi, dong } */
function noi(roomCode, { token, guestKey }) {
  return new Promise((resolve, reject) => {
    const suKien = []
    const rieng = []
    const client = new Client({
      webSocketFactory: () => new SockJS(WS),
      connectHeaders: guestKey
        ? { 'X-Guest-Key': guestKey }
        : { Authorization: 'Bearer ' + token },
      reconnectDelay: 0,
      debug: () => {},
      onConnect: () => {
        client.subscribe(`/topic/room/${roomCode}`, (m) => suKien.push(JSON.parse(m.body)))
        client.subscribe(`/user/queue/room/${roomCode}`, (m) => rieng.push(JSON.parse(m.body)))
        resolve({
          send: (action, payload) => client.publish({
            destination: `/app/room/${roomCode}/${action}`,
            body: JSON.stringify(payload ?? {}),
          }),
          /** Cho su kien dung loai o hang doi rieng */
          doiRieng: (type, ms = 8000) => cho(rieng, type, ms),
          doiChung: (type, ms = 8000) => cho(suKien, type, ms),
          coTrenKenhChung: (type) => suKien.some((e) => e.type === type),
          dong: () => client.deactivate(),
        })
      },
      onStompError: (f) => reject(new Error('STOMP error: ' + f.body)),
      onWebSocketError: (e) => reject(new Error('WS error: ' + e.message)),
    })
    client.activate()
  })
}

async function cho(hangDoi, type, ms) {
  const han = Date.now() + ms
  while (Date.now() < han) {
    const i = hangDoi.findIndex((e) => e.type === type)
    if (i >= 0) return hangDoi.splice(i, 1)[0].data
    await new Promise((r) => setTimeout(r, 50))
  }
  throw new Error(`Khong nhan duoc su kien ${type} sau ${ms}ms`)
}

/** Doc tong ket toi khi dat dung so lieu — diem dong bo dang tin duy nhat. */
async function choGhiXong(roomCode, token, soLan, soCau) {
  const han = Date.now() + 8000
  let cuoi = '[]'
  while (Date.now() < han) {
    const r = await call('GET', `/rooms/${roomCode}/proctoring`, undefined, token, 200)
    cuoi = JSON.stringify(r.body)
    const d = r.body[0]
    if (d && d.soLanRoiTrang === soLan && d.soCauLap === soCau) return r.body
    await new Promise((r2) => setTimeout(r2, 100))
  }
  throw new Error(`Tin hieu chua ghi du (mong ${soLan}/${soCau}). Dang la: ${cuoi}`)
}

async function main() {
  const gv = await dangKy('CREATOR', 'Cô Nguyễn Thị Lan')
  const hs = await dangKy('LEARNER', 'Trần Minh Quân')

  // --- Quiz 3 cau
  const quiz = (await call('POST', '/quizzes',
    { title: 'Quiz phòng đấu ' + rand(), difficulty: 'EASY', visibility: 'PUBLIC' },
    gv.token, 201)).body
  const ids = []
  for (let i = 0; i < 3; i++) {
    const q = (await call('POST', '/questions', {
      content: `Câu ${i + 1}: 2 + ${i} = ?`, type: 'SINGLE_CHOICE', points: 1,
      difficulty: 'EASY', explanation: 'Cộng thôi',
      options: [{ content: String(2 + i), correct: true }, { content: '99', correct: false }],
    }, gv.token, 201)).body
    ids.push(q.id)
  }
  await call('PUT', `/quizzes/${quiz.id}/questions`, { questionIds: ids }, gv.token, 200)

  // --- Mo phong cho phep khach
  const phong = (await call('POST', '/rooms',
    { quizId: quiz.id, secondsPerQuestion: 60, allowGuests: true }, gv.token, 201)).body
  const ma = phong.roomCode
  ok.push(`mo phong ma ${ma}, cho phep khach`)

  await call('POST', `/rooms/${ma}/join`, undefined, hs.token, 200)

  // --- Khach vao bang ma PIN
  const khach = (await call('POST', `/rooms/${ma}/join-as-guest`,
    { displayName: 'Khách quét QR' }, null, 200)).body
  ok.push(`hoc sinh + khach vang lai da vao phong`)

  const host = await noi(ma, { token: gv.token })
  const player = await noi(ma, { token: hs.token })
  const guest = await noi(ma, { guestKey: khach.guestKey })
  await new Promise((r) => setTimeout(r, 400))

  host.send('start')
  await host.doiChung('QUESTION')
  await player.doiChung('QUESTION')
  ok.push('van bat dau, moi nguoi nhan cau 1')

  // === Cau 0: roi roi ve — MOT cau, chua du khuon
  player.send('proctoring', { type: 'TAB_HIDDEN' })
  await choGhiXong(ma, gv.token, 1, 0)
  player.send('proctoring', { type: 'TAB_VISIBLE' })
  const sauCau0 = await choGhiXong(ma, gv.token, 1, 1)
  if (sauCau0[0].biGanCo) throw new Error('MOT cau da gan co — sai nguong')
  ok.push(`cau 1: roi roi ve -> soCauLap=1, biGanCo=false (mot lan la ngau nhien)`)

  // === Cau 1: lap lai -> du khuon
  host.send('next')
  await player.doiChung('QUESTION')
  player.send('proctoring', { type: 'TAB_HIDDEN' })
  await choGhiXong(ma, gv.token, 2, 1)
  player.send('proctoring', { type: 'TAB_VISIBLE' })

  const co = await host.doiRieng('PROCTORING_FLAG')
  if (co.playerId !== hs.id) throw new Error('co ve sai nguoi: ' + co.playerId)
  ok.push(`cau 2: lap lai -> HOST nhan co do: "${co.lyDo}" (soCauLap=${co.soCauLap})`)

  // === Co KHONG len kenh phat chung
  if (player.coTrenKenhChung('PROCTORING_FLAG')) {
    throw new Error('CO LEN KENH CHUNG — nguoi bi nghi thay ten minh, loi bao mat')
  }
  if (host.coTrenKenhChung('PROCTORING_FLAG')) {
    throw new Error('CO LEN KENH CHUNG')
  }
  ok.push('co KHONG len /topic/room/{ma} — nguoi bi nghi va ca phong khong thay gi')

  // === Host nhac rieng
  host.send('warn', { playerId: hs.id })
  const nhac = await player.doiRieng('PROCTORING_WARNING')
  if (/gian lận/i.test(nhac.message)) throw new Error('loi nhac buoc toi: ' + nhac.message)
  ok.push(`hoc sinh nhan loi nhac: "${nhac.message.slice(0, 60)}..."`)
  if (player.coTrenKenhChung('PROCTORING_WARNING')) throw new Error('loi nhac len kenh chung')
  ok.push('loi nhac cung KHONG len kenh chung')

  // === Nguoi choi thuong bam nhac -> bi tu choi
  const loi = []
  await new Promise((resolve) => {
    const c = new Client({
      webSocketFactory: () => new SockJS(WS),
      connectHeaders: { Authorization: 'Bearer ' + hs.token },
      reconnectDelay: 0, debug: () => {},
      onConnect: () => {
        c.subscribe('/user/queue/errors', (m) => loi.push(JSON.parse(m.body)))
        setTimeout(() => {
          c.publish({
            destination: `/app/room/${ma}/warn`,
            body: JSON.stringify({ playerId: gv.id }),
          })
          setTimeout(() => { c.deactivate(); resolve() }, 1500)
        }, 300)
      },
    })
    c.activate()
  })
  if (loi.length === 0 || loi[0].status !== 403) {
    throw new Error('nguoi choi thuong nhac duoc, phai 403. Nhan: ' + JSON.stringify(loi))
  }
  ok.push('nguoi choi thuong bam nhac -> 403')

  // === Khach vang lai cung duoc ghi va nhac duoc
  guest.send('proctoring', { type: 'TAB_HIDDEN' })
  guest.send('proctoring', { type: 'TAB_VISIBLE' })
  await new Promise((r) => setTimeout(r, 1200))
  guest.send('warn', { playerId: hs.id })   // khach bam nhac -> phai bi chan
  host.send('warn', { playerId: khach.playerId })
  const nhacKhach = await guest.doiRieng('PROCTORING_WARNING')
  if (!nhacKhach.message) throw new Error('khach khong nhan duoc loi nhac')
  ok.push('khach vang lai (khong co JWT) VAN nhan duoc loi nhac')

  // === Host tu chuyen tab -> khong sinh gi
  const truoc = (await call('GET', `/rooms/${ma}/proctoring`, undefined, gv.token, 200)).body.length
  host.send('proctoring', { type: 'TAB_HIDDEN' })
  host.send('proctoring', { type: 'TAB_VISIBLE' })
  await new Promise((r) => setTimeout(r, 1500))
  const sau = (await call('GET', `/rooms/${ma}/proctoring`, undefined, gv.token, 200)).body
  if (sau.some((d) => d.playerId === gv.id)) throw new Error('tin hieu cua host bi ghi')
  ok.push(`host tu chuyen tab -> khong ghi dong nao (${truoc} -> ${sau.length} nguoi)`)

  // === Chi host xem duoc tong ket
  const hsXem = await call('GET', `/rooms/${ma}/proctoring`, undefined, hs.token)
  if (hsXem.status !== 403) throw new Error('hoc sinh xem duoc tong ket, nhan ' + hsXem.status)
  ok.push('hoc sinh doc tong ket -> 403')

  const khachXem = await call('GET', `/rooms/${ma}/proctoring`, undefined, null)
  if (khachXem.status === 200) throw new Error('nguoi chua dang nhap xem duoc tong ket')
  ok.push(`nguoi chua dang nhap doc tong ket -> ${khachXem.status}`)

  console.log('\n--- BANG TONG KET HOST THAY ---')
  for (const d of sau) {
    console.log(`  ${(d.displayName ?? '?').padEnd(20)} ${d.guest ? '[khach]' : '[thanh vien]'} ` +
      `soCauLap=${d.soCauLap} roiTrang=${d.soLanRoiTrang} ${d.biGanCo ? '<-- GAN CO' : ''}`)
  }

  host.dong(); player.dong(); guest.dong()

  console.log('\n--- KET QUA ---')
  ok.forEach((m, i) => console.log(`${String(i + 1).padStart(2)}. OK  ${m}`))
  console.log(`\nGIAO VIEN (host): ${gv.email} / Matkhau@123`)
  console.log(`HOC SINH        : ${hs.email} / Matkhau@123`)
  console.log(`PHONG           : http://localhost:5173/rooms/${ma}`)
}

main().catch((e) => { console.error('\nFAIL:', e.message); process.exit(1) })
