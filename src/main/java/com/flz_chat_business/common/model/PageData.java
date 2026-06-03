package com.flz_chat_business.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageData<T> {
    private Long total;
    private Integer page;
    private Integer size;
    private List<T> records;

    public static <T> PageData<T> empty(Integer page, Integer size) {
        return new PageData<>(0L, page, size, Collections.emptyList());
    }
}
