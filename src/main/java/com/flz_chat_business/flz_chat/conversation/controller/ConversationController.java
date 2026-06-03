package com.flz_chat_business.flz_chat.conversation.controller;

import com.flz_chat_business.common.model.PageData;
import com.flz_chat_business.flz_chat.conversation.dto.AddMembersRequest;
import com.flz_chat_business.flz_chat.conversation.dto.CreateGroupConversationRequest;
import com.flz_chat_business.flz_chat.conversation.dto.CreateSingleConversationRequest;
import com.flz_chat_business.flz_chat.conversation.dto.MarkReadRequest;
import com.flz_chat_business.flz_chat.conversation.dto.MuteRequest;
import com.flz_chat_business.flz_chat.conversation.dto.PinRequest;
import com.flz_chat_business.flz_chat.conversation.dto.UpdateConversationRequest;
import com.flz_chat_business.flz_chat.conversation.dto.UpdateRoleRequest;
import com.flz_chat_business.flz_chat.conversation.service.ConversationService;
import com.flz_chat_business.security.util.SecurityUtils;
import com.flz_chat_business.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations")
/**
 * 会话业务接口。
 */
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping
    /**
     * 查询当前用户会话列表。
     *
     * @param page 页码（从1开始）
     * @param size 每页数量（最大50）
     * @return 会话分页列表
     */
    public Result<PageData<Map<String, Object>>> list(@RequestParam(value = "page", required = false) Integer page,
                                                      @RequestParam(value = "size", required = false) Integer size) {
        return Result.success(conversationService.list(SecurityUtils.getCurrentUserId(), page, size));
    }

    @PostMapping("/single")
    /**
     * 创建单聊会话（存在则返回已存在会话）。
     *
     * @param request peerUserId 为对方用户ID
     * @return data 包含 conversationId
     */
    public Result<Map<String, Object>> createSingle(@Valid @RequestBody CreateSingleConversationRequest request) {
        return Result.success(conversationService.createSingle(SecurityUtils.getCurrentUserId(), request));
    }

    @PostMapping("/group")
    /**
     * 创建群聊会话。
     *
     * @param request name 为群名，avatarUrl 为群头像，memberIds 为成员ID列表
     * @return data 包含 conversationId
     */
    public Result<Map<String, Object>> createGroup(@Valid @RequestBody CreateGroupConversationRequest request) {
        return Result.success(conversationService.createGroup(SecurityUtils.getCurrentUserId(), request));
    }

    @PutMapping("/{id}")
    /**
     * 更新会话基础信息（群名/群头像）。
     *
     * @param conversationId 会话ID
     * @param request 更新字段
     * @return 通用成功响应
     */
    public Result<Void> updateConversation(@PathVariable("id") Long conversationId,
                                           @RequestBody UpdateConversationRequest request) {
        conversationService.updateConversation(SecurityUtils.getCurrentUserId(), conversationId, request);
        return Result.success();
    }

    @PostMapping("/{id}/members")
    /**
     * 添加群成员。
     *
     * @param conversationId 会话ID
     * @param request memberIds 为新增成员ID列表
     * @return 通用成功响应
     */
    public Result<Void> addMembers(@PathVariable("id") Long conversationId, @RequestBody AddMembersRequest request) {
        conversationService.addMembers(SecurityUtils.getCurrentUserId(), conversationId, request);
        return Result.success();
    }

    @DeleteMapping("/{id}/members/{userId}")
    /**
     * 移除群成员。
     *
     * @param conversationId 会话ID
     * @param userId 被移除成员ID
     * @return 通用成功响应
     */
    public Result<Void> removeMember(@PathVariable("id") Long conversationId, @PathVariable("userId") Long userId) {
        conversationService.removeMember(SecurityUtils.getCurrentUserId(), conversationId, userId);
        return Result.success();
    }

    @PostMapping("/{id}/quit")
    /**
     * 当前用户退出会话（退群）。
     *
     * @param conversationId 会话ID
     * @return 通用成功响应
     */
    public Result<Void> quit(@PathVariable("id") Long conversationId) {
        conversationService.quit(SecurityUtils.getCurrentUserId(), conversationId);
        return Result.success();
    }

    @PostMapping("/{id}/dissolve")
    /**
     * 解散群聊（仅群主）。
     *
     * @param conversationId 会话ID
     * @return 通用成功响应
     */
    public Result<Void> dissolve(@PathVariable("id") Long conversationId) {
        conversationService.dissolve(SecurityUtils.getCurrentUserId(), conversationId);
        return Result.success();
    }

    @PutMapping("/{id}/role")
    /**
     * 设置群成员角色。
     *
     * @param conversationId 会话ID
     * @param request userId 为目标成员ID，role 为角色（2管理员/3普通成员）
     * @return 通用成功响应
     */
    public Result<Void> role(@PathVariable("id") Long conversationId, @Valid @RequestBody UpdateRoleRequest request) {
        conversationService.updateRole(SecurityUtils.getCurrentUserId(), conversationId, request);
        return Result.success();
    }

    @GetMapping("/{id}/members")
    /**
     * 查询会话成员列表。
     *
     * @param conversationId 会话ID
     * @param page 页码
     * @param size 每页数量
     * @return 成员分页列表
     */
    public Result<PageData<Map<String, Object>>> members(@PathVariable("id") Long conversationId,
                                                         @RequestParam(value = "page", required = false) Integer page,
                                                         @RequestParam(value = "size", required = false) Integer size) {
        return Result.success(conversationService.members(SecurityUtils.getCurrentUserId(), conversationId, page, size));
    }

    @PutMapping("/{id}/read")
    /**
     * 标记会话已读位置。
     *
     * @param conversationId 会话ID
     * @param request lastReadMessageId 为最新已读消息ID
     * @return 通用成功响应
     */
    public Result<Void> read(@PathVariable("id") Long conversationId, @Valid @RequestBody MarkReadRequest request) {
        conversationService.markRead(SecurityUtils.getCurrentUserId(), conversationId, request);
        return Result.success();
    }

    @PutMapping("/{id}/pin")
    /**
     * 设置会话置顶状态。
     *
     * @param conversationId 会话ID
     * @param request pinned=true 置顶，false 取消置顶
     * @return 通用成功响应
     */
    public Result<Void> pin(@PathVariable("id") Long conversationId, @Valid @RequestBody PinRequest request) {
        conversationService.updatePin(SecurityUtils.getCurrentUserId(), conversationId, request);
        return Result.success();
    }

    @PutMapping("/{id}/mute")
    /**
     * 设置会话免打扰状态。
     *
     * @param conversationId 会话ID
     * @param request mute=true 开启免打扰，false 关闭免打扰
     * @return 通用成功响应
     */
    public Result<Void> mute(@PathVariable("id") Long conversationId, @Valid @RequestBody MuteRequest request) {
        conversationService.updateMute(SecurityUtils.getCurrentUserId(), conversationId, request);
        return Result.success();
    }
}
