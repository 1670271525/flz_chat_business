package com.flz_chat_business.mq.model;

import lombok.Data;

@Data
public class MqEnvelope<T> {
    private String msgId;
    private Integer version;
    private String occurredAt;
    private String source;
    private String type;
    private T payload;
}
