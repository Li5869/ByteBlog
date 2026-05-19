package com.personblog.admin.controller;

import com.personblog.admin.service.ITagService;
import com.personblog.api.adminAPI.TagVO;
import com.personblog.common.result.JsonData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签控制器
 *
 * @author LSH
 */
@RestController
@RequestMapping("/tag")
@RequiredArgsConstructor
@Tag(name = "标签控制器", description = "标签查询相关接口")
public class TagController {

    private final ITagService tagService;

    @GetMapping
    @Operation(summary = "获取标签列表")
    public JsonData<List<TagVO>> getTags() {
        return JsonData.buildSuccess(tagService.getTagList(null));
    }

    @GetMapping("/batch")
    @Operation(summary = "批量获取标签", description = "根据ID列表批量查询标签，id用逗号分隔")
    public JsonData<List<TagVO>> getTagsByIds(@RequestParam String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
        return JsonData.buildSuccess(tagService.getTagListByIds(idList));
    }
}
