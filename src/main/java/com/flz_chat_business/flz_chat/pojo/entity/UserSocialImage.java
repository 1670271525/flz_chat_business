package com.flz_chat_business.flz_chat.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_social_image")
public class UserSocialImage {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("social_id")
    private Long socialId;

    @TableField("image_url")
    private String imageUrl;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("width")
    private Integer width;

    @TableField("height")
    private Integer height;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
