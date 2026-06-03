package com.flz_chat_business.flz_chat.conversation.dto;

import lombok.Data;

@Data
/**
 * 更新会话信息请求参数。
 */
public class UpdateConversationRequest {
    /** 会话名称（群聊使用）。 */
    private String name;
    /** 会话头像 objectKey。 */
    private String avatarUrl;
}
