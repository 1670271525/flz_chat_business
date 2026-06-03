package com.flz_chat_business.flz_chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flz_chat_business.flz_chat.pojo.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Select("<script>" +
            "SELECT m.* FROM message m " +
            "WHERE m.conversation_id=#{conversationId} AND m.deleted=0 " +
            "<if test='beforeId != null'> AND m.message_id &lt; #{beforeId} </if>" +
            "AND NOT EXISTS (SELECT 1 FROM message_user_delete d WHERE d.message_id=m.message_id AND d.user_id=#{userId}) " +
            "ORDER BY m.message_id DESC LIMIT #{size}" +
            "</script>")
    List<ChatMessage> listHistory(@Param("conversationId") Long conversationId, @Param("beforeId") Long beforeId, @Param("size") int size, @Param("userId") Long userId);

    @Select("SELECT m.* FROM message m WHERE m.conversation_id=#{conversationId} AND m.message_id>#{lastRead} AND m.deleted=0 " +
            "AND NOT EXISTS (SELECT 1 FROM message_user_delete d WHERE d.message_id=m.message_id AND d.user_id=#{userId}) " +
            "ORDER BY m.message_id ASC LIMIT #{size}")
    List<ChatMessage> listUnread(@Param("conversationId") Long conversationId, @Param("lastRead") Long lastRead, @Param("size") int size, @Param("userId") Long userId);

    @Update("UPDATE conversations SET last_message_id=GREATEST(IFNULL(last_message_id,0),#{messageId}), last_message_at=#{createdAt} WHERE conversation_id=#{conversationId}")
    int updateConversationLastMessage(@Param("conversationId") Long conversationId, @Param("messageId") Long messageId, @Param("createdAt") java.time.LocalDateTime createdAt);

    @Select("SELECT m.* FROM message m JOIN conversation_participants p ON p.conversation_id=m.conversation_id " +
            "WHERE p.user_id=#{userId} AND p.quit=0 AND m.message_id>p.last_read_message_id AND m.deleted=0 ORDER BY m.conversation_id,m.message_id LIMIT #{size}")
    List<Map<String, Object>> listAllUnreadForUser(@Param("userId") Long userId, @Param("size") int size);
}
