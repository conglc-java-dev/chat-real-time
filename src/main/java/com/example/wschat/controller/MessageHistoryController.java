package com.example.wschat.controller;

import com.example.wschat.model.ChatMessageEntity;
import com.example.wschat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageHistoryController {

    private final ChatMessageRepository chatMessageRepository;

    /**
     * Client gọi API này ngay sau khi kết nối WebSocket để tải lịch sử tin nhắn.
     * Bao gồm cả tin nhắn gửi khi client đang offline.
     *
     * GET /api/messages?roomId=room_1
     */
    @GetMapping
    public List<ChatMessageEntity> getMessages(@RequestParam String roomId) {
        return chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(roomId);
    }
}
