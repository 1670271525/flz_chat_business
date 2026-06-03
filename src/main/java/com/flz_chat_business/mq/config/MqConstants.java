package com.flz_chat_business.mq.config;

public final class MqConstants {

    private MqConstants() {
    }

    public static final String CHAT_EXCHANGE = "chat.exchange";
    public static final String BUSINESS_EXCHANGE = "business.exchange";
    public static final String BUSINESS_DLX = "business.dlx";
    public static final String BUSINESS_DLQ = "business.dlq";

    public static final String Q_PERSIST = "business.persist.queue";
    public static final String Q_USER_EVENT = "business.user.event.queue";
    public static final String Q_MSG_ACK = "business.msg.ack.queue";

    public static final String RK_BUSINESS_MSG_PERSIST = "business.msg.persist";
    public static final String RK_BUSINESS_USER_ONLINE = "business.user.online";
    public static final String RK_BUSINESS_USER_OFFLINE = "business.user.offline";
    public static final String RK_BUSINESS_MSG_ACK = "business.msg.ack";

    public static final String RK_CHAT_MSG_SEND = "chat.msg.send";
    public static final String RK_CHAT_MSG_RECALL = "chat.msg.recall";
    public static final String RK_CHAT_MSG_REPLAY = "chat.msg.replay";
    public static final String RK_CHAT_FRIEND_REQUEST = "chat.friend.request";
    public static final String RK_CHAT_FRIEND_ACCEPT = "chat.friend.accept";
}
