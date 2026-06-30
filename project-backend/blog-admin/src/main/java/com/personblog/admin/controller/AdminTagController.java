package com.personblog.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.admin.aspect.RecordLog;
import com.personblog.admin.dto.AdminTagQueryDTO;
import com.personblog.admin.entity.Tag;
import com.personblog.admin.service.ITagService;
import com.personblog.admin.vo.AdminTagVO;
import com.personblog.common.result.JsonData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端标签管理控制器
 *
 * @author LSH
 */
@RestController
@RequestMapping("/admin/tags")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "管理端-标签管理", description = "管理后台的标签管理接口")
public class AdminTagController {

    private final ITagService tagService;

    @Operation(summary = "获取标签列表", description = "分页查询标签列表，支持搜索和排序")
    @PostMapping("/list")
    public JsonData<Page<AdminTagVO>> getTagList(@RequestBody AdminTagQueryDTO dto) {
        Page<AdminTagVO> page = tagService.getAdminTagList(dto);
        return JsonData.buildSuccess(page);
    }

    @RecordLog(Type = "create", businessType = "tag", description = "创建标签")
    @Operation(summary = "创建标签", description = "新增一个标签，标签名称全局唯一")
    @PostMapping
    public JsonData<AdminTagVO> createTag(@RequestBody Tag tag) {
        AdminTagVO vo = tagService.createTag(tag);
        return JsonData.buildSuccess(vo);
    }

    @RecordLog(Type = "update", businessType = "tag", description = "更新标签")
    @Operation(summary = "更新标签", description = "编辑已有标签的名称")
    @PutMapping("/{id}")
    public JsonData<Void> updateTag(
            @Parameter(description = "标签ID") @PathVariable Long id,
            @RequestBody Tag tag) {
        tag.setId(id);
        tagService.updateTagByAdmin(tag);
        return JsonData.buildSuccess();
    }

    @RecordLog(Type = "delete", businessType = "tag", description = "删除标签")
    @Operation(summary = "删除标签", description = "删除指定标签")
    @DeleteMapping("/{id}")
    public JsonData<Void> deleteTag(
            @Parameter(description = "标签ID") @PathVariable Long id) {
        tagService.deleteTagByAdmin(id);
        return JsonData.buildSuccess();
    }
}
