package com.flz_chat_business.flz_chat.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class ChatUser {
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    @TableField("user_name")
    private String userName;

    @TableField("user_email")
    private String userEmail;

    @TableField("user_phone")
    private String userPhone;

    @TableField("user_password")
    private String userPassword;

    @TableField("user_status")
    private Integer userStatus;

    @TableField("user_deleted")
    private Integer userDeleted;

    @TableField(value = "user_created_at", fill = FieldFill.INSERT)
    private LocalDateTime userCreatedAt;

    @TableField(value = "user_updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime userUpdatedAt;
}
