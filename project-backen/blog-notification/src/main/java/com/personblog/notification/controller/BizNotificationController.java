package com.personblog.notification.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.common.dto.Notification.BatchDeleteDTO;
import com.personblog.common.dto.Notification.BatchDeleteResultVO;
import com.personblog.common.dto.Notification.UnreadCountVO;
import com.personblog.common.result.JsonData;
import com.personblog.common.utils.UserContextHolder;
import com.personblog.notification.dto.BizNotificationQueryDTO;
import com.personblog.notification.service.IBizNotificationService;
import com.personblog.notification.vo.BizNotificationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "业务通知", description = "业务通知相关接口")
@RestController
@RequestMapping("/interaction/notifications/biz")
@RequiredArgsConstructor
public class BizNotificationController {

    private final IBizNotificationService bizNotificationService;

    @PostMapping
    @Operation(summary = "获取业务通知列表", description = "分页查询当前用户的业务通知列表，支持按类型筛选")
    public JsonData<Page<BizNotificationVO>> getBizNotificationList(@RequestBody BizNotificationQueryDTO dto) {
        Long userId = UserContextHolder.getUserId();
        Page<BizNotificationVO> page = bizNotificationService.getBizNotificationPage(userId, dto);
        return JsonData.buildSuccess(page);
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记业务通知已读", description = "将指定的业务通知标记为已读")
    public JsonData<Void> markAsRead(
            @Parameter(description = "通知ID") @PathVariable Long id) {
        Long userId = UserContextHolder.getUserId();
        bizNotificationService.markAsRead(userId, id);
        return JsonData.buildSuccess();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除业务通知", description = "删除指定的业务通知")
    public JsonData<Void> deleteNotification(
            @Parameter(description = "通知ID") @PathVariable Long id) {
        Long userId = UserContextHolder.getUserId();
        bizNotificationService.deleteNotification(userId, id);
        return JsonData.buildSuccess();
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除业务通知", description = "批量删除多条业务通知")
    public JsonData<BatchDeleteResultVO> batchDeleteNotifications(
            @Valid @RequestBody BatchDeleteDTO dto) {
        Long userId = UserContextHolder.getUserId();
        BatchDeleteResultVO result = bizNotificationService.batchDeleteNotifications(userId, dto.getIds());
        return JsonData.buildSuccess(result);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取业务通知未读数", description = "获取当前用户业务通知的未读数量")
    public JsonData<UnreadCountVO> getUnreadCount() {
        Long userId = UserContextHolder.getUserId();
        UnreadCountVO result = bizNotificationService.getUnreadCount(userId);
        return JsonData.buildSuccess(result);
    }

    @PutMapping("/read-all")
    @Operation(summary = "标记所有通知为已读", description = "将当前用户的所有业务通知标记为已读")
    public JsonData<Void> markAllAsRead() {
        Long userId = UserContextHolder.getUserId();
        bizNotificationService.markAllAsRead(userId);
        return JsonData.buildSuccess();
    }
}
