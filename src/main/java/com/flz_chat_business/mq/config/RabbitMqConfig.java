package com.flz_chat_business.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public TopicExchange chatExchange() {
        return ExchangeBuilder.topicExchange(MqConstants.CHAT_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange businessExchange() {
        return ExchangeBuilder.topicExchange(MqConstants.BUSINESS_EXCHANGE).durable(true).build();
    }

    @Bean
    public FanoutExchange businessDlx() {
        return ExchangeBuilder.fanoutExchange(MqConstants.BUSINESS_DLX).durable(true).build();
    }

    @Bean
    public Queue businessDlq() {
        return QueueBuilder.durable(MqConstants.BUSINESS_DLQ).build();
    }

    @Bean
    public Queue qPersist() {
        return QueueBuilder.durable(MqConstants.Q_PERSIST)
                .deadLetterExchange(MqConstants.BUSINESS_DLX)
                .ttl(86_400_000)
                .build();
    }

    @Bean
    public Queue qUserEvent() {
        return QueueBuilder.durable(MqConstants.Q_USER_EVENT)
                .deadLetterExchange(MqConstants.BUSINESS_DLX)
                .ttl(86_400_000)
                .build();
    }

    @Bean
    public Queue qMsgAck() {
        return QueueBuilder.durable(MqConstants.Q_MSG_ACK)
                .deadLetterExchange(MqConstants.BUSINESS_DLX)
                .ttl(86_400_000)
                .build();
    }

    @Bean
    public Binding bPersist(Queue qPersist, TopicExchange businessExchange) {
        return BindingBuilder.bind(qPersist).to(businessExchange).with(MqConstants.RK_BUSINESS_MSG_PERSIST);
    }

    @Bean
    public Binding bUserOn(Queue qUserEvent, TopicExchange businessExchange) {
        return BindingBuilder.bind(qUserEvent).to(businessExchange).with(MqConstants.RK_BUSINESS_USER_ONLINE);
    }

    @Bean
    public Binding bUserOff(Queue qUserEvent, TopicExchange businessExchange) {
        return BindingBuilder.bind(qUserEvent).to(businessExchange).with(MqConstants.RK_BUSINESS_USER_OFFLINE);
    }

    @Bean
    public Binding bAck(Queue qMsgAck, TopicExchange businessExchange) {
        return BindingBuilder.bind(qMsgAck).to(businessExchange).with(MqConstants.RK_BUSINESS_MSG_ACK);
    }

    @Bean
    public Binding bDlq(Queue businessDlq, FanoutExchange businessDlx) {
        return BindingBuilder.bind(businessDlq).to(businessDlx);
    }
}
