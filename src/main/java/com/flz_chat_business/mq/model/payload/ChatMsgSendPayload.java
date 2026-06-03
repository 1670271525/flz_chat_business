package com.flz_chat_business.mq.model.payload;

import lombok.Data;

import java.util.List;

@Data
public class ChatMsgSendPayload {
    private Long messageId;
    private Long conversationId;
    private Long senderId;
    private List<Long> receivers;
    private Integer type;
    private String content;
    private Integer isAgent;
    private String downloadUrl;
    private String downloadUrlExpireAt;
    private String mediaMeta;
    private String createdAt;
}
