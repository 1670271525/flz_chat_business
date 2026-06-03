package com.flz_chat_business.flz_chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flz_chat_business.flz_chat.pojo.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    @Select("SELECT c.*,cp.pinned,cp.mute,cp.last_read_message_id " +
            "FROM conversations c JOIN conversation_participants cp ON cp.conversation_id=c.conversation_id " +
            "WHERE cp.user_id=#{userId} AND cp.quit=0 AND c.dissolved=0 " +
            "ORDER BY cp.pinned DESC,c.last_message_at DESC,c.conversation_id DESC LIMIT #{offset},#{size}")
    List<Map<String, Object>> listMyConversations(@Param("userId") Long userId, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(1) FROM conversations c JOIN conversation_participants cp ON cp.conversation_id=c.conversation_id " +
            "WHERE cp.user_id=#{userId} AND cp.quit=0 AND c.dissolved=0")
    Long countMyConversations(@Param("userId") Long userId);

    @Select("SELECT c.conversation_id FROM conversations c " +
            "JOIN conversation_participants p1 ON p1.conversation_id=c.conversation_id AND p1.user_id=#{userA} AND p1.quit=0 " +
            "JOIN conversation_participants p2 ON p2.conversation_id=c.conversation_id AND p2.user_id=#{userB} AND p2.quit=0 " +
            "WHERE c.type=1 AND c.dissolved=0 LIMIT 1")
    Long findSingleConversationId(@Param("userA") Long userA, @Param("userB") Long userB);
}
