package com.flz_chat_business.mq.producer;

import com.alibaba.fastjson2.JSON;
import com.flz_chat_business.mq.config.MqConstants;
import com.flz_chat_business.mq.model.MqEnvelope;
import com.flz_chat_business.common.util.DateTimes;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatMqProducer {

    private final RabbitTemplate rabbitTemplate;

    public <T> void publish(String routingKey, T payload) {
        MqEnvelope<T> envelope = new MqEnvelope<>();
        envelope.setMsgId(UUID.randomUUID().toString());
        envelope.setVersion(1);
        envelope.setOccurredAt(DateTimes.nowZoned().format(DateTimes.ISO_OFFSET));
        envelope.setSource("business");
        envelope.setType(routingKey);
        envelope.setPayload(payload);
        rabbitTemplate.convertAndSend(MqConstants.CHAT_EXCHANGE, routingKey, JSON.toJSONString(envelope));
    }
}
