package com.example.wschat.controller;

import com.example.wschat.model.ChatMessage;
import com.example.wschat.model.ChatMessageEntity;
import com.example.wschat.repository.ChatMessageRepository;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;

    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessage message) {
        String timestamp = LocalTime.now().format(TIME_FORMATTER);
        message.setTimestamp(timestamp);

        // 1. LUÔN LƯU VÀO DB TRƯỚC (để không bao giờ mất tin nhắn dù receiver offline)
        ChatMessageEntity entity = ChatMessageEntity.builder()
                .sender(message.getSender())
                .content(message.getContent())
                .roomId(message.getRoomId())
                .timestamp(timestamp)
                .createdAt(LocalDateTime.now())
                .build();
        chatMessageRepository.save(entity);

        // 2. Broadcast qua WebSocket cho những ai đang subscribe room này
        messagingTemplate.convertAndSend(
                "/topic/room/" + message.getRoomId(),
                message
        );
    }
}
