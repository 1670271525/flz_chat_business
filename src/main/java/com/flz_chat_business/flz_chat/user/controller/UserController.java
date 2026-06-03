package com.flz_chat_business.flz_chat.user.controller;

import com.flz_chat_business.common.model.PageData;
import com.flz_chat_business.flz_chat.user.dto.ChangePasswordRequest;
import com.flz_chat_business.flz_chat.user.dto.UpdateProfileRequest;
import com.flz_chat_business.flz_chat.user.service.UserService;
import com.flz_chat_business.security.util.SecurityUtils;
import com.flz_chat_business.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
/**
 * 用户资料业务接口。
 */
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    /**
     * 查询当前登录用户完整资料。
     *
     * @return 账号信息 + 个人资料信息
     */
    public Result<Map<String, Object>> me() {
        return Result.success(userService.getMyProfile(SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/me")
    /**
     * 更新当前登录用户资料。
     *
     * @param request 个人资料更新对象，字段为空则不更新
     * @return 通用成功响应
     */
    public Result<Void> updateMe(@RequestBody UpdateProfileRequest request) {
        userService.updateMyProfile(SecurityUtils.getCurrentUserId(), request);
        return Result.success();
    }

    @GetMapping("/{userId}")
    /**
     * 查看指定用户公开资料。
     *
     * @param userId 目标用户ID
     * @return 公开资料字段
     */
    public Result<Map<String, Object>> getById(@PathVariable("userId") Long userId) {
        return Result.success(userService.getUserPublicProfile(SecurityUtils.getCurrentUserId(), userId));
    }

    @GetMapping("/search")
    /**
     * 按关键字搜索用户。
     *
     * @param keyword 搜索关键词（用户名/邮箱/手机号）
     * @param page 页码（从1开始）
     * @param size 每页数量（最大50）
     * @return 分页用户列表
     */
    public Result<PageData<Map<String, Object>>> search(@RequestParam("keyword") String keyword,
                                                        @RequestParam(value = "page", required = false) Integer page,
                                                        @RequestParam(value = "size", required = false) Integer size) {
        return Result.success(userService.searchUsers(keyword, page, size));
    }

    @PutMapping("/me/password")
    /**
     * 修改当前登录用户密码。
     *
     * @param request oldPassword 为旧密码，newPassword 为新密码
     * @return 通用成功响应
     */
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(SecurityUtils.getCurrentUserId(), request);
        return Result.success();
    }
}
