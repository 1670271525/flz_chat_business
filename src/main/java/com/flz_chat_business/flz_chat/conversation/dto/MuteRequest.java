package com.flz_chat_business.flz_chat.conversation.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
/**
 * 会话免打扰开关请求参数。
 */
public class MuteRequest {
    /** true 开启免打扰，false 关闭免打扰。 */
    @NotNull
    private Boolean mute;
}
