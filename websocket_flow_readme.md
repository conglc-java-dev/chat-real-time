# Kiến Trúc & Luồng Dữ Liệu WebSocket (Real-Time Chat)

Tài liệu chi tiết mô tả luồng hoạt động giữa **Frontend** (SockJS + STOMP) và **Backend** (Spring Boot WebSocket) trong dự án Chat Real-Time.


### Nguyên tắc phân làn:
* **`/ws` (Cổng bắt tay):** Nơi khởi tạo kết nối Socket vật lý ban đầu (Handshake Upgrade từ HTTP sang WebSocket).
* **`/app` (Làn gửi vào - Client -> Server):** Mọi tin nhắn Client gửi có tiền tố này sẽ được Spring chuyển vào **Controller** (`@MessageMapping`) để chạy code logic Java.
* **`/topic` (Làn phát ra - Server -> Client):** Kênh phát thanh do **SimpleBroker** (trong RAM) quản lý. Bất kỳ ai `subscribe` kênh này sẽ nhận được tin nhắn khi Server phát loa.

---

## 2. Toàn Bộ Vòng Đời & Luồng Dữ Liệu (End-to-End Workflow)

```
[ FRONTEND (Client) ]                                              [ BACKEND (Spring Boot) ]
  index.html (SockJS + STOMP)                                       Tomcat / Broker / Controller
        │                                                                       │
        ├──────────────── GIAI ĐOẠN 1: BẮT TAY (TẠO SOCKET VẬT LÝ) ──────────────┤
        │                                                                       │
        │── (1) HTTP GET /ws (Header: Upgrade: websocket) ─────────────────────►│ Endpoint: /ws
        │◄── (2) HTTP 101 Switching Protocols (Giữ ống TCP mở) ─────────────────┤
        │── (3) Gửi bản tin STOMP "CONNECT" ───────────────────────────────────►│
        │◄── (4) Trả về bản tin STOMP "CONNECTED" ──────────────────────────────┤ (Đường ống Socket đã sẵn sàng)
        │                                                                       │
        ├──────────────── GIAI ĐOẠN 2: LẮNG NGHE (SUBSCRIBE LOGIC) ─────────────┤
        │                                                                       │
        │── (5) SUBSCRIBE /topic/room/room_1 ──────────────────────────────────►│ SimpleBroker:
        │       (Client A & B cùng mở tai nghe qua socket có sẵn)               │ Ghi danh Session A, B
        │                                                                       │ vào danh bạ RAM của room_1 ( Spring sẽ làm tự động,
        │                                                                       │ không cần code logic gì thêm )
        │                                                                       │
        ├──────────────── GIAI ĐOẠN 3: GỬI & NHẬN TIN (SEND & PUSH) ───────────┤
        │                                                                       │
        │── (6) SEND /app/chat.send ───────────────────────────────────────────►│ ChatController:
        │       Payload: { sender, content, roomId }                            │ @MessageMapping("/chat.send")
        │                                                                       │       │
        │                                                                       │ [Java xử lý logic, gán giờ...]
        │                                                                       │       │
        │                                                                       │ messagingTemplate.convertAndSend(
        │                                                                       │    "/topic/room/room_1", msg)
        │                                                                       │       ▼
        │◄═════ (7) PUSH /topic/room/room_1 ════════════════════════════════════╪ SimpleBroker:
        │       Payload: { sender, content, roomId, timestamp }                 │ Phát loa đồng thời xuống
        │                                                                       │ tất cả session trong room_1
        ▼                                                                       │
[Nhận payload -> render ra HTML]
```
