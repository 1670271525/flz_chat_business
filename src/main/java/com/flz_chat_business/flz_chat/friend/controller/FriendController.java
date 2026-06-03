package com.flz_chat_business.flz_chat.friend.controller;

import com.flz_chat_business.common.model.PageData;
import com.flz_chat_business.flz_chat.friend.dto.CreateFriendRequestDTO;
import com.flz_chat_business.flz_chat.friend.dto.UpdateFriendAliasRequest;
import com.flz_chat_business.flz_chat.friend.service.FriendService;
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
@RequestMapping("/api/friends")
/**
 * 好友关系业务接口。
 */
public class FriendController {

    private final FriendService friendService;

    @GetMapping
    /**
     * 查询好友列表。
     *
     * @param page 页码（从1开始）
     * @param size 每页数量（最大50）
     * @return 好友分页列表
     */
    public Result<PageData<Map<String, Object>>> list(@RequestParam(value = "page", required = false) Integer page,
                                                      @RequestParam(value = "size", required = false) Integer size) {
        return Result.success(friendService.listFriends(SecurityUtils.getCurrentUserId(), page, size));
    }

    @PostMapping("/requests")
    /**
     * 发起好友申请。
     *
     * @param request toUserId 为接收者ID，remark 为申请备注
     * @return 通用成功响应
     */
    public Result<Void> request(@Valid @RequestBody CreateFriendRequestDTO request) {
        friendService.createRequest(SecurityUtils.getCurrentUserId(), request);
        return Result.success();
    }

    @GetMapping("/requests/incoming")
    /**
     * 查询我收到的好友申请。
     *
     * @param status 申请状态，可为空
     * @param page 页码
     * @param size 每页数量
     * @return 申请分页列表
     */
    public Result<PageData<Map<String, Object>>> incoming(@RequestParam(value = "status", required = false) Integer status,
                                                          @RequestParam(value = "page", required = false) Integer page,
                                                          @RequestParam(value = "size", required = false) Integer size) {
        return Result.success(friendService.listIncoming(SecurityUtils.getCurrentUserId(), status, page, size));
    }

    @GetMapping("/requests/outgoing")
    /**
     * 查询我发出的好友申请。
     *
     * @param status 申请状态，可为空
     * @param page 页码
     * @param size 每页数量
     * @return 申请分页列表
     */
    public Result<PageData<Map<String, Object>>> outgoing(@RequestParam(value = "status", required = false) Integer status,
                                                          @RequestParam(value = "page", required = false) Integer page,
                                                          @RequestParam(value = "size", required = false) Integer size) {
        return Result.success(friendService.listOutgoing(SecurityUtils.getCurrentUserId(), status, page, size));
    }

    @PostMapping("/requests/{requestId}/accept")
    /**
     * 同意好友申请。
     *
     * @param requestId 好友申请ID
     * @return data 包含自动创建/复用的 conversationId
     */
    public Result<Map<String, Object>> accept(@PathVariable("requestId") Long requestId) {
        return Result.success(friendService.accept(SecurityUtils.getCurrentUserId(), requestId));
    }

    @PostMapping("/requests/{requestId}/reject")
    /**
     * 拒绝好友申请。
     *
     * @param requestId 好友申请ID
     * @return 通用成功响应
     */
    public Result<Void> reject(@PathVariable("requestId") Long requestId) {
        friendService.reject(SecurityUtils.getCurrentUserId(), requestId);
        return Result.success();
    }

    @PutMapping("/{friendId}")
    /**
     * 设置好友备注。
     *
     * @param friendId 好友用户ID
     * @param request alias 为备注名
     * @return 通用成功响应
     */
    public Result<Void> updateAlias(@PathVariable("friendId") Long friendId, @RequestBody UpdateFriendAliasRequest request) {
        friendService.updateAlias(SecurityUtils.getCurrentUserId(), friendId, request);
        return Result.success();
    }

    @PostMapping("/{friendId}/block")
    /**
     * 拉黑好友。
     *
     * @param friendId 好友用户ID
     * @return 通用成功响应
     */
    public Result<Void> block(@PathVariable("friendId") Long friendId) {
        friendService.block(SecurityUtils.getCurrentUserId(), friendId);
        return Result.success();
    }

    @PostMapping("/{friendId}/unblock")
    /**
     * 取消拉黑好友。
     *
     * @param friendId 好友用户ID
     * @return 通用成功响应
     */
    public Result<Void> unblock(@PathVariable("friendId") Long friendId) {
        friendService.unblock(SecurityUtils.getCurrentUserId(), friendId);
        return Result.success();
    }

    @DeleteMapping("/{friendId}")
    /**
     * 删除好友关系。
     *
     * @param friendId 好友用户ID
     * @return 通用成功响应
     */
    public Result<Void> delete(@PathVariable("friendId") Long friendId) {
        friendService.deleteFriend(SecurityUtils.getCurrentUserId(), friendId);
        return Result.success();
    }
}
