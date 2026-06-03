package com.flz_chat_business.flz_chat.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("friend_requests")
public class FriendRequest {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("from_user_id")
    private Long fromUserId;

    @TableField("to_user_id")
    private Long toUserId;

    @TableField("remark")
    private String remark;

    @TableField("status")
    private Integer status;

    @TableField("expire_at")
    private LocalDateTime expireAt;

    @TableField("handled_at")
    private LocalDateTime handledAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
