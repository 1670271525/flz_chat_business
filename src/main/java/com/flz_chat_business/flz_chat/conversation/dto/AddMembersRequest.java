package com.flz_chat_business.flz_chat.conversation.dto;

import lombok.Data;

import java.util.List;

@Data
/**
 * 添加会话成员请求参数。
 */
public class AddMembersRequest {
    /** 待添加成员ID列表。 */
    private List<Long> memberIds;
}
