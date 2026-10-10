# WebSocket Chat Demo

Demo chat real-time tối giản bằng Spring Boot, STOMP, SockJS và Vanilla JavaScript.
Project chỉ triển khai **happy path** để bạn có một nền tảng chạy được và tự nghiên cứu
các phần production sau đó.

## Chạy project

Yêu cầu: Java 21.

```bash
./mvnw spring-boot:run
```

Mở `http://localhost:8080` ở hai tab:

1. Tab thứ nhất: tên `Alice`, room `room_1`, bấm **Kết nối**.
2. Tab thứ hai: tên `Bob`, room `room_1`, bấm **Kết nối**.
3. Gửi tin nhắn từ một tab và quan sát tab còn lại.

Bạn cũng có thể mở `index.html` bằng Live Server ở port `5500`. Frontend được cấu
hình kết nối về backend Spring Boot tại `http://localhost:8080/ws`, vì vậy backend
vẫn phải được chạy trước.

Frontend tải SockJS và STOMP từ CDN, vì vậy trình duyệt cần có Internet khi mở trang.

## Luồng cơ bản

```text
Browser -- connect --> /ws
Browser -- send ----> /app/chat.send
Server  -- publish -> /topic/room/{roomId}
Browser -- subscribe  /topic/room/{roomId}
```

## Cố ý chưa làm — bài tập tự nghiên cứu

- Authentication/JWT và phân quyền vào phòng.
- Lưu tin nhắn, tin nhắn offline và phân trang lịch sử.
- Chạy nhiều server qua Redis/RabbitMQ.
- Heartbeat và tự động reconnect.
- API tạo/xóa/mời thành viên vào phòng.
- Online/offline presence.
- Trạng thái đã gửi/đã đọc.
- Rate limiting chống spam.
- Giới hạn CORS/origin cho môi trường production.
- Validation và xử lý lỗi đầy đủ.

> Lưu ý: `roomId`, `sender` và nội dung hiện được client gửi thẳng lên server. Đây là chủ
> ý của demo học tập, không phải thiết kế an toàn để deploy production.
