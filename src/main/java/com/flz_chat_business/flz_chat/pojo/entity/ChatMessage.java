package com.flz_chat_business.flz_chat.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message")
public class ChatMessage {
    @TableId(value = "message_id", type = IdType.AUTO)
    private Long messageId;

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("sender_id")
    private Long senderId;

    @TableField("content")
    private String content;

    @TableField("type")
    private Integer type;

    @TableField("is_agent")
    private Integer isAgent;

    @TableField("media_meta")
    private String mediaMeta;

    @TableField("status")
    private Integer status;

    @TableField("client_msg_id")
    private String clientMsgId;

    @TableField("deleted")
    private Integer deleted;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
