package com.personblog.notification.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.common.result.JsonData;
import com.personblog.common.utils.UserContextHolder;
import com.personblog.notification.dto.BatchDeleteDTO;
import com.personblog.notification.dto.SystemNotificationQueryDTO;
import com.personblog.notification.service.ISystemNotificationService;
import com.personblog.notification.vo.BatchDeleteResultVO;
import com.personblog.notification.vo.SystemNotificationVO;
import com.personblog.notification.vo.UnreadCountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "系统通知", description = "系统通知相关接口")
@RestController
@RequestMapping("/interaction/notifications/system")
@RequiredArgsConstructor
public class SystemNotificationController {

    private final ISystemNotificationService systemNotificationService;

    @PostMapping
    @Operation(summary = "获取系统通知列表", description = "分页查询当前用户的系统通知列表")
    public JsonData<Page<SystemNotificationVO>> getSystemNotificationList(@RequestBody SystemNotificationQueryDTO dto) {
        Long userId = UserContextHolder.getUserId();
        Page<SystemNotificationVO> page = systemNotificationService.getSystemNotificationPage(userId, dto);
        return JsonData.buildSuccess(page);
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记系统通知已读", description = "将指定的系统通知标记为已读")
    public JsonData<Void> markAsRead(
            @Parameter(description = "通知ID") @PathVariable Long id) {
        Long userId = UserContextHolder.getUserId();
        systemNotificationService.markAsRead(userId, id);
        return JsonData.buildSuccess();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除系统通知", description = "删除指定的系统通知")
    public JsonData<Void> deleteNotification(
            @Parameter(description = "通知ID") @PathVariable Long id) {
        Long userId = UserContextHolder.getUserId();
        systemNotificationService.deleteNotification(userId, id);
        return JsonData.buildSuccess();
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除系统通知", description = "批量删除多条系统通知")
    public JsonData<BatchDeleteResultVO> batchDeleteNotifications(
            @Valid @RequestBody BatchDeleteDTO dto) {
        Long userId = UserContextHolder.getUserId();
        BatchDeleteResultVO result = systemNotificationService.batchDeleteNotifications(userId, dto.getIds());
        return JsonData.buildSuccess(result);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取系统通知未读数", description = "获取当前用户系统通知的未读数量")
    public JsonData<UnreadCountVO> getUnreadCount() {
        Long userId = UserContextHolder.getUserId();
        UnreadCountVO result = systemNotificationService.getUnreadCount(userId);
        return JsonData.buildSuccess(result);
    }
}
