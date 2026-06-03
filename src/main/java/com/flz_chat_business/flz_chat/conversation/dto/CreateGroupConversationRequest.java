package com.flz_chat_business.flz_chat.conversation.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
/**
 * 创建群聊请求参数。
 */
public class CreateGroupConversationRequest {
    /** 群名称。 */
    @NotBlank
    private String name;
    /** 群头像 objectKey。 */
    private String avatarUrl;
    /** 初始成员用户ID列表。 */
    private List<Long> memberIds;
}
