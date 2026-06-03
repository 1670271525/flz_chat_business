package com.flz_chat_business.flz_chat.social.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;

@Data
/**
 * 发布动态请求参数。
 */
public class PublishSocialRequest {
    /** 动态文本内容。 */
    private String content;

    /** 可见性：0公开/1仅好友/2仅自己。 */
    @Min(0)
    @Max(2)
    private Integer visibility;

    /** 动态图片列表。 */
    private List<ImageItem> images;

    @Data
    /**
     * 动态图片项参数。
     */
    public static class ImageItem {
        /** 图片 objectKey。 */
        private String imageUrl;
        /** 排序序号。 */
        private Integer sortOrder;
        /** 图片宽度。 */
        private Integer width;
        /** 图片高度。 */
        private Integer height;
    }
}
