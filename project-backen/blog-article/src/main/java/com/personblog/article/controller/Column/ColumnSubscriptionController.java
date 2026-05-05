package com.personblog.article.controller.Column;

import com.personblog.article.service.IColumnService;
import com.personblog.article.vo.SubscriberVO;
import com.personblog.article.vo.SubscriptionVO;
import com.personblog.common.result.JsonData;
import com.personblog.common.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 专栏订阅控制器
 * 负责专栏订阅的管理
 *
 * @author LSH
 */
@RestController
@RequestMapping("/article/columns")
@RequiredArgsConstructor
@Tag(name = "专栏订阅管理", description = "专栏订阅的管理接口，包括订阅、取消订阅、查询订阅状态和列表")
public class ColumnSubscriptionController {

    private final IColumnService columnService;

    /**
     * 订阅专栏
     * 用户可订阅感兴趣的专栏
     * 不能订阅自己的专栏，不能重复订阅
     *
     * @param id 专栏ID
     * @return 成功响应
     */
    @Operation(summary = "订阅专栏", description = "订阅指定专栏，不能订阅自己的专栏")
    @PostMapping("/{id}/subscribe")
    public JsonData<Void> subscribeColumn(
            @Parameter(description = "专栏ID") @PathVariable Long id) {
        Long userId = UserContextHolder.getUserId();
        columnService.subscribeColumn(userId, id);
        return JsonData.buildSuccess();
    }

    /**
     * 取消订阅专栏
     * 用户取消已订阅的专栏
     *
     * @param id 专栏ID
     * @return 成功响应
     */
    @Operation(summary = "取消订阅", description = "取消订阅专栏")
    @DeleteMapping("/{id}/subscribe")
    public JsonData<Void> unsubscribeColumn(
            @Parameter(description = "专栏ID") @PathVariable Long id) {
        Long userId = UserContextHolder.getUserId();
        columnService.unsubscribeColumn(userId, id);
        return JsonData.buildSuccess();
    }

    /**
     * 检查订阅状态
     * 检查当前用户是否已订阅该专栏
     * 未登录用户返回false
     *
     * @param id 专栏ID
     * @return 是否已订阅
     */
    @Operation(summary = "检查订阅状态", description = "检查是否已订阅该专栏")
    @GetMapping("/{id}/subscribe")
    public JsonData<Boolean> checkSubscribed(
            @Parameter(description = "专栏ID") @PathVariable Long id) {
        Long userId = UserContextHolder.getUserId();
        Boolean isSubscribed = columnService.checkSubscribed(userId, id);
        return JsonData.buildSuccess(isSubscribed);
    }

    /**
     * 获取用户订阅的专栏列表
     * 返回当前用户订阅的所有专栏
     * 按订阅时间降序排列
     *
     * @return 订阅列表
     */
    @Operation(summary = "获取订阅列表", description = "获取用户订阅的专栏列表")
    @GetMapping("/subscriptions")
    public JsonData<List<SubscriptionVO>> getSubscriptions() {
        Long userId = UserContextHolder.getUserId();
        List<SubscriptionVO> list = columnService.getSubscriptions(userId);
        return JsonData.buildSuccess(list);
    }

    /**
     * 获取专栏的订阅用户列表
     * 公开接口，返回订阅该专栏的用户列表
     * 最多返回100个用户，按订阅时间降序排列
     *
     * @param id 专栏ID
     * @return 订阅用户列表
     */
    @Operation(summary = "获取订阅用户列表", description = "获取专栏的订阅用户列表")
    @GetMapping("/{id}/subscribers")
    public JsonData<List<SubscriberVO>> getSubscribers(
            @Parameter(description = "专栏ID") @PathVariable Long id) {
        List<SubscriberVO> list = columnService.getSubscribers(id);
        return JsonData.buildSuccess(list);
    }
}
