package com.flz_chat_business.flz_chat.social.controller;

import com.flz_chat_business.common.model.PageData;
import com.flz_chat_business.flz_chat.social.dto.PublishSocialRequest;
import com.flz_chat_business.flz_chat.social.service.SocialService;
import com.flz_chat_business.security.util.SecurityUtils;
import com.flz_chat_business.vo.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/social")
/**
 * 社交动态业务接口。
 */
public class SocialController {

    private final SocialService socialService;

    @PostMapping
    /**
     * 发布动态。
     *
     * @param request content 为动态文字，visibility 为可见范围（0公开/1好友/2仅自己），images 为图片数组
     * @return data 包含 socialId
     */
    public Result<Map<String, Object>> publish(@Valid @RequestBody PublishSocialRequest request) {
        return Result.success(socialService.publish(SecurityUtils.getCurrentUserId(), request));
    }

    @GetMapping("/users/{userId}")
    /**
     * 查询指定用户动态列表。
     *
     * @param userId 目标用户ID
     * @param page 页码
     * @param size 每页数量
     * @return 动态分页列表
     */
    public Result<PageData<Map<String, Object>>> userSocials(@PathVariable("userId") Long userId,
                                                             @RequestParam(value = "page", required = false) Integer page,
                                                             @RequestParam(value = "size", required = false) Integer size) {
        return Result.success(socialService.userSocials(SecurityUtils.getCurrentUserId(), userId, page, size));
    }

    @GetMapping("/feed")
    /**
     * 查询当前用户好友动态流。
     *
     * @param page 页码
     * @param size 每页数量
     * @return 动态分页列表
     */
    public Result<PageData<Map<String, Object>>> feed(@RequestParam(value = "page", required = false) Integer page,
                                                      @RequestParam(value = "size", required = false) Integer size) {
        return Result.success(socialService.feed(SecurityUtils.getCurrentUserId(), page, size));
    }

    @DeleteMapping("/{socialId}")
    /**
     * 删除动态（仅本人）。
     *
     * @param socialId 动态ID
     * @return 通用成功响应
     */
    public Result<Void> delete(@PathVariable("socialId") Long socialId) {
        socialService.delete(SecurityUtils.getCurrentUserId(), socialId);
        return Result.success();
    }

    @PostMapping("/{socialId}/like")
    /**
     * 点赞动态。
     *
     * @param socialId 动态ID
     * @return 通用成功响应
     */
    public Result<Void> like(@PathVariable("socialId") Long socialId) {
        socialService.like(SecurityUtils.getCurrentUserId(), socialId);
        return Result.success();
    }

    @DeleteMapping("/{socialId}/like")
    /**
     * 取消点赞动态。
     *
     * @param socialId 动态ID
     * @return 通用成功响应
     */
    public Result<Void> unlike(@PathVariable("socialId") Long socialId) {
        socialService.unlike(SecurityUtils.getCurrentUserId(), socialId);
        return Result.success();
    }
}
