package com.personblog.common.exception;

import com.personblog.common.enums.BizCodeEnum;
import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final Integer code;
    private final String message;

    public BizException(BizCodeEnum bizCodeEnum) {
        super(bizCodeEnum.getMessage());
        this.code = bizCodeEnum.getCode();
        this.message = bizCodeEnum.getMessage();
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BizException(String message) {
        super(message);
        this.code = -1;
        this.message = message;
    }
}
