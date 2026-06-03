package com.flz_chat_business.mq.model.payload;

import lombok.Data;

@Data
public class BusinessPersistPayload {
    private String clientMsgId;
    private Long conversationId;
    private Long senderId;
    private Integer type;
    private String content;
    private Integer isAgent;
    private String mediaMeta;
    private String sentAt;
}
