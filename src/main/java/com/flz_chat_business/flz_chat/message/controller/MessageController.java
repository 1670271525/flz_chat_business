package com.flz_chat_business.flz_chat.message.controller;

import com.flz_chat_business.flz_chat.message.dto.SendMessageRequest;
import com.flz_chat_business.flz_chat.message.service.MessageService;
import com.flz_chat_business.flz_chat.pojo.entity.ChatMessage;
import com.flz_chat_business.security.util.SecurityUtils;
import com.flz_chat_business.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/messages")
/**
 * 消息业务接口。
 */
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    /**
     * 发送文件类型消息（图片/语音/视频/文件）。
     * 纯文本消息改为由 chat 长连接服务通过 MQ `business.msg.persist` 回调持久化。
     *
     * @param request conversationId 为会话ID，type 为消息类型，content 为消息内容或对象键，
     *                mediaMeta 为媒体信息，clientMsgId 为客户端幂等ID
     * @return data 包含 messageId/conversationId/createdAt/downloadUrl
     */
    public Result<Map<String, Object>> send(@Valid @RequestBody SendMessageRequest request) {
        return Result.success(messageService.send(SecurityUtils.getCurrentUserId(), request));
    }

    @GetMapping
    /**
     * 查询历史消息。
     *
     * @param conversationId 会话ID
     * @param beforeId 向前翻页游标，空则从最新开始
     * @param size 返回条数，默认30，最大50
     * @return 历史消息列表（倒序）
     */
    public Result<List<ChatMessage>> history(@RequestParam("conversationId") Long conversationId,
                                             @RequestParam(value = "beforeId", required = false) Long beforeId,
                                             @RequestParam(value = "size", required = false) Integer size) {
        return Result.success(messageService.history(SecurityUtils.getCurrentUserId(), conversationId, beforeId, size));
    }

    @GetMapping("/unread")
    /**
     * 查询当前会话未读消息。
     *
     * @param conversationId 会话ID
     * @return 未读消息列表（正序，最多200条）
     */
    public Result<List<ChatMessage>> unread(@RequestParam("conversationId") Long conversationId) {
        return Result.success(messageService.unread(SecurityUtils.getCurrentUserId(), conversationId));
    }

    @PostMapping("/{messageId}/recall")
    /**
     * 撤回消息（发送后2分钟内）。
     *
     * @param messageId 消息ID
     * @return 通用成功响应
     */
    public Result<Void> recall(@PathVariable("messageId") Long messageId) {
        messageService.recall(SecurityUtils.getCurrentUserId(), messageId);
        return Result.success();
    }

    @DeleteMapping("/{messageId}")
    /**
     * 单边删除消息（仅对当前用户隐藏）。
     *
     * @param messageId 消息ID
     * @return 通用成功响应
     */
    public Result<Void> delete(@PathVariable("messageId") Long messageId) {
        messageService.deleteForUser(SecurityUtils.getCurrentUserId(), messageId);
        return Result.success();
    }
}
