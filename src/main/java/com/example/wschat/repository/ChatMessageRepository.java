package com.example.wschat.repository;

import com.example.wschat.model.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    // Lấy toàn bộ tin nhắn của 1 room, sắp xếp theo thứ tự thời gian tăng dần
    List<ChatMessageEntity> findByRoomIdOrderByCreatedAtAsc(String roomId);
}
