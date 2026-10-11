package com.example.wschat.controller;

import com.example.wschat.model.ChatMessage;
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

    @MessageMapping("/chat.send")  // tin nhan gui di se duoc chay vao day
    public void sendMessage(ChatMessage message) {
        message.setTimestamp(LocalTime.now().format(TIME_FORMATTER));

        messagingTemplate.convertAndSend(   // gui message vao broker da duoc enable o file config
                "/topic/room/" + message.getRoomId(),
                message
        );
    }
}
