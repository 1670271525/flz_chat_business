package com.flz_chat_business.flz_chat.conversation.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
/**
 * 创建单聊请求参数。
 */
public class CreateSingleConversationRequest {
    /** 对端用户ID。 */
    @NotNull
    private Long peerUserId;
}
