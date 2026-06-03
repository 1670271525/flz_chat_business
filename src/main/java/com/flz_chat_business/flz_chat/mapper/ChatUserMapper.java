package com.flz_chat_business.flz_chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flz_chat_business.flz_chat.pojo.entity.ChatUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ChatUserMapper extends BaseMapper<ChatUser> {

    @Select("SELECT * FROM user WHERE user_deleted=0 AND (user_name=#{account} OR user_email=#{account} OR user_phone=#{account}) LIMIT 1")
    ChatUser findByAccount(@Param("account") String account);

    @Select("SELECT user_id AS userId,user_name AS userName,user_email AS userEmail,user_phone AS userPhone " +
            "FROM user WHERE user_deleted=0 AND (user_name LIKE CONCAT('%',#{keyword},'%') OR user_email LIKE CONCAT('%',#{keyword},'%') OR user_phone=#{keyword}) " +
            "ORDER BY user_id DESC LIMIT #{offset},#{size}")
    List<Map<String, Object>> searchUsers(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(1) FROM user WHERE user_deleted=0 AND (user_name LIKE CONCAT('%',#{keyword},'%') OR user_email LIKE CONCAT('%',#{keyword},'%') OR user_phone=#{keyword})")
    Long countSearchUsers(@Param("keyword") String keyword);
}
