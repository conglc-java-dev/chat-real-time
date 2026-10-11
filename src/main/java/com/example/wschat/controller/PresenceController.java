package com.example.wschat.controller;

import com.example.wschat.listener.WebSocketEventListener;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final WebSocketEventListener eventListener;

    /**
     * Client gọi API này ngay sau khi vừa connect để lấy danh sách
     * user đang online tại thời điểm đó (snapshot tức thời).
     *
     * GET /api/presence/users
     */
    @GetMapping("/users")
    public Map<String, Object> getOnlineUsers() {
        List<String> onlineUsers = eventListener.getOnlineUsers();
        return Map.of(
                "onlineUsers", onlineUsers,
                "totalOnline", onlineUsers.size()
        );
    }
}
