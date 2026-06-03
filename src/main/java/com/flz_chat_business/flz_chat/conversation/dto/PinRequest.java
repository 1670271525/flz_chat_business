package com.flz_chat_business.flz_chat.conversation.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
/**
 * 会话置顶开关请求参数。
 */
public class PinRequest {
    /** true 置顶，false 取消置顶。 */
    @NotNull
    private Boolean pinned;
}
