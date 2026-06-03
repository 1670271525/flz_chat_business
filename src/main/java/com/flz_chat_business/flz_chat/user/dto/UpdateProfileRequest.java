package com.flz_chat_business.flz_chat.user.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
/**
 * 更新个人资料请求参数。
 */
public class UpdateProfileRequest {
    /** 用户昵称。 */
    private String nickname;
    /** 头像对象地址（MinIO objectKey）。 */
    private String avatarUrl;
    /** 对外状态（如 HAPPY/SAD 等）。 */
    private String mood;
    /** 个性签名。 */
    private String signature;
    /** 性别：0未知，1男，2女。 */
    private Integer gender;
    /** 生日。 */
    private LocalDate birthday;
    /** 地区。 */
    private String region;
}
