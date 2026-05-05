package com.personblog.interaction.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.common.result.JsonData;
import com.personblog.common.utils.UserContextHolder;
import com.personblog.interaction.bizService.BizLikeService;
import com.personblog.interaction.service.BrowseHistoryService;
import com.personblog.interaction.service.CollectionService;
import com.personblog.interaction.vo.BrowseHistoryVO;
import com.personblog.interaction.vo.MyCollectionVO;
import com.personblog.interaction.vo.MyLikeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/interaction")
@RequiredArgsConstructor
@Tag(name = "用户互动", description = "用户互动相关接口")
public class InteractionController {

    private final BrowseHistoryService browseHistoryService;
    private final BizLikeService likeService;
    private final CollectionService collectionService;

    @GetMapping("/browse-history")
    @Operation(summary = "查询用户浏览历史", description = "分页查询当前用户的浏览历史记录")
    public JsonData<Page<BrowseHistoryVO>> getUserBrowseHistory(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size) {
        Long userId = UserContextHolder.getUserId();
        Page<BrowseHistoryVO> page = browseHistoryService.getUserBrowseHistory(userId, current, size);
        return JsonData.buildSuccess(page);
    }

    @GetMapping("/my-likes")
    @Operation(summary = "查询我的点赞", description = "分页查询当前用户点赞的文章列表")
    public JsonData<Page<MyLikeVO>> getMyLikes(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size) {
        Long userId = UserContextHolder.getUserId();
        Page<MyLikeVO> page = likeService.getMyLikes(userId, current, size);
        return JsonData.buildSuccess(page);
    }

    @GetMapping("/my-collections")
    @Operation(summary = "查询我的收藏", description = "分页查询当前用户收藏的文章列表")
    public JsonData<Page<MyCollectionVO>> getMyCollections(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size) {
        Long userId = UserContextHolder.getUserId();
        Page<MyCollectionVO> page = collectionService.getMyCollections(userId, current, size);
        return JsonData.buildSuccess(page);
    }
}
