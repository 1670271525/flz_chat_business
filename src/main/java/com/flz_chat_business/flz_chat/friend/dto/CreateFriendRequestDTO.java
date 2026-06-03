package com.flz_chat_business.flz_chat.friend.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
/**
 * 发起好友申请请求参数。
 */
public class CreateFriendRequestDTO {
    /** 申请目标用户ID。 */
    @NotNull
    private Long toUserId;
    /** 申请附言。 */
    private String remark;
}
