package com.flz_chat_business.flz_chat.conversation.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
/**
 * 更新成员角色请求参数。
 */
public class UpdateRoleRequest {
    /** 目标成员用户ID。 */
    @NotNull
    private Long userId;
    /** 角色值：2管理员，3普通成员。 */
    @NotNull
    private Integer role;
}
