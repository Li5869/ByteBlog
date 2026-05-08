package com.personblog.ai.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChatEventTypeEnum {

    DATA(1, "数据"),
    STOP(2, "停止"),
    PARAM(3, "参数");

    private final int value;
    private final String desc;
}
