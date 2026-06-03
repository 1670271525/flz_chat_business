package com.flz_chat_business.flz_chat.message.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
/**
 * 发送消息请求参数。
 */
public class SendMessageRequest {
    /** 所属会话ID。 */
    @NotNull
    private Long conversationId;

    /** 消息类型：2图片/3语音/4视频/5文件（HTTP 入口不接收纯文本 type=1）。 */
    @NotNull
    private Integer type;

    /** 消息内容：文本或 MinIO objectKey。 */
    @NotBlank
    private String content;

    /** 媒体扩展信息（JSON 字符串）。 */
    private String mediaMeta;

    /** 客户端消息幂等ID。 */
    @NotBlank
    private String clientMsgId;
}
