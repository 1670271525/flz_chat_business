package com.flz_chat_business.flz_chat.file.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
/**
 * 文件预签名请求参数。
 */
public class FilePresignRequest {
    /** 桶别名：chat/public。 */
    @NotBlank(message = "bucket 不能为空")
    @Pattern(regexp = "chat|public", message = "bucket 仅支持 chat/public")
    private String bucket;

    /** 原始文件名。 */
    @NotBlank(message = "filename 不能为空")
    private String filename;

    /** 文件 MIME 类型。 */
    @NotBlank(message = "contentType 不能为空")
    private String contentType;

    /** 文件大小（字节）。 */
    @Min(value = 1, message = "size 需大于0")
    @Max(value = 10485760, message = "size 不可超过10MB")
    private Long size;
}
