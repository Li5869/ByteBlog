package com.personblog.interaction.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.common.result.JsonData;
import com.personblog.interaction.dto.CollectionDTO;
import com.personblog.interaction.service.CollectionService;
import com.personblog.interaction.vo.CollectionVO;
import com.personblog.interaction.vo.UserCollectionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/interaction/collections")
@RequiredArgsConstructor
@Tag(name = "收藏",description = "收藏文章")
public class CollectionController {
    private final CollectionService collectionService;

    @PostMapping("/toggle")
    @Operation(description = "收藏/取消收藏")
    public JsonData<CollectionVO> doCollection(@RequestBody CollectionDTO dto){
        CollectionVO vo = collectionService.doCollection(dto);
        return JsonData.buildSuccess(vo);
    }

    @GetMapping("/users/{userId}/collections")
    @Operation(summary = "获取用户收藏列表", description = "分页获取指定用户的收藏文章列表")
    public JsonData<Page<UserCollectionVO>> getUserCollections(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        return JsonData.buildSuccess(collectionService.getUserCollections(userId, current, size));
    }
}
