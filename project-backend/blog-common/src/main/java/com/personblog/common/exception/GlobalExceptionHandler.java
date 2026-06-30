package com.personblog.common.exception;

import com.personblog.common.result.JsonData;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 捕获所有异常并转换为统一的JsonData响应格式
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.OK)
    public JsonData<Void> handleBizException(BizException e) {
        log.error("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return JsonData.buildError(e.getMessage());
    }

    /**
     * 处理参数校验异常（@Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public JsonData<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.error("参数校验失败: {}", message);
        return JsonData.buildError(message);
    }

    /**
     * 处理参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public JsonData<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.error("参数绑定失败: {}", message);
        return JsonData.buildError(message);
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public JsonData<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("非法参数异常: {}", e.getMessage());
        return JsonData.buildError(e.getMessage());
    }

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public JsonData<Void> handleException(Exception e, WebRequest request) {
        // SSE 连接断开等异步异常到达时，响应已提交或 Content-Type 为 text/event-stream，
        // 此时写入 JSON 会二次报错，直接跳过
        if (request instanceof ServletWebRequest servletRequest) {
            HttpServletResponse response = servletRequest.getResponse();
            if (response != null && response.isCommitted()) {
                log.debug("响应已提交，跳过异常响应: {}", e.getMessage());
                return null;
            }
        }
        log.error("系统异常: ", e);
        return JsonData.buildError("系统繁忙，请稍后重试");
    }
}
