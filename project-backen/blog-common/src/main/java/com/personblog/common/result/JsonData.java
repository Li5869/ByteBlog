package com.personblog.common.result;

import cn.hutool.core.lang.TypeReference;
import com.alibaba.fastjson2.JSON;
import com.personblog.common.enums.BizCodeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "返回包装类")
public class JsonData<T> {

    @Schema(description = "状态码：0=成功, 1=处理中, -1=失败")
    private Integer code;

    @Schema(description = "响应消息")
    private String msg;

    @Schema(description = "响应数据")
    private T data;

    /**
     * 无数据成功返回
     */
    public static <T> JsonData<T> buildSuccess() {
        return new JsonData<>(0, null, null);
    }

    /**
     * 带数据成功返回
     */
    public static <T> JsonData<T> buildSuccess(T data) {
        return new JsonData<>(0, null, data);
    }

    /**
     * 带数据和消息成功返回
     */
    public static <T> JsonData<T> buildSuccess(T data, String msg) {
        return new JsonData<>(0, msg, data);
    }

    /**
     * 错误返回
     */
    public static <T> JsonData<T> buildError(String msg) {
        return new JsonData<>(-1, msg, null);
    }

    /**
     * 自定义code和msg返回
     */
    public static <T> JsonData<T> buildCodeAndMsg(Integer code, String msg) {
        return new JsonData<>(code, msg, null);
    }

    /**
     * 从枚举返回
     */
    public static <T> JsonData<T> buildResult(BizCodeEnum bizCodeEnum) {
        return new JsonData<>(bizCodeEnum.getCode(), bizCodeEnum.getMessage(), null);
    }

    /**
     * 解析data字段
     */
    public <R> R getData(TypeReference<R> typeReference) {
        return JSON.parseObject(JSON.toJSONString(data), typeReference);
    }
}