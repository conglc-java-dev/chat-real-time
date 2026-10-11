package com.example.wschat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Payload phát ra kênh /topic/presence khi có user join hoặc leave.
 * Client lắng nghe kênh này để cập nhật danh sách online và badge tổng số.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresencePayload {

    /** USER_JOINED hoặc USER_LEFT */
    private String type;

    /** Username vừa join hoặc vừa leave */
    private String username;

    /** Danh sách tất cả username đang online tại thời điểm này */
    private List<String> onlineUsers;

    /** Tổng số user đang online */
    private int totalOnline;
}
