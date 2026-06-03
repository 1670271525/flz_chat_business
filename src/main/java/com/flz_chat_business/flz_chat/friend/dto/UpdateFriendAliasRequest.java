package com.flz_chat_business.flz_chat.friend.dto;

import lombok.Data;

@Data
/**
 * 更新好友备注请求参数。
 */
public class UpdateFriendAliasRequest {
    /** 好友备注名。 */
    private String alias;
}
