package com.flz_chat_business.flz_chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flz_chat_business.flz_chat.pojo.entity.Friendship;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface FriendshipMapper extends BaseMapper<Friendship> {

    @Select("SELECT f.friend_id AS userId,f.alias AS alias,ui.nickname AS nickname,ui.avatar_url AS avatarUrl,ui.mood AS mood,ui.signature AS signature " +
            "FROM friendships f LEFT JOIN user_information ui ON ui.user_id=f.friend_id " +
            "WHERE f.user_id=#{userId} AND f.status=1 ORDER BY f.updated_at DESC LIMIT #{offset},#{size}")
    List<Map<String, Object>> listFriends(@Param("userId") Long userId, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(1) FROM friendships WHERE user_id=#{userId} AND status=1")
    Long countFriends(@Param("userId") Long userId);
}
