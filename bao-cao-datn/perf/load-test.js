/**
 * k6 load test cho EduExam — đo thời gian phản hồi API (CRUD) và khả năng chịu tải.
 * Đối chiếu NFR Bảng 1.2: p95 < 500ms, 50 người dùng đồng thời.
 *
 * Cài k6: https://k6.io/docs/get-started/installation/  (Windows: `winget install k6`)
 * Chạy:
 *   k6 run -e EMAIL=gv.demo@eduexam.local -e PASSWORD=Demo@12345 bao-cao/perf/load-test.js
 *
 * Kết quả: xem dòng `http_req_duration ... p(95)=...` và checks pass.
 * Điền các số này vào mục 3.4.2 (đánh giá hiệu năng) của báo cáo.
 */
import http from "k6/http";
import { check, sleep } from "k6";
import { Trend } from "k6/metrics";

const API = __ENV.API || "http://localhost:8080";
const EMAIL = __ENV.EMAIL || "gv.demo@eduexam.local";
const PASSWORD = __ENV.PASSWORD || "Demo@12345";

const listCourses = new Trend("t_list_courses", true);
const listQuestions = new Trend("t_list_questions", true);

export const options = {
  scenarios: {
    crud_load: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "20s", target: 50 }, // tăng dần tới 50 VU
        { duration: "40s", target: 50 }, // giữ tải 50 VU
        { duration: "10s", target: 0 },  // hạ tải
      ],
    },
  },
  thresholds: {
    http_req_duration: ["p(95)<500"], // NFR: p95 < 500ms
    http_req_failed: ["rate<0.01"],   // < 1% lỗi
  },
};

export function setup() {
  const res = http.post(`${API}/api/auth/login`, JSON.stringify({ email: EMAIL, password: PASSWORD }), {
    headers: { "Content-Type": "application/json" },
  });
  check(res, { "login 200": (r) => r.status === 200 });
  const token = res.json("accessToken");
  // lấy 1 courseId để test endpoint con
  const courses = http.get(`${API}/api/courses`, { headers: { Authorization: `Bearer ${token}` } });
  let courseId = null;
  try {
    const body = courses.json();
    const arr = Array.isArray(body) ? body : (body.content || []);
    courseId = arr.length ? arr[0].id : null;
  } catch (_) {}
  return { token, courseId };
}

export default function (data) {
  const headers = { Authorization: `Bearer ${data.token}` };

  const r1 = http.get(`${API}/api/courses`, { headers });
  listCourses.add(r1.timings.duration);
  check(r1, { "GET /courses 200": (r) => r.status === 200 });

  if (data.courseId) {
    const r2 = http.get(`${API}/api/courses/${data.courseId}/questions`, { headers });
    listQuestions.add(r2.timings.duration);
    check(r2, { "GET /questions 200": (r) => r.status === 200 });
  }

  sleep(1);
}
