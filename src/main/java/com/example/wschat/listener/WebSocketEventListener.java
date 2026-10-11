package com.example.wschat.listener;

import com.example.wschat.model.PresencePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lắng nghe vòng đời kết nối STOMP WebSocket để quản lý trạng thái Online/Offline.
 *
 * Cấu trúc lưu trữ: ConcurrentHashMap<username, Set<sessionId>>
 *   - Key   : username (String)
 *   - Value : tập hợp các sessionId đang mở của user đó
 *
 * Lý do dùng Set<sessionId> thay vì chỉ đếm số:
 *   Một user có thể mở nhiều tab hoặc thiết bị cùng lúc.
 *   User chỉ thực sự OFFLINE khi Set rỗng (không còn bất kỳ session nào).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    // Thread-safe map: username -> danh sách sessionId đang active
    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();

    /**
     * Kích hoạt khi Client connect websocket thành công với Server.
     * Viết các logic xử lý khi user connect thành công( thêm session socket, đếm số user đang onl )
     */
    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor ackAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = ackAccessor.getSessionId();

        // Lấy bản tin CONNECT gốc từ bên trong CONNECT_ACK
        Message<?> connectMessage =
                (Message<?>) ackAccessor.getHeader("simpConnectMessage");

        if (connectMessage == null) {
            log.warn("[Presence] Không tìm thấy simpConnectMessage trong CONNECT_ACK, session={}", sessionId);
            return;
        }

        StompHeaderAccessor connectAccessor = StompHeaderAccessor.wrap(connectMessage);
        Map<String, Object> sessionAttributes = connectAccessor.getSessionAttributes();

        if (sessionAttributes == null) {
            log.warn("[Presence] SessionAttributes null cho session={}", sessionId);
            return;
        }

        String username = (String) sessionAttributes.get("username");
        if (username == null || username.isBlank()) {
            log.warn("[Presence] Session {} connect không có header username — bỏ qua", sessionId);
            return;
        }

        // Thêm sessionId vào tập hợp session của user
        userSessions.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        log.info("[Presence] USER_JOINED: {} (session: {}) | Tổng online: {}", username, sessionId, userSessions.size());

        broadcastPresence("USER_JOINED", username);
    }

    /**
     * Kích hoạt khi kết nối của một session bị đóng — dù tắt tab, mất mạng, hay ngắt socket.
     * Nếu user không còn session nào → User chính thức OFFLINE.
     */
    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            return;
        }

        String username = (String) sessionAttributes.get("username");
        if (username == null || username.isBlank()) {
            return;
        }

        Set<String> sessions = userSessions.get(username);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                // Không còn session nào đang mở → OFFLINE
                userSessions.remove(username);
                log.info("[Presence] USER_LEFT: {} | Tổng online: {}", username, userSessions.size());
                broadcastPresence("USER_LEFT", username);
            } else {
                log.info("[Presence] Session {} của {} đóng, còn {} session khác", sessionId, username, sessions.size());
            }
        }
    }

    /**
     * Trả về danh sách username đang online (để REST API /api/presence/users dùng).
     */
    public List<String> getOnlineUsers() {
        return Collections.unmodifiableList(new ArrayList<>(userSessions.keySet()));
    }

    /**
     * Phát bản tin Presence ra kênh /topic/presence.
     * Toàn bộ Client đang subscribe kênh này sẽ nhận được và cập nhật giao diện.
     */
    private void broadcastPresence(String type, String username) {
        // khi có user connect thành công -> broadcastPresence phát event cho client đang lắng nghe để tự động update số lương user đang onl

        List<String> onlineUsers = getOnlineUsers();

        PresencePayload payload = PresencePayload.builder()
                .type(type)
                .username(username)
                .onlineUsers(onlineUsers)
                .totalOnline(onlineUsers.size())
                .build();

        messagingTemplate.convertAndSend("/topic/presence", payload);   // hien tai, fe dang config tat ca nguoi dung sub kenh nay deu nhan duoc thong bao 
    }

    /**
     * [THAM KHẢO THỰC TẾ]: Chỉ phát bản tin Presence đến danh sách BẠN BÈ đang online.
     * 
     * Khác biệt so với broadcastPresence() ở trên:
     * 1. Kênh gửi:
     *    - broadcast: gửi vào kênh public "/topic/presence" -> ai subscribe cũng nhận được.
     *    - bạn bè: gửi vào kênh cá nhân "/queue/friends-presence" của từng user cụ thể.
     * 
     * 2. Phía Frontend của bạn bè sẽ subscribe:
     *    stompClient.subscribe("/user/queue/friends-presence", callback);
     *    -> Spring tự động ánh xạ ngầm session của người nhận để chỉ đúng người đó nhận được,
     *       đảm bảo tính riêng tư (người lạ không thấy trạng thái online).
     *
     * @param type           Loại sự kiện: "ONLINE" hoặc "OFFLINE"
     * @param username       Người vừa đổi trạng thái (vừa online hoặc vừa offline)
     * @param friendUsernames Danh sách username những người bạn (thường lấy từ DB/Service bạn bè)
     */
    private void broadcastPresenceToFriends(String type, String username, List<String> friendUsernames) {
        if (friendUsernames == null || friendUsernames.isEmpty()) {
            return;
        }

        PresencePayload payload = PresencePayload.builder()
                .type(type)
                .username(username)
                .build();

        // Chỉ gửi cho những người bạn ĐANG ONLINE thực tế
        for (String friend : friendUsernames) {
            if (userSessions.containsKey(friend)) { // check xem ban be co online ko
                // Client của friend đang nghe: /user/queue/friends-presence
                // mac du client chi sub tren kenh friends-prsence , khong truyen username nhung backend se tu bien doi thanh duong dan /queue/friends-presence-userabc-123
                messagingTemplate.convertAndSendToUser(
                        friend,
                        "/queue/friends-presence",
                        payload
                );
            }
        }
    }
}
