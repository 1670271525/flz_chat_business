package com.flz_chat_business.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.flz_chat_business.common.exception.BizException;
import com.flz_chat_business.mq.config.MqConstants;
import com.flz_chat_business.mq.model.MqEnvelope;
import com.flz_chat_business.mq.model.payload.BusinessMsgAckPayload;
import com.flz_chat_business.mq.model.payload.BusinessPersistPayload;
import com.flz_chat_business.mq.model.payload.BusinessUserOnlinePayload;
import com.flz_chat_business.flz_chat.message.service.MessageService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class BusinessMqConsumer {

    private final MqIdempotencyService idempotencyService;
    private final MessageService messageService;

    @RabbitListener(queues = MqConstants.Q_PERSIST)
    public void onPersist(Message amqp, Channel channel) throws IOException {
        long tag = amqp.getMessageProperties().getDeliveryTag();
        try {
            String body = new String(amqp.getBody(), StandardCharsets.UTF_8);
            MqEnvelope<JSONObject> env = JSON.parseObject(body, new com.alibaba.fastjson2.TypeReference<MqEnvelope<JSONObject>>() {
            });
            if (!idempotencyService.tryMarkProcessed(env.getMsgId())) {
                channel.basicAck(tag, false);
                return;
            }
            BusinessPersistPayload payload = env.getPayload().to(BusinessPersistPayload.class);
            messageService.persistFromMq(payload);
            channel.basicAck(tag, false);
        } catch (BizException ex) {
            channel.basicNack(tag, false, false);
        } catch (Exception ex) {
            log.error("Consume persist failed", ex);
            channel.basicNack(tag, false, true);
        }
    }

    @RabbitListener(queues = MqConstants.Q_USER_EVENT)
    public void onUserEvent(Message amqp, Channel channel) throws IOException {
        long tag = amqp.getMessageProperties().getDeliveryTag();
        try {
            String body = new String(amqp.getBody(), StandardCharsets.UTF_8);
            MqEnvelope<JSONObject> env = JSON.parseObject(body, new com.alibaba.fastjson2.TypeReference<MqEnvelope<JSONObject>>() {
            });
            if (!idempotencyService.tryMarkProcessed(env.getMsgId())) {
                channel.basicAck(tag, false);
                return;
            }
            BusinessUserOnlinePayload payload = env.getPayload().to(BusinessUserOnlinePayload.class);
            if (MqConstants.RK_BUSINESS_USER_ONLINE.equals(env.getType())) {
                messageService.replayUnreadForUser(payload);
            } else {
                log.info("Receive user event type={}, userId={}", env.getType(), payload.getUserId());
            }
            channel.basicAck(tag, false);
        } catch (BizException ex) {
            channel.basicNack(tag, false, false);
        } catch (Exception ex) {
            log.error("Consume user event failed", ex);
            channel.basicNack(tag, false, true);
        }
    }

    @RabbitListener(queues = MqConstants.Q_MSG_ACK)
    public void onAck(Message amqp, Channel channel) throws IOException {
        long tag = amqp.getMessageProperties().getDeliveryTag();
        try {
            String body = new String(amqp.getBody(), StandardCharsets.UTF_8);
            MqEnvelope<JSONObject> env = JSON.parseObject(body, new com.alibaba.fastjson2.TypeReference<MqEnvelope<JSONObject>>() {
            });
            if (!idempotencyService.tryMarkProcessed(env.getMsgId())) {
                channel.basicAck(tag, false);
                return;
            }
            BusinessMsgAckPayload payload = env.getPayload().to(BusinessMsgAckPayload.class);
            messageService.ackMessage(payload);
            channel.basicAck(tag, false);
        } catch (BizException ex) {
            channel.basicNack(tag, false, false);
        } catch (Exception ex) {
            log.error("Consume message ack failed", ex);
            channel.basicNack(tag, false, true);
        }
    }
}
