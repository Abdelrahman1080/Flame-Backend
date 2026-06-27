package com.Flame.backend.DAO.chat;

import com.Flame.backend.entities.chat.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {

    // جيب كل الرسايل بين يوزرين (الـ conversation)
    @Query("""
        SELECT m FROM ChatMessage m
        WHERE (m.sender.id = :userId1 AND m.receiver.id = :userId2)
           OR (m.sender.id = :userId2 AND m.receiver.id = :userId1)
        ORDER BY m.sentAt ASC
    """)
    List<ChatMessage> findConversation(@Param("userId1") Integer userId1,
                                       @Param("userId2") Integer userId2);

    // جيب آخر رسالة مع كل شخص (للـ inbox)
    @Query("""
        SELECT m FROM ChatMessage m
        WHERE m.id IN (
            SELECT MAX(m2.id) FROM ChatMessage m2
            WHERE m2.sender.id = :userId OR m2.receiver.id = :userId
            GROUP BY CASE
                WHEN m2.sender.id = :userId THEN m2.receiver.id
                ELSE m2.sender.id
            END
        )
        ORDER BY m.sentAt DESC
    """)
    List<ChatMessage> findInbox(@Param("userId") Integer userId);

    // عدد الرسايل الغير مقروءة
    long countByReceiverIdAndIsReadFalse(Integer receiverId);
}