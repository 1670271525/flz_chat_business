package com.flz_chat_business.flz_chat.conversation.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class BooleanFlagRequest {
    @NotNull
    private Boolean value;
}
