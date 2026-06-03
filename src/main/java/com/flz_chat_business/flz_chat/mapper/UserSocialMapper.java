package com.flz_chat_business.flz_chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flz_chat_business.flz_chat.pojo.entity.UserSocial;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserSocialMapper extends BaseMapper<UserSocial> {

    @Select("SELECT us.*,ui.nickname,ui.avatar_url FROM user_social us " +
            "LEFT JOIN user_information ui ON ui.user_id=us.user_id " +
            "WHERE us.user_id=#{userId} AND us.deleted=0 AND us.visibility IN (${visibilitySql}) " +
            "ORDER BY us.created_at DESC LIMIT #{offset},#{size}")
    List<Map<String, Object>> listUserSocials(@Param("userId") Long userId, @Param("visibilitySql") String visibilitySql, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(1) FROM user_social WHERE user_id=#{userId} AND deleted=0 AND visibility IN (${visibilitySql})")
    Long countUserSocials(@Param("userId") Long userId, @Param("visibilitySql") String visibilitySql);
}
