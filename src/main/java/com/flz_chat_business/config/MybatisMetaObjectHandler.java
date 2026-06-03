package com.flz_chat_business.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.flz_chat_business.common.util.DateTimes;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = DateTimes.now();
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "userCreatedAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "userUpdatedAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "joinedAt", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, DateTimes.now());
        strictUpdateFill(metaObject, "userUpdatedAt", LocalDateTime.class, DateTimes.now());
    }
}
