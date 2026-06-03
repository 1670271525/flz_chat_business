package com.flz_chat_business.flz_chat.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_social")
public class UserSocial {
    @TableId(value = "social_id", type = IdType.AUTO)
    private Long socialId;

    @TableField("user_id")
    private Long userId;

    @TableField("content")
    private String content;

    @TableField("visibility")
    private Integer visibility;

    @TableField("like_count")
    private Integer likeCount;

    @TableField("image_count")
    private Integer imageCount;

    @TableField("deleted")
    private Integer deleted;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
