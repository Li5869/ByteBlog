package com.personblog.interaction.controller;

import com.personblog.common.result.JsonData;
import com.personblog.common.sse.SseEmitterManager;
import com.personblog.common.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/interaction/sse")
@Tag(name = "SSE推送",description = "服务器推送")
@RequiredArgsConstructor
@Slf4j
public class SseController {
    private final SseEmitterManager sseEmitterManager;

    /**
     * 建立 SSE 连接
     * 客户端通过 EventSource 连接此端点
     */
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "建立SSE连接", description = "客户端通过EventSource连接此端点，接收实时通知（需登录）")
    public SseEmitter connect() {
        Long userId = UserContextHolder.getUserId();
        // 未登录用户不建立 SSE 连接，避免空指针异常
        if (userId == null) {
            log.warn("未登录用户尝试建立 SSE 连接，已拒绝");
            return null;
        }

        log.info("用户 {} 建立 SSE 连接", userId);

        SseEmitter emitter = sseEmitterManager.createEmitter(userId);

        // 发送连接成功消息
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("SSE 连接成功"));
        } catch (Exception e) {
            log.error("发送连接成功消息失败", e);
        }

        return emitter;
    }
    /**
     * 手动断开 SSE 连接
     */
    @DeleteMapping("/disconnect")
    @Operation(summary = "断开SSE连接", description = "客户端主动断开SSE连接")
    public JsonData<Void> disconnect() {
        Long userId = UserContextHolder.getUserId();
        // SseEmitterManager 中会自动清理，这里只需返回成功
        log.info("用户 {} 断开 SSE 连接", userId);
        return JsonData.buildSuccess();
    }
}
