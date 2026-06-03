package com.flz_chat_business.flz_chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flz_chat_business.flz_chat.pojo.entity.ConversationParticipant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ConversationParticipantMapper extends BaseMapper<ConversationParticipant> {

    @Select("SELECT * FROM conversation_participants WHERE conversation_id=#{conversationId} AND user_id=#{userId} LIMIT 1")
    ConversationParticipant findOne(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Select("SELECT user_id FROM conversation_participants WHERE conversation_id=#{conversationId} AND quit=0")
    List<Long> listActiveUserIds(@Param("conversationId") Long conversationId);

    @Select("SELECT cp.*,ui.nickname,ui.avatar_url FROM conversation_participants cp " +
            "LEFT JOIN user_information ui ON ui.user_id=cp.user_id " +
            "WHERE cp.conversation_id=#{conversationId} AND cp.quit=0 ORDER BY cp.role ASC,cp.user_id ASC LIMIT #{offset},#{size}")
    List<Map<String, Object>> listMembers(@Param("conversationId") Long conversationId, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(1) FROM conversation_participants WHERE conversation_id=#{conversationId} AND quit=0")
    Long countMembers(@Param("conversationId") Long conversationId);
}
