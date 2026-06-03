package com.flz_chat_business.flz_chat.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversations")
public class Conversation {
    @TableId(value = "conversation_id", type = IdType.AUTO)
    private Long conversationId;

    @TableField("type")
    private Integer type;

    @TableField("name")
    private String name;

    @TableField("avatar_url")
    private String avatarUrl;

    @TableField("owner_id")
    private Long ownerId;

    @TableField("max_members")
    private Integer maxMembers;

    @TableField("last_message_id")
    private Long lastMessageId;

    @TableField("last_message_at")
    private LocalDateTime lastMessageAt;

    @TableField("dissolved")
    private Integer dissolved;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
