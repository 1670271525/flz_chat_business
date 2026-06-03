package com.flz_chat_business.common.util;

public final class PageUtils {
    private PageUtils() {
    }

    public static int normalizePage(Integer page) {
        if (page == null || page < 1) {
            return 1;
        }
        return page;
    }

    public static int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 20;
        }
        return Math.min(size, 50);
    }

    public static int offset(int page, int size) {
        return (page - 1) * size;
    }
}
