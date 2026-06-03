package com.flz_chat_business.flz_chat.conversation.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
/**
 * 标记会话已读请求参数。
 */
public class MarkReadRequest {
    /** 最新已读消息ID。 */
    @NotNull
    private Long lastReadMessageId;
}
