package com.flz_chat_business.flz_chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flz_chat_business.flz_chat.pojo.entity.FriendRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface FriendRequestMapper extends BaseMapper<FriendRequest> {

    @Select("SELECT fr.*,ui.nickname AS fromNickname,ui.avatar_url AS fromAvatar " +
            "FROM friend_requests fr LEFT JOIN user_information ui ON ui.user_id=fr.from_user_id " +
            "WHERE fr.to_user_id=#{userId} AND (#{status} IS NULL OR fr.status=#{status}) ORDER BY fr.created_at DESC LIMIT #{offset},#{size}")
    List<Map<String, Object>> listIncoming(@Param("userId") Long userId, @Param("status") Integer status, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(1) FROM friend_requests WHERE to_user_id=#{userId} AND (#{status} IS NULL OR status=#{status})")
    Long countIncoming(@Param("userId") Long userId, @Param("status") Integer status);

    @Select("SELECT fr.*,ui.nickname AS toNickname,ui.avatar_url AS toAvatar " +
            "FROM friend_requests fr LEFT JOIN user_information ui ON ui.user_id=fr.to_user_id " +
            "WHERE fr.from_user_id=#{userId} AND (#{status} IS NULL OR fr.status=#{status}) ORDER BY fr.created_at DESC LIMIT #{offset},#{size}")
    List<Map<String, Object>> listOutgoing(@Param("userId") Long userId, @Param("status") Integer status, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(1) FROM friend_requests WHERE from_user_id=#{userId} AND (#{status} IS NULL OR status=#{status})")
    Long countOutgoing(@Param("userId") Long userId, @Param("status") Integer status);

    @Select("SELECT COUNT(1) FROM friend_requests WHERE from_user_id=#{fromUserId} AND to_user_id=#{toUserId} AND status=0 AND created_at>=#{since}")
    Long countPendingWithin(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId, @Param("since") LocalDateTime since);
}
