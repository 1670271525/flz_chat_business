package com.flz_chat_business.flz_chat.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation_participants")
public class ConversationParticipant {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("user_id")
    private Long userId;

    @TableField("role")
    private Integer role;

    @TableField("display_name")
    private String displayName;

    @TableField("last_read_message_id")
    private Long lastReadMessageId;

    @TableField("mute")
    private Integer mute;

    @TableField("pinned")
    private Integer pinned;

    @TableField(value = "joined_at", fill = FieldFill.INSERT)
    private LocalDateTime joinedAt;

    @TableField("quit")
    private Integer quit;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
