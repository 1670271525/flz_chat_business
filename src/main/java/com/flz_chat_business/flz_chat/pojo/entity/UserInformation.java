package com.flz_chat_business.flz_chat.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("user_information")
public class UserInformation {
    @TableId("user_id")
    private Long userId;

    @TableField("avatar_url")
    private String avatarUrl;

    @TableField("nickname")
    private String nickname;

    @TableField("mood")
    private String mood;

    @TableField("signature")
    private String signature;

    @TableField("gender")
    private Integer gender;

    @TableField("birthday")
    private LocalDate birthday;

    @TableField("region")
    private String region;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
