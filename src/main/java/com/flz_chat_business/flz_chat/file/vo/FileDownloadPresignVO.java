package com.flz_chat_business.flz_chat.file.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileDownloadPresignVO {
    private String url;
    private Integer expireSeconds;
}
