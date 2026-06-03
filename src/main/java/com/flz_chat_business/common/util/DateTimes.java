package com.flz_chat_business.common.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 业务统一使用 Asia/Shanghai 墙钟时间，与 MySQL DATETIME 语义一致。
 */
public final class DateTimes {

    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    public static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private DateTimes() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    public static String formatOffset(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZONE).format(ISO_OFFSET);
    }

    public static ZonedDateTime nowZoned() {
        return ZonedDateTime.now(ZONE);
    }
}
