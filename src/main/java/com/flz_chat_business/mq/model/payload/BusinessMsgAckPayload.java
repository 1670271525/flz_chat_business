package com.flz_chat_business.mq.model.payload;

import lombok.Data;

@Data
public class BusinessMsgAckPayload {
    private Long messageId;
    private Long receiverId;
    private String ackAt;
}
